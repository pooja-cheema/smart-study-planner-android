package edu.niu.android.smartstudyplanner.ui.stats;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.StudySession;
import edu.niu.android.smartstudyplanner.utils.DateUtils;

public class SessionAdapter extends ListAdapter<StudySession, SessionAdapter.SessionViewHolder> {

    // Course id to short course name lookup, used to render the course
    // label on each row. Supplied by the host fragment.
    private Map<Long, String> courseNameMap = new HashMap<>();

    public SessionAdapter() {
        super(DIFF_CALLBACK);
    }

    /**
     * Pushes a fresh course id to name lookup into the adapter and
     * forces a rebind so labels reflect the new map.
     */
    public void setCourseNameMap(Map<Long, String> map) {
        this.courseNameMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    // Tells DiffUtil whether two rows represent the same session (by id)
    // and whether their visible content actually changed.
    private static final DiffUtil.ItemCallback<StudySession> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<StudySession>() {
                @Override
                public boolean areItemsTheSame(@NonNull StudySession a, @NonNull StudySession b) {
                    return a.getId() == b.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull StudySession a, @NonNull StudySession b) {
                    return a.getDurationSeconds() == b.getDurationSeconds()
                            && a.getStartTime() == b.getStartTime()
                            && a.getPomodoroCount() == b.getPomodoroCount();
                }
            };

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        holder.bind(getItem(position), courseNameMap);
    }

    /**
     * View holder for one session row. Holds direct references to every
     * view that bind() needs to update so we don't call findViewById on
     * every bind.
     */
    static class SessionViewHolder extends RecyclerView.ViewHolder {

        private final View     sidebar;
        private final TextView textIcon;
        private final TextView textDate;
        private final TextView textCourse;
        private final TextView textPomodoros;
        private final TextView textDuration;
        private final TextView textType;

        SessionViewHolder(View itemView) {
            super(itemView);
            sidebar       = itemView.findViewById(R.id.view_session_sidebar);
            textIcon      = itemView.findViewById(R.id.text_session_icon);
            textDate      = itemView.findViewById(R.id.text_session_date);
            textCourse    = itemView.findViewById(R.id.text_session_course);
            textPomodoros = itemView.findViewById(R.id.text_session_pomodoros);
            textDuration  = itemView.findViewById(R.id.text_session_duration);
            textType      = itemView.findViewById(R.id.text_session_type);
        }

        /**
         * Fills the row from the session and the supplied course name map.
         * Pomodoro and Free Study sessions get different colors and icons
         * so they're distinguishable at a glance.
         */
        void bind(StudySession session, Map<Long, String> courseNameMap) {
            float dp = itemView.getResources().getDisplayMetrics().density;

            // Pick the color/icon palette based on session type.
            boolean isPomodoro = session.getSessionType()
                    == StudySession.SESSION_TYPE_POMODORO;

            int color  = isPomodoro ? 0xFF0369A1 : 0xFF065F46;
            int iconBg = isPomodoro ? 0xFFE0F2FE : 0xFFDCFCE7;

            // Color the left sidebar to reinforce the type at a glance.
            sidebar.setBackgroundColor(color);

            // Rounded tile behind the emoji icon, tinted to match.
            GradientDrawable iconBgDrawable = new GradientDrawable();
            iconBgDrawable.setColor(iconBg);
            iconBgDrawable.setCornerRadius(14 * dp);
            textIcon.setBackground(iconBgDrawable);
            textIcon.setText(isPomodoro ? "🍅" : "📖");

            // Session start time formatted as a friendly date and time string.
            textDate.setText(DateUtils.formatDateTime(session.getStartTime()));

            // Course label. The stored name is "CODE - Name"; show only the
            // code portion. Hide the row entirely if the session has no
            // course (general study) or the course was deleted.
            if (textCourse != null) {
                Long courseId = session.getCourseId();
                if (courseId != null && courseNameMap.containsKey(courseId)) {
                    String name = courseNameMap.get(courseId);
                    if (name != null && name.contains(" - ")) {
                        name = name.substring(0, name.indexOf(" - "));
                    }
                    textCourse.setText(name);
                    textCourse.setVisibility(View.VISIBLE);
                } else {
                    textCourse.setVisibility(View.GONE);
                }
            }

            // Pomodoro count line. Hidden for free study sessions, since
            // there's no count to show.
            if (session.getPomodoroCount() > 0) {
                textPomodoros.setVisibility(View.VISIBLE);
                textPomodoros.setText(
                        "🍅 " + session.getPomodoroCount()
                                + " pomodoro" + (session.getPomodoroCount() == 1 ? "" : "s"));
            } else {
                textPomodoros.setVisibility(View.GONE);
            }

            // Right side duration and type label, both colored to match.
            textDuration.setText(session.getFormattedDuration());
            textDuration.setTextColor(color);
            textType.setText(isPomodoro ? "Pomodoro" : "Free Study");
        }
    }
}