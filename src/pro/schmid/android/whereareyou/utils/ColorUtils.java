package pro.schmid.android.whereareyou.utils;

import android.graphics.Color;

public class ColorUtils {
	private static final int FILL_COLOR_ALPHA = 50;

	public static final int[] ACCURACY_STROKE_COLORS = {
			Color.BLUE,
			Color.CYAN,
			Color.RED,
			Color.GREEN,
			Color.MAGENTA,
			Color.GRAY,
			Color.YELLOW
	};
	public static final int[] ACCURACY_FILL_COLORS;
	public static final float[] MARKER_COLORS;
	private static final int COLORS_LENGTH;

	private static int sColorCounter = 0;

	static {
		COLORS_LENGTH = ACCURACY_STROKE_COLORS.length;

		ACCURACY_FILL_COLORS = new int[COLORS_LENGTH];
		MARKER_COLORS = new float[COLORS_LENGTH];

		for (int i = 0; i < COLORS_LENGTH; i++) {
			int currentcolor = ACCURACY_STROKE_COLORS[i];

			float hsv[] = new float[3];
			Color.colorToHSV(currentcolor, hsv);

			ACCURACY_FILL_COLORS[i] = Color.HSVToColor(FILL_COLOR_ALPHA, hsv);
			MARKER_COLORS[i] = hsv[0];
		}
	}

	public static void resetColor() {
		sColorCounter = 0;
	}

	public static int getCurrentColor() {
		return sColorCounter % COLORS_LENGTH;
	}

	public static void incrementColor() {
		++sColorCounter;
	}
}
