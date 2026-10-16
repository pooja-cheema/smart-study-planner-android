package edu.niu.android.smartstudyplanner.ui.flashcards;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.Flashcard;
import edu.niu.android.smartstudyplanner.databinding.FragmentFlashcardsBinding;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.FlashcardViewModel;

public class FlashcardsFragment extends Fragment {

    private FragmentFlashcardsBinding binding;
    private FlashcardViewModel flashcardViewModel;
    private CourseViewModel courseViewModel;

    // All enrolled courses, kept so the spinner position can be mapped to a course id.
    private List<Course> courses = new ArrayList<>();

    // Course currently being studied, or -1 if none picked yet.
    private long selectedCourseId = -1;

    // The deck for the selected course.
    private List<Flashcard> currentDeck = new ArrayList<>();

    // Index of the visible card within currentDeck.
    private int currentCardIndex = 0;

    // True when the back of the current card is showing.
    private boolean isShowingBack = false;

    // The two halves of the flip animation, loaded once and retargeted per flip.
    private AnimatorSet flipInAnim;
    private AnimatorSet flipOutAnim;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFlashcardsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        flashcardViewModel = new ViewModelProvider(this).get(FlashcardViewModel.class);
        courseViewModel    = new ViewModelProvider(this).get(CourseViewModel.class);

        setupFlipAnimations();
        setupCourseSpinner();
        setupButtons();
    }

    /**
     * Loads the two flip animator sets and pushes the camera distance back
     * so the rotation looks 3D instead of squashed.
     */
    private void setupFlipAnimations() {
        flipOutAnim = (AnimatorSet) AnimatorInflater.loadAnimator(
                requireContext(), R.animator.card_flip_left_out);
        flipInAnim = (AnimatorSet) AnimatorInflater.loadAnimator(
                requireContext(), R.animator.card_flip_left_in);

        float scale = requireContext().getResources().getDisplayMetrics().density;
        binding.cardFront.setCameraDistance(8000 * scale);
        binding.cardBack.setCameraDistance(8000 * scale);
    }

    /**
     * Populates the course spinner from LiveData and loads the deck for
     * whichever course the user picks.
     */
    private void setupCourseSpinner() {
        courseViewModel.getAllCourses().observe(getViewLifecycleOwner(), courseList -> {
            courses.clear();
            courses.addAll(courseList);

            List<String> names = new ArrayList<>();
            for (Course c : courseList) names.add(c.getName());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerCourse.setAdapter(adapter);
        });

        binding.spinnerCourse.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view, int pos, long id) {
                        if (!courses.isEmpty()) {
                            selectedCourseId = courses.get(pos).getId();
                            loadDeck();
                        }
                    }
                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    /**
     * Loads the deck for the currently selected course and resets the
     * study session back to card 1, front side. Toggles the empty state
     * if the course has no cards yet.
     */
    private void loadDeck() {
        if (selectedCourseId == -1) return;

        flashcardViewModel.getFlashcardsByCourse(selectedCourseId)
                .observe(getViewLifecycleOwner(), flashcards -> {
                    currentDeck.clear();
                    currentDeck.addAll(flashcards);
                    currentCardIndex = 0;
                    isShowingBack = false;

                    if (flashcards.isEmpty()) {
                        binding.layoutDeck.setVisibility(View.GONE);
                        binding.textEmptyDeck.setVisibility(View.VISIBLE);
                    } else {
                        binding.layoutDeck.setVisibility(View.VISIBLE);
                        binding.textEmptyDeck.setVisibility(View.GONE);
                        showCurrentCard();
                        updateProgress();
                    }
                });
    }

    /**
     * Renders the current card on the front face. Resets any leftover
     * rotation from a previous flip and updates the "X / Y" counter and
     * the mastered badge.
     */
    private void showCurrentCard() {
        if (currentDeck.isEmpty()) return;

        Flashcard card = currentDeck.get(currentCardIndex);

        binding.cardFront.setVisibility(View.VISIBLE);
        binding.cardBack.setVisibility(View.GONE);
        binding.cardFront.setRotationY(0f);
        binding.cardBack.setRotationY(0f);
        isShowingBack = false;

        binding.textCardFront.setText(card.getFront());
        binding.textCardBack.setText(card.getBack());
        binding.textCardCounter.setText(
                (currentCardIndex + 1) + " / " + currentDeck.size());

        binding.badgeMastered.setVisibility(
                card.isMastered() ? View.VISIBLE : View.GONE);
    }

    /**
     * Refreshes the "N / M mastered" label and the progress bar. The
     * counts come from the ViewModel's callback methods, so this hops
     * to a background thread and back to the UI thread before drawing.
     */
    private void updateProgress() {
        flashcardViewModel.getMasteredCount(selectedCourseId, mastered -> {
            flashcardViewModel.getTotalCount(selectedCourseId, total -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.textProgress.setText(mastered + " / " + total + " mastered");
                        int percent = total > 0 ? (mastered * 100 / total) : 0;
                        binding.progressMastery.setProgress(percent);
                    });
                }
            });
        });
    }

    /**
     * Wires up every button on the screen: flip, got it, again, prev,
     * next, add card, reset deck. Also styles the FAB with the same
     * gradient used on the Tasks tab so the visual language is consistent.
     */
    private void setupButtons() {
        // Black to ocean blue gradient, matches the Tasks tab FAB.
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable fabGradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{ 0xFF000000, 0xFF0369A1 });
        fabGradient.setCornerRadius(28f * dp);
        binding.fabAddCard.setBackground(fabGradient);

        // Tapping the card itself or the Flip button both flip it.
        binding.btnFlip.setOnClickListener(v -> flipCard());
        binding.cardFront.setOnClickListener(v -> flipCard());
        binding.cardBack.setOnClickListener(v -> flipCard());

        // "Got it" marks mastered and moves on.
        binding.btnKnowIt.setOnClickListener(v -> {
            if (currentDeck.isEmpty()) return;
            Flashcard card = currentDeck.get(currentCardIndex);
            flashcardViewModel.markMastered(card.getId());
            Toast.makeText(requireContext(),
                    "✅ Marked as mastered!", Toast.LENGTH_SHORT).show();
            advanceCard();
            updateProgress();
        });

        // "Again" leaves the card in the unmastered pile and moves on.
        binding.btnDontKnow.setOnClickListener(v -> {
            if (currentDeck.isEmpty()) return;
            Flashcard card = currentDeck.get(currentCardIndex);
            flashcardViewModel.markNotMastered(card.getId());
            Toast.makeText(requireContext(),
                    "📖 Keep studying!", Toast.LENGTH_SHORT).show();
            advanceCard();
        });

        // Wraps around to the end when on the first card.
        binding.btnPrevCard.setOnClickListener(v -> {
            if (currentDeck.isEmpty()) return;
            currentCardIndex = (currentCardIndex - 1 + currentDeck.size())
                    % currentDeck.size();
            showCurrentCard();
        });

        binding.btnNextCard.setOnClickListener(v -> {
            if (currentDeck.isEmpty()) return;
            advanceCard();
        });

        binding.fabAddCard.setOnClickListener(v -> showAddCardDialog());

        // Reset deck is destructive, so it goes through a confirm dialog.
        binding.btnResetDeck.setOnClickListener(v -> {
            if (selectedCourseId == -1) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle("Reset Deck")
                    .setMessage("Mark all cards as unmastered and start fresh?")
                    .setPositiveButton("Reset", (d, w) -> {
                        flashcardViewModel.resetDeck(selectedCourseId);
                        Toast.makeText(requireContext(),
                                "Deck reset! Study from the beginning.",
                                Toast.LENGTH_SHORT).show();
                        updateProgress();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Runs the flip animation between front and back. The 200ms delayed
     * visibility swap matches the halfway point of the animation, so the
     * face that's becoming hidden is gone by the time the new one appears.
     */
    private void flipCard() {
        if (!isShowingBack) {
            flipOutAnim.setTarget(binding.cardFront);
            flipInAnim.setTarget(binding.cardBack);
            flipOutAnim.start();
            flipInAnim.start();
            binding.cardBack.setVisibility(View.VISIBLE);
            binding.cardFront.postDelayed(
                    () -> binding.cardFront.setVisibility(View.GONE), 200);
            isShowingBack = true;
        } else {
            flipOutAnim.setTarget(binding.cardBack);
            flipInAnim.setTarget(binding.cardFront);
            flipOutAnim.start();
            flipInAnim.start();
            binding.cardFront.setVisibility(View.VISIBLE);
            binding.cardBack.postDelayed(
                    () -> binding.cardBack.setVisibility(View.GONE), 200);
            isShowingBack = false;
        }
    }

    /**
     * Moves to the next card in the deck, wrapping back to the start
     * when the end is reached.
     */
    private void advanceCard() {
        if (currentDeck.isEmpty()) return;
        if (currentCardIndex >= currentDeck.size() - 1) {
            currentCardIndex = 0;
            Toast.makeText(requireContext(),
                    "🎉 End of deck! Starting over.", Toast.LENGTH_SHORT).show();
        } else {
            currentCardIndex++;
        }
        showCurrentCard();
    }

    /**
     * Shows the "add a flashcard" dialog. Both front and back are required;
     * empty inputs show a toast and the dialog stays open. The new card
     * is inserted for the currently selected course.
     */
    private void showAddCardDialog() {
        if (selectedCourseId == -1) {
            Toast.makeText(requireContext(),
                    "Select a course first", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_flashcard, null);

        TextInputEditText editFront = dialogView.findViewById(R.id.edit_card_front);
        TextInputEditText editBack  = dialogView.findViewById(R.id.edit_card_back);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Flashcard")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String front = editFront.getText().toString().trim();
                    String back  = editBack.getText().toString().trim();

                    if (front.isEmpty() || back.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Both sides are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Flashcard card = new Flashcard(
                            selectedCourseId, front, back,
                            System.currentTimeMillis());
                    flashcardViewModel.insert(card);
                    Toast.makeText(requireContext(),
                            "Card added!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}