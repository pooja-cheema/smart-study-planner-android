/**************************************************************
 * Purpose: Color helpers used across the app. Holds the      *
 * fixed palette assigned to courses, builds the foreground/  *
 * background pair for priority badges, and provides small    *
 * helpers to parse hex strings safely, darken a color, and   *
 * apply a custom alpha.                                      *
 **************************************************************/

package edu.niu.android.smartstudyplanner.utils;

import android.graphics.Color;

public class ColorUtils {

    /**
     * Fixed palette the user picks from in AddEditCourseActivity. Each
     * course's stored color is one of these strings; the same palette
     * shows up in the Stats pie chart and on every course tile.
     */
    public static final String[] COURSE_COLORS = {
            "#8E24AA", // Purple
            "#1E88E5", // Blue
            "#00897B", // Teal
            "#43A047", // Green
            "#FB8C00", // Orange
            "#6D4C41", // Brown
            "#00ACC1", // Cyan
            "#7CB342", // Light Green
            "#FFB300", // Amber
            "#0369A1", // Ocean Blue
            "#6D28D9", // Deep Purple
            "#065F46", // Forest Green
            "#B45309", // Amber Orange
            "#0F766E", // Teal
            "#9D174D", // Deep Rose
            "#3730A3", // Indigo
            "#0E7490", // Cyan
            "#4D7C0F", // Lime Green
            "#7C3AED", // Violet
            "#1D4ED8", // Royal Blue
    };

    /**
     * Returns the palette color at the given index, wrapping around with
     * modulo so callers don't have to worry about overshooting the array.
     */
    public static String getColorForIndex(int index) {
        return COURSE_COLORS[index % COURSE_COLORS.length];
    }

    /**
     * Safe parse for a hex color string. Falls back to a known good blue
     * if the input is null or malformed so the UI never crashes on bad data.
     */
    public static int parseColor(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.parseColor("#1E88E5");
        }
    }

    /**
     * Returns a {background, text} color pair for a task priority badge.
     * High maps to a red pair, Medium to orange, Low (default) to green.
     */
    public static int[] getPriorityColor(int priority) {
        switch (priority) {
            case 2:
                // High priority: red tones.
                return new int[]{
                        Color.parseColor("#FFEBEE"),
                        Color.parseColor("#C62828")
                };
            case 1:
                // Medium priority: orange tones.
                return new int[]{
                        Color.parseColor("#FFF3E0"),
                        Color.parseColor("#E65100")
                };
            default:
                // Low priority: green tones.
                return new int[]{
                        Color.parseColor("#E8F5E9"),
                        Color.parseColor("#2E7D32")
                };
        }
    }

    /**
     * Darkens a color by multiplying each RGB channel by the factor.
     * For example, factor 0.8 returns a 20% darker version of the color.
     * Alpha is preserved.
     */
    public static int darkenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.max((int) (Color.red(color) * factor), 0);
        int g = Math.max((int) (Color.green(color) * factor), 0);
        int b = Math.max((int) (Color.blue(color) * factor), 0);
        return Color.argb(a, r, g, b);
    }

    /**
     * Returns the same color with a custom alpha. 255 is fully opaque
     * and 0 is fully transparent.
     */
    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}