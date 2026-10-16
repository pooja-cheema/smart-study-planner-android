/**************************************************************
 * Purpose: Study Tips screen. Shows curated, hard coded study*
 * advice grouped into 8 categories with 4 tips per category  *
 * (32 total). The user steps through tips inside a category, *
 * jumps between categories, or pulls a random tip from any   *
 * category. Each tip transition runs a 3D card flip          *
 * animation. Fully offline; no network or database needed.   *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui.tips;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Random;

import edu.niu.android.smartstudyplanner.databinding.FragmentStudyTipsBinding;

public class StudyTipsFragment extends Fragment {

    private FragmentStudyTipsBinding binding;

    // Index of the active category in the categories array.
    private int currentCategoryIndex = 0;

    // Index of the active tip within the active category.
    private int currentTipIndex = 0;

    // Used by the random tip button.
    private final Random random = new Random();

    // Category labels shown above the tip card.
    private final String[] categories = {
            "🎯  Focus",
            "🧠  Memory",
            "📝  Exams",
            "⏰  Time",
            "🚀  Motivation",
            "📓  Notes",
            "😴  Rest",
            "👥  Group Study"
    };

    // Tip text per category. Outer array index lines up with categories.
    private final String[][] tips = {
            // Focus
            {
                    "Use the Pomodoro Technique - 25 minutes of focused work followed by a 5-minute break. Your brain stays sharp and fatigue builds up slowly.",
                    "Study in the same spot every day. Your brain associates that location with focus and enters concentration mode automatically over time.",
                    "Write down every distraction that pops into your head, then return to work. Your brain stops looping on it once it is captured on paper.",
                    "Turn your phone face-down and put it across the room. Even its presence on the desk reduces cognitive capacity by up to 20%."
            },
            // Memory
            {
                    "Review new material after 1 day, 3 days, 1 week, then 1 month. This spaced repetition is the single most proven memory technique in cognitive science.",
                    "Teach the concept out loud to an imaginary student. If you cannot explain it simply, you have found a gap in your understanding.",
                    "Use memory palaces - place concepts at locations in a familiar building and mentally walk through it to recall them in sequence.",
                    "Write notes by hand rather than typing. The slower pace forces you to paraphrase and process, which dramatically improves retention."
            },
            // Exams
            {
                    "Practice retrieval - close your notes and write everything you remember on a blank page. Active recall beats re-reading every time.",
                    "Take at least 2 full practice exams under real conditions: timed, no notes, alone. Performance under pressure is a separate skill from knowing the material.",
                    "Make a one-page summary for each topic one week before the exam. Deciding what matters forces deep processing of the content.",
                    "Study your most difficult subject first, when mental energy is highest. Save review of familiar material for the end of your session."
            },
            // Time Management
            {
                    "Schedule your study blocks on Sunday evening for the whole week. Treat them as non-negotiable appointments, not suggestions.",
                    "If a task takes under 2 minutes - do it immediately. Small tasks handled on sight prevent the pile-up that creates overwhelm.",
                    "Plan your next day the night before. Write exactly what you will study, when, and for how long. Start with clarity instead of deciding in the moment.",
                    "Use the 80/20 rule - identify the 20% of content that will appear on 80% of the exam and master it first before going broader."
            },
            // Motivation
            {
                    "Break large assignments into the smallest possible steps. 'Write introduction paragraph' is actionable. 'Write paper' is paralyzing.",
                    "Tell yourself you will study for just 5 minutes. Starting is the hardest part - most of the time you will continue well past 5 minutes.",
                    "Track your study hours daily. The visual record of past effort builds momentum and makes it harder to skip a session.",
                    "Connect today's studying to your long-term goal. A concrete reason why this exam matters makes short-term discomfort easier to push through."
            },
            // Notes
            {
                    "Use the Cornell Method - a narrow left column for keywords, wide right column for notes, summary box at the bottom filled in from memory.",
                    "Never copy slides verbatim. Always paraphrase before writing. If you cannot restate it in your own words, you do not yet understand it.",
                    "Review and rewrite your notes within 24 hours of every lecture. 60% of new information is lost within the first day without review.",
                    "Add a question mark next to anything confusing during lecture. Review those marked items first before the confusion solidifies into a gap."
            },
            // Rest
            {
                    "Protect your sleep above everything else. Memory consolidation happens during deep sleep - a well-rested brain outperforms an exhausted one every time.",
                    "During study breaks, avoid your phone. Scrolling social media is mentally exhausting, not restful. Walk, stretch, or sit quietly instead.",
                    "Exercise before your most important study session. Even a 20-minute walk increases blood flow to the prefrontal cortex - the learning center of the brain.",
                    "A 20-minute nap between study sessions can restore alertness as effectively as caffeine without the crash that follows."
            },
            // Group Study
            {
                    "Set a written agenda before every group session. Without structure, group study reliably becomes social time within 15 minutes.",
                    "Quiz each other rather than reading notes together. Mutual testing identifies gaps and forces everyone to actively retrieve rather than passively review.",
                    "Keep groups to 3 or 4 people. Larger groups produce more off-topic conversation and less participation per person.",
                    "Assign each person a section to teach to the group. Teaching requires the deepest understanding and benefits everyone who listens."
            }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudyTipsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show the first tip on first display before any button is tapped.
        displayTip();

        // Advance to the next tip within the current category, wrapping back to start.
        binding.btnNextTip.setOnClickListener(v -> {
            currentTipIndex = (currentTipIndex + 1) % tips[currentCategoryIndex].length;
            animateAndShow();
        });

        // Pick a random category and a random tip in that category.
        binding.btnRandomTip.setOnClickListener(v -> {
            currentCategoryIndex = random.nextInt(categories.length);
            currentTipIndex = random.nextInt(tips[currentCategoryIndex].length);
            displayCategory();
            animateAndShow();
        });

        // Move to the previous category and reset to its first tip.
        binding.btnPrevCategory.setOnClickListener(v -> {
            currentCategoryIndex =
                    (currentCategoryIndex - 1 + categories.length) % categories.length;
            currentTipIndex = 0;
            displayCategory();
            animateAndShow();
        });

        // Move to the next category and reset to its first tip.
        binding.btnNextCategory.setOnClickListener(v -> {
            currentCategoryIndex = (currentCategoryIndex + 1) % categories.length;
            currentTipIndex = 0;
            displayCategory();
            animateAndShow();
        });
    }

    /**
     * Updates the category label and the "Topic N of M" indicator.
     */
    private void displayCategory() {
        binding.textCategory.setText(categories[currentCategoryIndex]);
        binding.textCategoryNumber.setText(
                "Topic " + (currentCategoryIndex + 1) + " of " + categories.length);
    }

    /**
     * Renders the current tip and category without animating, used for
     * the very first paint when the screen opens.
     */
    private void displayTip() {
        displayCategory();
        binding.textTipContent.setText(tips[currentCategoryIndex][currentTipIndex]);
        binding.textTipNumber.setText(
                "Tip " + (currentTipIndex + 1) + " of "
                        + tips[currentCategoryIndex].length);
    }

    /**
     * Plays the 3D card flip when moving to a new tip. Rotates the card
     * to 90 degrees on the Y axis (so its edge faces the viewer), swaps
     * the text while it's invisible, then rotates back from 90 with an
     * overshoot interpolator for a slight bounce.
     */
    private void animateAndShow() {
        // First half: rotate from face-on to edge-on so the old text disappears.
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(
                binding.cardTip, "rotationY", 0f, 90f);
        flipOut.setDuration(180);

        flipOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Card is edge-on now, so the text swap is invisible to the user.
                binding.textTipContent.setText(
                        tips[currentCategoryIndex][currentTipIndex]);
                binding.textTipNumber.setText(
                        "Tip " + (currentTipIndex + 1) + " of "
                                + tips[currentCategoryIndex].length);

                // Second half: rotate the new face from edge-on back to flat,
                // with a small overshoot so it has a tactile bounce.
                ObjectAnimator flipIn = ObjectAnimator.ofFloat(
                        binding.cardTip, "rotationY", -90f, 0f);
                flipIn.setDuration(180);
                flipIn.setInterpolator(new OvershootInterpolator(1.1f));
                flipIn.start();
            }
        });

        flipOut.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}