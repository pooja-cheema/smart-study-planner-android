/**************************************************************
 * Purpose: RecyclerView adapter for the Tasks list. Uses     *
 * ListAdapter with DiffUtil so updates animate cleanly.      *
 * Renders each task with a different color scheme depending  *
 * on whether it is completed, overdue, or pending, and shows *
 * type and priority badges plus an inline overdue badge on   *
 * the All tab. Click callbacks are exposed for the round     *
 * checkbox, the edit button, and the delete button so the    *
 * host fragment owns the actual actions.                     *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui.tasks;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.niu.android.smartstudyplanner.data.entities.Task;
import edu.niu.android.smartstudyplanner.utils.DateUtils;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    /**
     * Single click callback used by all three row actions (check, edit,
     * delete). The host fragment registers a separate instance for each.
     */
    public interface OnTaskClickListener {
        void onClick(Task task);
    }

    // Required at construction time; the others are optional and may stay null.
    private final OnTaskClickListener checkListener;
    private OnTaskClickListener editListener;
    private OnTaskClickListener deleteListener;

    // Tab constants used by setCurrentTab so the adapter can show or hide
    // the Overdue inline badge appropriately.
    public static final int TAB_PENDING   = 0;
    public static final int TAB_COMPLETED = 1;
    public static final int TAB_ALL       = 2;
    public static final int TAB_OVERDUE   = 3;

    // Currently active tab; affects card color and the Overdue badge.
    private int currentTab = TAB_PENDING;

    // Course id to course name lookup, supplied by the host fragment.
    private Map<Long, String> courseNameMap = new HashMap<>();

    /**
     * Tells the adapter which Tasks tab is currently visible so rows can
     * style themselves accordingly.
     */
    public void setCurrentTab(int tab) { this.currentTab = tab; }

    /**
     * Pushes a fresh course id to name lookup into the adapter and forces
     * a rebind so course labels reflect the new map.
     */
    public void setCourseNameMap(Map<Long, String> map) {
        this.courseNameMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    // Tells DiffUtil whether two rows represent the same task (by id) and
    // whether their visible content actually changed.
    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Task>() {
                @Override
                public boolean areItemsTheSame(@NonNull Task a, @NonNull Task b) {
                    return a.getId() == b.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Task a, @NonNull Task b) {
                    return a.isCompleted() == b.isCompleted()
                            && a.getTitle().equals(b.getTitle())
                            && a.getDueDate() == b.getDueDate()
                            && a.getPriority() == b.getPriority()
                            && a.getType() == b.getType();
                }
            };

    public TaskAdapter(OnTaskClickListener checkListener) {
        super(DIFF_CALLBACK);
        this.checkListener = checkListener;
    }

    /**
     * Registers the click handler for the row's edit button.
     */
    public void setOnEditClickListener(OnTaskClickListener l) { this.editListener = l; }

    /**
     * Registers the click handler for the row's delete button.
     */
    public void setOnDeleteClickListener(OnTaskClickListener l) { this.deleteListener = l; }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(edu.niu.android.smartstudyplanner.R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    /**
     * View holder for one task row. Holds direct references to every
     * view that bind() needs to update so we don't call findViewById on
     * every bind.
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final View priorityIndicator;
        private final FrameLayout checkboxContainer;
        private final View circleOuter;
        private final ImageView checkIcon;
        private final TextView textTitle;
        private final TextView textCourse;
        private final TextView textDueDate;
        private final TextView textType;
        private final TextView textPriority;
        private final TextView textStatusBadge;
        private final LinearLayout layoutBadges;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;
        private final CardView cardView;

        TaskViewHolder(View itemView) {
            super(itemView);
            cardView          = (CardView) itemView;
            priorityIndicator = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.view_priority_indicator);
            checkboxContainer = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.checkbox_complete);
            circleOuter       = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.circle_outer);
            checkIcon         = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.check_icon);
            textTitle         = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.text_task_title);
            textCourse        = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.text_course);
            textDueDate       = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.text_due_date);
            textType          = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.text_task_type);
            textPriority      = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.text_priority);
            textStatusBadge   = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.text_status_badge);
            layoutBadges      = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.layout_badges);
            btnEdit           = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.btn_edit);
            btnDelete         = itemView.findViewById(edu.niu.android.smartstudyplanner.R.id.btn_delete);
        }

        /**
         * Fills every view in the row from the given task. Picks the card
         * color scheme (mint for completed, rose for overdue, ocean blue
         * for pending), draws the circular checkbox in the matching state,
         * configures the badges, and wires up the action button clicks.
         */
        void bind(Task task) {
            float dp = itemView.getResources().getDisplayMetrics().density;

            // Title. Strip any leftover strikethrough and reset alpha so
            // a recycled view that previously rendered a completed task
            // doesn't carry that styling over.
            textTitle.setText(task.getTitle());
            textTitle.setPaintFlags(
                    textTitle.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            textTitle.setAlpha(1f);

            // Course label. Show only the course code part of the stored
            // "CODE - Name" string. Fall back to the id if we don't have
            // the course in the map yet.
            String courseName = courseNameMap.containsKey(task.getCourseId())
                    ? courseNameMap.get(task.getCourseId())
                    : "Course " + task.getCourseId();
            if (courseName != null && courseName.contains(" - ")) {
                courseName = courseName.substring(0, courseName.indexOf(" - "));
            }
            textCourse.setText(courseName);

            // Due date. Completed tasks show a plain absolute date; pending
            // tasks show a relative "in 2 days" line plus the absolute date.
            if (task.isCompleted()) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                textDueDate.setText("Due: " + sdf.format(new Date(task.getDueDate())));
            } else {
                textDueDate.setText(
                        DateUtils.getRelativeDueDate(task.getDueDate())
                                + "  •  "
                                + DateUtils.formatDate(task.getDueDate()));
            }

            textType.setText(task.getTypeLabel());
            textPriority.setText(task.getPriorityLabel());

            // Pick the color scheme for this row based on the task state
            // and the active tab. Completed wins regardless of overdue.
            int cardBg, stripColor, circleColor;
            boolean isOverdue = !task.isCompleted()
                    && task.getDueDate() > 0
                    && task.getDueDate() < System.currentTimeMillis();

            if (task.isCompleted()) {
                cardBg      = 0xFFDCFCE7;
                stripColor  = 0xFF065F46;
                circleColor = 0xFF065F46;
            } else if (currentTab == TAB_OVERDUE || isOverdue) {
                cardBg      = 0xFFFFF1F2;
                stripColor  = 0xFF9F1239;
                circleColor = 0xFF9F1239;
            } else {
                cardBg      = 0xFFE0F2FE;
                stripColor  = 0xFF0369A1;
                circleColor = 0xFF0369A1;
            }

            cardView.setCardBackgroundColor(cardBg);
            priorityIndicator.setBackgroundColor(stripColor);

            // Circular checkbox: filled circle with check mark when complete,
            // empty outlined ring when pending.
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            if (task.isCompleted()) {
                circle.setColor(circleColor);
                circle.setStroke(0, 0);
                checkIcon.setVisibility(View.VISIBLE);
            } else {
                circle.setColor(0x00000000);
                circle.setStroke((int)(2.5f * dp), circleColor);
                checkIcon.setVisibility(View.GONE);
            }
            circleOuter.setBackground(circle);

            // Tapping the circle is the "toggle complete" action.
            checkboxContainer.setOnClickListener(v -> {
                if (checkListener != null) checkListener.onClick(task);
            });

            // Badges: hidden on completed rows so the row stays uncluttered;
            // shown on pending rows with type, priority, and an inline
            // Overdue badge on the All tab.
            if (task.isCompleted()) {
                layoutBadges.setVisibility(View.GONE);
            } else {
                layoutBadges.setVisibility(View.VISIBLE);

                // Type pill: rounded background tinted with the strip color.
                GradientDrawable typeBg = new GradientDrawable();
                typeBg.setCornerRadius(20 * dp);
                typeBg.setColor(0x33000000);
                textType.setBackground(typeBg);
                textType.setTextColor(stripColor);

                // Priority pill: paired background and text color from the
                // priority palette.
                int[] pc = getPriorityColors(task.getPriority());
                GradientDrawable prioBg = new GradientDrawable();
                prioBg.setCornerRadius(20 * dp);
                prioBg.setColor(pc[0]);
                textPriority.setBackground(prioBg);
                textPriority.setTextColor(pc[1]);

                // Inline Overdue badge only shows on the All tab; the
                // Overdue tab itself doesn't need it because every row
                // there is overdue by definition.
                if (isOverdue && currentTab == TAB_ALL) {
                    textStatusBadge.setVisibility(View.VISIBLE);
                    textStatusBadge.setText("Overdue");
                    GradientDrawable statusBg = new GradientDrawable();
                    statusBg.setCornerRadius(20 * dp);
                    statusBg.setColor(0xFFFFCDD2);
                    textStatusBadge.setBackground(statusBg);
                    textStatusBadge.setTextColor(0xFF9F1239);
                } else {
                    textStatusBadge.setVisibility(View.GONE);
                }
            }

            // Wire up the edit and delete buttons only if the host fragment
            // registered listeners.
            if (btnEdit != null && editListener != null) {
                btnEdit.setOnClickListener(v -> editListener.onClick(task));
            }
            if (btnDelete != null && deleteListener != null) {
                btnDelete.setOnClickListener(v -> deleteListener.onClick(task));
            }
        }

        /**
         * Returns the [background, text] color pair for the priority pill.
         * High is rose, Low is mint, anything else (Medium) is amber.
         */
        private int[] getPriorityColors(int priority) {
            switch (priority) {
                case Task.PRIORITY_HIGH:
                    return new int[]{ 0xFFFFCDD2, 0xFFC62828 };
                case Task.PRIORITY_LOW:
                    return new int[]{ 0xFFA7F3D0, 0xFF065F46 };
                default:
                    return new int[]{ 0xFFFFE0B2, 0xFFE65100 };
            }
        }
    }
}