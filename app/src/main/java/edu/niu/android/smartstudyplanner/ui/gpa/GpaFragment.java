package edu.niu.android.smartstudyplanner.ui.gpa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import edu.niu.android.smartstudyplanner.databinding.FragmentGpaBinding;
import edu.niu.android.smartstudyplanner.databinding.ItemGpaCourseBinding;

public class GpaFragment extends Fragment {

    private FragmentGpaBinding binding;

    /**
     * One row in the GPA calculator: course name, credit hours, the
     * picked letter grade, and its numeric value on the 4.0 scale.
     */
    private static class GpaEntry {
        String courseName;
        int creditHours;
        String grade;
        double gradePoints;

        GpaEntry(String name, int credits, String grade, double points) {
            this.courseName = name;
            this.creditHours = credits;
            this.grade = grade;
            this.gradePoints = points;
        }
    }

    // Every course the student has added in the current session.
    private final List<GpaEntry> entries = new ArrayList<>();

    // Letter grade options shown in the spinner, on the 4.0 scale.
    private final String[] gradeOptions = {
            "A+", "A", "A-",
            "B+", "B", "B-",
            "C+", "C", "C-",
            "D+", "D", "D-",
            "F"
    };

    // Numeric grade points for each entry in gradeOptions, same index order.
    private final double[] gradePoints = {
            4.0, 4.0, 3.7,
            3.3, 3.0, 2.7,
            2.3, 2.0, 1.7,
            1.3, 1.0, 0.7,
            0.0
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGpaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupGradeSpinner();
        setupButtons();

        // Render the empty state on first display.
        updateGpaDisplay();
    }

    /**
     * Fills the grade spinner with A+ through F.
     */
    private void setupGradeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                gradeOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGrade.setAdapter(adapter);
    }

    /**
     * Wires up the three primary buttons: Add Course, Calculate Target,
     * and Clear All. Add Course also handles input validation.
     */
    private void setupButtons() {

        binding.btnAddCourse.setOnClickListener(v -> {
            String name = binding.editCourseName.getText().toString().trim();
            String creditsStr = binding.editCredits.getText().toString().trim();

            if (name.isEmpty()) {
                binding.editCourseName.setError("Enter course name");
                return;
            }

            if (creditsStr.isEmpty()) {
                binding.editCredits.setError("Enter credit hours");
                return;
            }

            int credits;
            try {
                credits = Integer.parseInt(creditsStr);
                if (credits < 1 || credits > 12) {
                    binding.editCredits.setError("Credits must be 1–12");
                    return;
                }
            } catch (NumberFormatException e) {
                binding.editCredits.setError("Invalid number");
                return;
            }

            // The spinner index lines up with both option arrays.
            int gradeIndex = binding.spinnerGrade.getSelectedItemPosition();
            String grade = gradeOptions[gradeIndex];
            double points = gradePoints[gradeIndex];

            entries.add(new GpaEntry(name, credits, grade, points));
            addCourseRowToTable(entries.get(entries.size() - 1),
                    entries.size() - 1);

            // Reset the inputs so the student can keep adding courses quickly.
            binding.editCourseName.setText("");
            binding.editCredits.setText("");
            binding.editCourseName.requestFocus();

            updateGpaDisplay();
        });

        binding.btnCalculateTarget.setOnClickListener(v -> {
            String targetStr = binding.editTargetGpa.getText().toString().trim();
            String remainingStr = binding.editRemainingCredits.getText().toString().trim();

            if (targetStr.isEmpty() || remainingStr.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Enter target GPA and remaining credits",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            double targetGpa;
            int remainingCredits;

            try {
                targetGpa = Double.parseDouble(targetStr);
                remainingCredits = Integer.parseInt(remainingStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(),
                        "Invalid numbers entered", Toast.LENGTH_SHORT).show();
                return;
            }

            if (targetGpa < 0 || targetGpa > 4.0) {
                binding.editTargetGpa.setError("GPA must be 0.0 – 4.0");
                return;
            }

            if (remainingCredits < 1) {
                binding.editRemainingCredits.setError("Must be at least 1");
                return;
            }

            calculateTargetGpa(targetGpa, remainingCredits);
        });

        binding.btnClearAll.setOnClickListener(v -> {
            entries.clear();
            binding.layoutCourseList.removeAllViews();
            updateGpaDisplay();
            binding.textTargetResult.setVisibility(View.GONE);
            binding.editTargetGpa.setText("");
            binding.editRemainingCredits.setText("");
            Toast.makeText(requireContext(),
                    "All courses cleared", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Inflates one row for the given entry and appends it to the course
     * list container. The delete button removes the entry from the model
     * and rebuilds the list. Index isn't used directly here; deletion
     * matches by name, grade, and credits, which is fine for a small
     * in memory list.
     */
    private void addCourseRowToTable(GpaEntry entry, int index) {
        ItemGpaCourseBinding itemBinding = ItemGpaCourseBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.layoutCourseList,
                false
        );

        itemBinding.textCourseNameRow.setText(entry.courseName);
        itemBinding.textCreditsRow.setText(entry.creditHours + " cr");
        itemBinding.textGradeRow.setText(entry.grade);
        itemBinding.textPointsRow.setText(
                String.format("%.1f pts", entry.gradePoints));

        itemBinding.btnDeleteEntry.setOnClickListener(v -> {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).courseName.equals(entry.courseName)
                        && entries.get(i).grade.equals(entry.grade)
                        && entries.get(i).creditHours == entry.creditHours) {
                    entries.remove(i);
                    break;
                }
            }
            // Rebuild instead of tracking individual view references.
            rebuildCourseList();
            updateGpaDisplay();
        });

        binding.layoutCourseList.addView(itemBinding.getRoot());
    }

    /**
     * Clears the course list container and re-adds a row for every entry.
     * Used after a deletion. Fine for a list of this size.
     */
    private void rebuildCourseList() {
        binding.layoutCourseList.removeAllViews();
        for (int i = 0; i < entries.size(); i++) {
            addCourseRowToTable(entries.get(i), i);
        }
    }

    /**
     * Recomputes and displays the current cumulative GPA. The card's
     * background and labels change color based on the GPA tier so the
     * student gets an at a glance read on their standing.
     *
     * Formula: GPA = sum(gradePoints * credits) / sum(credits)
     */
    private void updateGpaDisplay() {
        if (entries.isEmpty()) {
            binding.textCurrentGpa.setText("–");
            binding.textGpaLabel.setText("Add courses above to calculate GPA");
            binding.textTotalCredits.setText("Total Credits: 0");
            binding.cardGpaResult.setCardBackgroundColor(
                    requireContext().getColor(android.R.color.white));

            // Dark blue text so everything is readable on the white card.
            binding.textGpaTitle.setTextColor(0xFF1A237E);
            binding.textCurrentGpa.setTextColor(0xFF1A237E);
            binding.textGpaLabel.setTextColor(0xFF1A237E);
            binding.textTotalCredits.setTextColor(0xFF1A237E);
            return;
        }

        double totalQualityPoints = 0;
        int totalCredits = 0;
        for (GpaEntry e : entries) {
            totalQualityPoints += e.gradePoints * e.creditHours;
            totalCredits += e.creditHours;
        }

        double gpa = totalQualityPoints / totalCredits;

        // Tiered cards are dark, so the title goes back to light blue.
        binding.textGpaTitle.setTextColor(0xFF90CAF9);

        binding.textCurrentGpa.setText(String.format("%.2f", gpa));
        binding.textTotalCredits.setText("Total Credits: " + totalCredits);

        // Tiered color and label based on the resulting GPA.
        if (gpa >= 3.5) {
            binding.textGpaLabel.setText("🌟 Excellent - Dean's List Territory");
            binding.cardGpaResult.setCardBackgroundColor(0xFF1B5E20);
            binding.textCurrentGpa.setTextColor(0xFFFFFFFF);
            binding.textGpaLabel.setTextColor(0xFFA5D6A7);
            binding.textTotalCredits.setTextColor(0xFFA5D6A7);
        } else if (gpa >= 3.0) {
            binding.textGpaLabel.setText("👍 Good Standing");
            binding.cardGpaResult.setCardBackgroundColor(0xFF0D47A1);
            binding.textCurrentGpa.setTextColor(0xFFFFFFFF);
            binding.textGpaLabel.setTextColor(0xFF90CAF9);
            binding.textTotalCredits.setTextColor(0xFF90CAF9);
        } else if (gpa >= 2.0) {
            binding.textGpaLabel.setText("⚠️ Satisfactory - Room to Improve");
            binding.cardGpaResult.setCardBackgroundColor(0xFFE65100);
            binding.textCurrentGpa.setTextColor(0xFFFFFFFF);
            binding.textGpaLabel.setTextColor(0xFFFFE0B2);
            binding.textTotalCredits.setTextColor(0xFFFFE0B2);
        } else {
            binding.textGpaLabel.setText("🚨 At Risk - Seek Academic Support");
            binding.cardGpaResult.setCardBackgroundColor(0xFFB71C1C);
            binding.textCurrentGpa.setTextColor(0xFFFFFFFF);
            binding.textGpaLabel.setTextColor(0xFFFFCDD2);
            binding.textTotalCredits.setTextColor(0xFFFFCDD2);
        }
    }

    /**
     * Computes the average grade points the student needs across their
     * remaining credits to land on the target GPA. The result is shown
     * with the closest letter grade equivalent, or as "not achievable"
     * if even straight A's wouldn't get there, or as "already achieved"
     * if the current GPA is already at or above the target.
     *
     * Derivation:
     *   target = (currentPoints + neededPoints) / (currentCredits + remaining)
     *   neededAvg = (target * totalCredits - currentPoints) / remaining
     */
    private void calculateTargetGpa(double targetGpa, int remainingCredits) {
        double currentQualityPoints = 0;
        int currentCredits = 0;
        for (GpaEntry e : entries) {
            currentQualityPoints += e.gradePoints * e.creditHours;
            currentCredits += e.creditHours;
        }

        int totalCredits = currentCredits + remainingCredits;

        double neededQualityPoints =
                (targetGpa * totalCredits) - currentQualityPoints;

        double neededAvg = neededQualityPoints / remainingCredits;

        binding.textTargetResult.setVisibility(View.VISIBLE);

        if (neededAvg > 4.0) {
            // Even an A average in remaining courses can't reach the target.
            binding.textTargetResult.setText(
                    "❌ Target GPA of " + String.format("%.2f", targetGpa)
                            + " is not achievable in " + remainingCredits + " remaining credits.\n\n"
                            + "Maximum possible GPA with all A grades:\n"
                            + String.format("%.2f",
                            (currentQualityPoints + 4.0 * remainingCredits) / totalCredits));
            binding.textTargetResult.setBackgroundColor(0xFFFFCDD2);
        } else if (neededAvg < 0) {
            // Current GPA is already above the target.
            binding.textTargetResult.setText(
                    "✅ You have already achieved a GPA above "
                            + String.format("%.2f", targetGpa) + "!\n\n"
                            + "Current GPA: "
                            + String.format("%.2f",
                            currentCredits > 0
                                    ? currentQualityPoints / currentCredits : 0.0));
            binding.textTargetResult.setBackgroundColor(0xFFC8E6C9);
        } else {
            // Achievable. Show numeric average and the closest letter grade.
            String letterNeeded = pointsToLetter(neededAvg);
            binding.textTargetResult.setText(
                    "📊 To reach a " + String.format("%.2f", targetGpa) + " GPA:\n\n"
                            + "You need an average of  "
                            + String.format("%.2f", neededAvg) + "  (" + letterNeeded + ")\n"
                            + "in your remaining " + remainingCredits + " credit hours.");
            binding.textTargetResult.setBackgroundColor(0xFFE3F2FD);
        }
    }

    /**
     * Maps a numeric grade points value (0.0 to 4.0) to the closest
     * letter grade so the target result reads naturally.
     */
    private String pointsToLetter(double points) {
        if (points >= 4.0) return "A / A+";
        if (points >= 3.7) return "A-";
        if (points >= 3.3) return "B+";
        if (points >= 3.0) return "B";
        if (points >= 2.7) return "B-";
        if (points >= 2.3) return "C+";
        if (points >= 2.0) return "C";
        if (points >= 1.7) return "C-";
        if (points >= 1.3) return "D+";
        if (points >= 1.0) return "D";
        if (points >= 0.7) return "D-";
        return "F";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}