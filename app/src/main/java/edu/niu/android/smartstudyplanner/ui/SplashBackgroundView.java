/**************************************************************
 * Purpose: Custom View that draws the geometric background   *
 * for the splash screen: concentric blue rings, a dashed     *
 * gold orbit ring matching where the pills travel, depth     *
 * ellipses at the top and bottom, faint grid and diagonal    *
 * lines, scattered white star particles, and four compass    *
 * dots on the cardinal points of the orbit. Painted directly *
 * onto the Canvas in onDraw.                                 *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.DashPathEffect;
import android.util.AttributeSet;
import android.view.View;

public class SplashBackgroundView extends View {

    // Concentric blue rings drawn around the icon center.
    private Paint circlePaint;

    // Dashed gold orbit ring whose radius matches the pill orbit.
    private Paint dashedCirclePaint;

    // Faint grid lines: the main vertical/horizontal cross.
    private Paint linePaint;

    // Even fainter corner-to-corner diagonal lines.
    private Paint diagonalPaint;

    // Solid blue depth ellipses anchored at the top and bottom edges.
    private Paint depthPaint;

    // Small white circles used as scattered star particles.
    private Paint starPaint;

    // Cardinal compass dots on the orbit ring (gold and teal).
    private Paint compassGoldPaint;
    private Paint compassTealPaint;

    public SplashBackgroundView(Context context) {
        super(context);
        init();
    }

    public SplashBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SplashBackgroundView(Context context,
                                AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Builds every Paint used by onDraw exactly once. Allocating Paints
     * inside onDraw would create garbage on every frame, so we set them
     * up here and just retune their alpha during drawing when needed.
     */
    private void init() {
        float dp = getResources().getDisplayMetrics().density;

        // Thin blue ring color used for the four concentric circles.
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor(0xFF3949AB);
        circlePaint.setStrokeWidth(1.2f);

        // Dashed gold ring. Its radius lines up with the pill orbit in
        // SplashActivity so it visually traces the path the pills take.
        dashedCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashedCirclePaint.setStyle(Paint.Style.STROKE);
        dashedCirclePaint.setColor(0xFFFFD600);
        dashedCirclePaint.setStrokeWidth(1.8f);
        dashedCirclePaint.setAlpha(65);
        dashedCirclePaint.setPathEffect(
                new DashPathEffect(new float[]{12f * dp, 14f * dp}, 0f));

        // Main grid cross lines.
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(0xFF3949AB);
        linePaint.setStrokeWidth(0.8f);
        linePaint.setAlpha(45);

        // Diagonal lines from corner to corner.
        diagonalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diagonalPaint.setStyle(Paint.Style.STROKE);
        diagonalPaint.setColor(0xFF3949AB);
        diagonalPaint.setStrokeWidth(0.5f);
        diagonalPaint.setAlpha(25);

        // Filled deep-blue oval used for top and bottom depth zones.
        depthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        depthPaint.setStyle(Paint.Style.FILL);
        depthPaint.setColor(0xFF0D1B6E);

        // Solid white dots used for star particles.
        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setColor(0xFFFFFFFF);

        // Filled gold for the top/bottom compass markers.
        compassGoldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        compassGoldPaint.setStyle(Paint.Style.FILL);
        compassGoldPaint.setColor(0xFFFFD600);

        // Filled teal for the left/right compass markers.
        compassTealPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        compassTealPaint.setStyle(Paint.Style.FILL);
        compassTealPaint.setColor(0xFF00BFA5);
        compassTealPaint.setAlpha(180);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w  = getWidth();
        float h  = getHeight();
        float dp = getResources().getDisplayMetrics().density;

        // Anchor everything around the same icon center used in
        // activity_splash.xml: horizontal middle, 270dp from the top
        // (icon marginTop 210dp + half its height 60dp).
        float cx = w / 2f;
        float cy = 270f * dp;

        // Top depth ellipse. Starts above the screen so it reads as a
        // soft, cropped halo behind the icon.
        depthPaint.setAlpha(80);
        canvas.drawOval(
                cx - 240 * dp, -120 * dp,
                cx + 240 * dp, 140 * dp, depthPaint);

        // Bottom depth ellipse, dimmer counterpart anchored at the bottom edge.
        depthPaint.setAlpha(35);
        canvas.drawOval(
                cx - 200 * dp, h - 160 * dp,
                cx + 200 * dp, h + 80 * dp, depthPaint);

        // Four concentric rings around the icon, each a little fainter
        // than the last so they fade away from the center.
        float[] radii  = { 230 * dp, 180 * dp, 135 * dp, 50 * dp };
        int[]   alphas = { 45, 38, 30, 22 };

        for (int i = 0; i < radii.length; i++) {
            circlePaint.setAlpha(alphas[i]);
            canvas.drawCircle(cx, cy, radii[i], circlePaint);
        }

        // Dashed gold orbit ring. Same radius as the pills' orbit in
        // SplashActivity so the ring sits exactly where they travel.
        float orbitRadius = 165f * dp;
        canvas.drawCircle(cx, cy, orbitRadius, dashedCirclePaint);

        // Main vertical and horizontal cross lines through the icon center.
        linePaint.setAlpha(40);
        canvas.drawLine(cx, 0, cx, h, linePaint);
        canvas.drawLine(0, cy, w, cy, linePaint);

        // Secondary grid lines, dimmer, parallel to the main cross.
        linePaint.setAlpha(18);
        canvas.drawLine(0, cy - 110 * dp, w, cy - 110 * dp, linePaint);
        canvas.drawLine(0, cy + 110 * dp, w, cy + 110 * dp, linePaint);
        canvas.drawLine(cx - 100 * dp, 0, cx - 100 * dp, h, linePaint);
        canvas.drawLine(cx + 100 * dp, 0, cx + 100 * dp, h, linePaint);

        // Two corner-to-corner diagonals for extra depth.
        canvas.drawLine(0, 0, w, h, diagonalPaint);
        canvas.drawLine(w, 0, 0, h, diagonalPaint);

        // Scattered white star particles. Each row is
        // {fractional X, fractional Y, dp size, alpha}.
        float[][] stars = {
                {0.14f, 0.06f, 2.5f, 150},
                {0.80f, 0.05f, 2.0f, 130},
                {0.06f, 0.32f, 1.5f, 100},
                {0.92f, 0.28f, 1.5f, 100},
                {0.18f, 0.75f, 2.0f, 110},
                {0.84f, 0.78f, 1.5f, 90},
                {0.08f, 0.90f, 1.5f, 80},
                {0.92f, 0.86f, 2.0f, 100},
                {0.50f, 0.03f, 2.0f, 120},
                {0.68f, 0.09f, 1.5f, 90},
                {0.30f, 0.12f, 1.5f, 80},
                {0.88f, 0.55f, 1.5f, 70}
        };

        for (float[] s : stars) {
            starPaint.setAlpha((int) s[3]);
            canvas.drawCircle(
                    s[0] * w, s[1] * h, s[2] * dp, starPaint);
        }

        // Compass dots on the cardinal points of the orbit ring.
        // Top is the brightest gold; bottom is dim gold; left and right
        // are teal so the four points read as two pairs.
        compassGoldPaint.setAlpha(230);
        canvas.drawCircle(cx, cy - orbitRadius, 5 * dp, compassGoldPaint);
        compassGoldPaint.setAlpha(110);
        canvas.drawCircle(cx, cy + orbitRadius, 3.5f * dp, compassGoldPaint);
        compassTealPaint.setAlpha(170);
        canvas.drawCircle(cx + orbitRadius, cy, 3.5f * dp, compassTealPaint);
        canvas.drawCircle(cx - orbitRadius, cy, 3.5f * dp, compassTealPaint);
    }
}