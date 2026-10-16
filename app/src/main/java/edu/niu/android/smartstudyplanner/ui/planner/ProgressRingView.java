package edu.niu.android.smartstudyplanner.ui.planner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ProgressRingView extends View {

    // Faint full circle drawn underneath the progress arc.
    private Paint bgPaint;

    // Yellow arc that grows as done increases.
    private Paint arcPaint;

    // Big "done/total" number drawn in the center.
    private Paint textPaint;

    // Small "done" label drawn underneath the number.
    private Paint subTextPaint;

    // Bounding rectangle the ring is drawn inside, recomputed in onDraw.
    private RectF oval;

    // Current values driving the arc length and the centered label.
    private int total = 0;
    private int done  = 0;

    public ProgressRingView(Context context) {
        super(context); init();
    }
    public ProgressRingView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public ProgressRingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    /**
     * Builds the four Paint objects once. Doing the setup here instead of
     * in onDraw avoids allocating Paints on every frame.
     */
    private void init() {
        // Background ring, semi transparent white.
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setColor(0x26FFFFFF);
        bgPaint.setStrokeWidth(21f);

        // Foreground arc, solid yellow with rounded ends.
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setColor(0xFFFFD600);
        arcPaint.setStrokeWidth(21f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Big centered number, e.g. "3/5".
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(64f);
        textPaint.setFakeBoldText(true);

        // Small "done" label underneath.
        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(0xFF90CAF9);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setTextSize(38f);
        subTextPaint.setFakeBoldText(true);

        oval = new RectF();
    }

    /**
     * Updates the values driving the ring and triggers a repaint.
     * Called from PlannerFragment whenever the task list changes.
     */
    public void setProgress(int done, int total) {
        this.done  = done;
        this.total = total;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Inset by half the stroke so the ring isn't clipped at the edges.
        float w = getWidth();
        float h = getHeight();
        float stroke = 18f;
        float pad = stroke / 2f + 4f;

        oval.set(pad, pad, w - pad, h - pad);

        // Faint full ring first, the yellow arc then sits on top of it.
        canvas.drawOval(oval, bgPaint);

        // Convert the done/total ratio into an arc sweep in degrees.
        // Starts from 90 (top of the circle) so progress reads clockwise from 12 o'clock.
        float sweep = total > 0 ? (float) done / total * 360f : 0f;
        canvas.drawArc(oval, -90f, sweep, false, arcPaint);

        // Center the labels. The text size and fudge offsets are tuned by eye
        // so the number and "done" label sit nicely inside the ring.
        float cx = w / 2f;
        float cy = h / 2f;
        canvas.drawText(done + "/" + total,
                cx, cy + textPaint.getTextSize() / 3f - 13f, textPaint);
        canvas.drawText("done",
                cx, cy + textPaint.getTextSize() / 3f + subTextPaint.getTextSize() + 2f - 13f,
                subTextPaint);
    }
}