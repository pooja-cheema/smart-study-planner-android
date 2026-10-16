/**************************************************************
 * Purpose: Animated splash screen shown on app launch. Six   *
 * feature pills orbit continuously around a central icon, a  *
 * row of three gold dots pulses in sequence, then the screen *
 * fades into MainActivity. Pills are repositioned every      *
 * frame using trigonometry and explicitly locked to a flat   *
 * rotation so the text never tilts as it travels.            *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import edu.niu.android.smartstudyplanner.databinding.ActivitySplashBinding;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    // Total time the splash stays on screen before transitioning to MainActivity.
    private static final long SPLASH_DURATION_MS = 8000L;

    // One full orbit revolution duration.
    private static final long ORBIT_DURATION_MS = 8000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Wait for the layout pass so views report real width/height before
        // we start computing orbit positions.
        binding.splashRoot.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Only need one layout pass, so detach this listener immediately.
                        binding.splashRoot.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);

                        startOrbitalAnimation();
                        startDotsPulse();

                        // Schedule the handoff to MainActivity.
                        binding.splashRoot.postDelayed(
                                SplashActivity.this::navigateToMain,
                                SPLASH_DURATION_MS);
                    }
                });
    }

    /**
     * Animates the six feature pills around the icon. A single ValueAnimator
     * loops 0 to 360 forever and drives every pill's position through
     * trigonometry. Pill rotation is forced to zero every frame so the
     * text always reads horizontally regardless of where it is on the orbit.
     */
    private void startOrbitalAnimation() {
        float dp = getResources().getDisplayMetrics().density;

        // Screen width is known after the layout pass.
        float screenW = binding.splashRoot.getWidth();

        // Icon is horizontally centered, so its X is the screen midpoint.
        // Y is the icon's marginTop plus half its height (in dp).
        float iconCenterX = screenW / 2f;
        float iconCenterY = 270f * dp;

        // Orbit radius. Sits outside the glow ring so pills stay clear of the icon.
        float orbitRadius = 160f * dp;

        // Each pill's starting angle, 60 degrees apart.
        // Negative 90 starts at the top, then we walk clockwise around the circle.
        float[] startDeg = { -90f, -30f, 30f, 90f, 150f, 210f };

        // The six pill views, ordered to match startDeg by index.
        View[] pills = {
                binding.pillTimer,
                binding.pillTasks,
                binding.pillGoals,
                binding.pillStats,
                binding.pillCards,
                binding.pillMusic
        };

        // Single animator that all six pills read from.
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 360f);
        anim.setDuration(ORBIT_DURATION_MS);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.RESTART);
        anim.setInterpolator(new LinearInterpolator());

        anim.addUpdateListener(animation -> {
            // Current orbital progress (0 to 360 degrees) shared by every pill.
            float progress = (float) animation.getAnimatedValue();

            for (int i = 0; i < pills.length; i++) {
                View pill = pills[i];
                if (pill == null) continue;

                // Pill's current angle on the orbit.
                double angleRad = Math.toRadians(startDeg[i] + progress);

                // Compute the target X and Y. Subtracting half the pill's
                // width/height aligns the pill's center with the orbit ring
                // instead of its top-left corner.
                float targetX = iconCenterX
                        + orbitRadius * (float) Math.cos(angleRad)
                        - pill.getWidth()  / 2f;

                float targetY = iconCenterY
                        + orbitRadius * (float) Math.sin(angleRad)
                        - pill.getHeight() / 2f;

                pill.setX(targetX);
                pill.setY(targetY);

                // Force a flat rotation every frame. Without this the pills
                // inherit the parent's transform and appear to spin in place
                // as they travel around the orbit.
                pill.setRotation(0f);
            }
        });

        anim.start();
    }

    /**
     * Starts the three gold dots pulsing in sequence at the bottom of the
     * screen. Each dot is offset by 200ms so the pulse reads as a wave.
     * Each dot also has its own resting alpha so the trio looks layered.
     */
    private void startDotsPulse() {
        // Front dot is full opacity at rest; the others fade back further.
        pulseDot(binding.dot1, 0,   1.0f);
        pulseDot(binding.dot2, 200, 0.4f);
        pulseDot(binding.dot3, 400, 0.15f);
    }

    /**
     * Builds a single looping pulse animation on one dot. A pulse is a
     * scale up to 1.6 with brightness rising to full, followed by a
     * scale back down with brightness returning to the resting alpha.
     */
    private void pulseDot(final View dot, long delay, final float restAlpha) {

        // Up phase: scale to 1.6 and bring alpha up to fully opaque.
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 1.6f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 1.6f);
        ObjectAnimator alphaUp  = ObjectAnimator.ofFloat(dot, "alpha", restAlpha, 1f);
        AnimatorSet up = new AnimatorSet();
        up.playTogether(scaleUpX, scaleUpY, alphaUp);
        up.setDuration(240);
        up.setInterpolator(new DecelerateInterpolator());

        // Down phase: scale back to 1.0 and fade back to resting alpha.
        ObjectAnimator scaleDnX = ObjectAnimator.ofFloat(dot, "scaleX", 1.6f, 1f);
        ObjectAnimator scaleDnY = ObjectAnimator.ofFloat(dot, "scaleY", 1.6f, 1f);
        ObjectAnimator alphaDn  = ObjectAnimator.ofFloat(dot, "alpha", 1f, restAlpha);
        AnimatorSet down = new AnimatorSet();
        down.playTogether(scaleDnX, scaleDnY, alphaDn);
        down.setDuration(240);
        down.setInterpolator(new DecelerateInterpolator());

        // Up then down. Initial start delay staggers each dot in the row.
        AnimatorSet pulse = new AnimatorSet();
        pulse.playSequentially(up, down);
        pulse.setStartDelay(delay);

        // Loop by restarting at the end. Skip if the activity has gone away
        // so we don't restart a pulse against torn-down views.
        pulse.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isFinishing() && !isDestroyed()) {
                    pulse.setStartDelay(0);
                    pulse.start();
                }
            }
        });

        pulse.start();
    }

    /**
     * Fades the splash root to transparent, then launches MainActivity
     * with the system fade transition for a smooth handoff.
     */
    private void navigateToMain() {
        binding.splashRoot.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out);
                    finish();
                })
                .start();
    }
}