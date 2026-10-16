package edu.niu.android.smartstudyplanner.ui.courses;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.utils.ColorUtils;

public class CourseAdapter extends ListAdapter<Course, CourseAdapter.CourseViewHolder> {

    // Click callbacks the host fragment provides for the row action buttons.
    public interface OnCourseEditListener   { void onEdit(Course course); }
    public interface OnCourseDeleteListener { void onDelete(Course course); }

    private final OnCourseEditListener   editListener;
    private final OnCourseDeleteListener deleteListener;

    // Unfiltered copy of the list. Search filter is applied against this
    // so clearing the query restores the original set.
    private List<Course> fullList = new ArrayList<>();

    // Tells DiffUtil whether two rows represent the same course (by id) and
    // whether their visible content actually changed (so unchanged rows
    // don't get rebound).
    private static final DiffUtil.ItemCallback<Course> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Course>() {
                @Override
                public boolean areItemsTheSame(@NonNull Course a, @NonNull Course b) {
                    return a.getId() == b.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull Course a, @NonNull Course b) {
                    return a.getName().equals(b.getName())
                            && a.getInstructor().equals(b.getInstructor())
                            && a.getColor().equals(b.getColor())
                            && a.getSchedule().equals(b.getSchedule());
                }
            };

    public CourseAdapter(OnCourseEditListener editListener,
                         OnCourseDeleteListener deleteListener) {
        super(DIFF_CALLBACK);
        this.editListener   = editListener;
        this.deleteListener = deleteListener;
    }

    /**
     * Replaces the underlying course list and shows it. Called from the
     * fragment whenever LiveData emits a fresh list.
     */
    public void setFullList(List<Course> courses) {
        fullList = courses != null ? new ArrayList<>(courses) : new ArrayList<>();
        submitList(new ArrayList<>(fullList));
    }

    /**
     * Filters the displayed list against the given query. Empty query
     * restores the full list. Matches against course name, instructor,
     * room, and schedule.
     */
    public List<Course> filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            submitList(new ArrayList<>(fullList));
            return fullList;
        }
        String q = query.toLowerCase().trim();
        List<Course> filtered = new ArrayList<>();
        for (Course c : fullList) {
            if (c.getName().toLowerCase().contains(q)
                    || c.getInstructor().toLowerCase().contains(q)
                    || (c.getRoom() != null && c.getRoom().toLowerCase().contains(q))
                    || (c.getSchedule() != null && c.getSchedule().toLowerCase().contains(q))) {
                filtered.add(c);
            }
        }
        submitList(filtered);
        return filtered;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    /**
     * View holder for one course row. Holds direct references to every
     * view that bind() needs to update so we don't call findViewById on
     * every bind.
     */
    class CourseViewHolder extends RecyclerView.ViewHolder {

        private final View        sidebar;
        private final TextView    textAbbr;
        private final TextView    textCode;
        private final TextView    textName;
        private final TextView    textInstructorAvatar;
        private final TextView    textInstructor;
        private final TextView    textSemesterBadge;
        private final ImageView   imgSchedule;
        private final TextView    textSchedule;
        private final ImageView   imgRoom;
        private final TextView    textRoom;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        // Container for the avatar, name, and semester badge. Hidden when
        // the course has no instructor on file.
        private final View instructorRow;

        CourseViewHolder(View itemView) {
            super(itemView);
            sidebar              = itemView.findViewById(R.id.view_color_sidebar);
            textAbbr             = itemView.findViewById(R.id.text_course_abbr);
            textCode             = itemView.findViewById(R.id.text_course_code);
            textName             = itemView.findViewById(R.id.text_course_name);
            textInstructorAvatar = itemView.findViewById(R.id.text_instructor_avatar);
            textInstructor       = itemView.findViewById(R.id.text_instructor);
            textSemesterBadge    = itemView.findViewById(R.id.text_semester_badge);
            imgSchedule          = itemView.findViewById(R.id.img_schedule);
            textSchedule         = itemView.findViewById(R.id.text_schedule);
            imgRoom              = itemView.findViewById(R.id.img_room);
            textRoom             = itemView.findViewById(R.id.text_room);
            btnEdit              = itemView.findViewById(R.id.btn_edit);
            btnDelete            = itemView.findViewById(R.id.btn_delete);
            instructorRow        = itemView.findViewById(R.id.layout_instructor_row);
        }

        /**
         * Fills every view in the row from the given course. The course
         * color is reused on the sidebar, code, badges, and icons so each
         * row has a consistent identity.
         */
        void bind(Course course) {
            float dp    = itemView.getResources().getDisplayMetrics().density;
            int   color = ColorUtils.parseColor(course.getColor());

            sidebar.setBackgroundColor(color);

            // The course's stored name is "CODE - Name". Split it back into
            // code and display name for the two TextViews.
            String fullName    = course.getName();
            String code        = fullName;
            String displayName = fullName;
            if (fullName.contains(" - ")) {
                code        = fullName.substring(0, fullName.indexOf(" - "));
                displayName = fullName.substring(fullName.indexOf(" - ") + 3);
            }
            textCode.setText(code);
            textCode.setTextColor(color);
            textName.setText(displayName);

            // Rounded color tile with a short abbreviation pulled from the code.
            textAbbr.setText(buildAbbr(code));
            GradientDrawable abbrBg = new GradientDrawable();
            abbrBg.setColor(color);
            abbrBg.setCornerRadius(14 * dp);
            textAbbr.setBackground(abbrBg);

            // Instructor row is hidden entirely when the course has no
            // instructor on file, so the row doesn't render with an empty avatar.
            String instructor = course.getInstructor();
            if (instructor == null || instructor.trim().isEmpty()) {
                if (instructorRow != null) instructorRow.setVisibility(View.GONE);
            } else {
                if (instructorRow != null) instructorRow.setVisibility(View.VISIBLE);
                textInstructor.setText(instructor);
                textInstructorAvatar.setText(buildInitials(instructor));
                GradientDrawable avatarBg = new GradientDrawable();
                avatarBg.setShape(GradientDrawable.OVAL);
                avatarBg.setColor(color);
                textInstructorAvatar.setBackground(avatarBg);
            }

            String semester = course.getSemester();
            textSemesterBadge.setText(semester != null && !semester.isEmpty()
                    ? semester : "");
            textSemesterBadge.setTextColor(color);

            // Schedule line. Em dash placeholder when the course has no schedule set.
            if (course.getSchedule() != null && !course.getSchedule().isEmpty()) {
                textSchedule.setText(course.getSchedule());
            } else {
                textSchedule.setText("-");
            }
            textSchedule.setTextColor(color);
            imgSchedule.setColorFilter(color);

            // Room line. Same em dash placeholder.
            if (course.getRoom() != null && !course.getRoom().isEmpty()) {
                textRoom.setText(course.getRoom());
            } else {
                textRoom.setText("-");
            }
            textRoom.setTextColor(color);
            imgRoom.setColorFilter(color);

            btnEdit.setColorFilter(color);

            btnEdit.setOnClickListener(v  -> editListener.onEdit(course));
            btnDelete.setOnClickListener(v -> deleteListener.onDelete(course));
        }

        /**
         * Builds the short abbreviation shown in the colored tile. For a
         * code like "CSCI 524" returns "524"; for a single token returns
         * the first 3 characters.
         */
        private String buildAbbr(String code) {
            if (code == null || code.isEmpty()) return "??";
            String[] parts = code.trim().split("\\s+");
            if (parts.length >= 2) {
                String last = parts[parts.length - 1];
                return last.length() > 3 ? last.substring(0, 3) : last;
            }
            return code.length() > 3 ? code.substring(0, 3) : code;
        }

        /**
         * Builds the initials shown in the instructor avatar circle.
         * Strips a leading "Prof." or "Dr." prefix first, then takes the
         * first letter of the first and last name parts.
         */
        private String buildInitials(String name) {
            if (name == null || name.isEmpty()) return "";
            String cleaned = name.replaceAll("(?i)^(prof\\.|dr\\.)\\s*", "").trim();
            String[] parts = cleaned.split("\\s+");
            if (parts.length == 0) return "";
            if (parts.length == 1)
                return String.valueOf(Character.toUpperCase(parts[0].charAt(0)));
            return String.valueOf(Character.toUpperCase(parts[0].charAt(0)))
                    + String.valueOf(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
    }
}