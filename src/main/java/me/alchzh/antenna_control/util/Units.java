package me.alchzh.antenna_control.util;

/**
 * Utility class for converting between 4 byte integer (unit) positions and degrees
 */
public class Units {
    private Units() {
    }

    /**
     * Converts degrees to units
     *
     * @param degrees Degrees from -180.0 to 180.0
     * @return 4 byte integer (unit) position
     */
    public static int u(double degrees) {
        return (int) (degrees * (Integer.MAX_VALUE / 180.0));
    }

    /**
     * Converts units to degrees
     *
     * @param units 4 byte integer (unit) position
     * @return Degrees from -180.0 to 180.0
     */
    public static double d(int units) {
        return units * 180.0 / Integer.MAX_VALUE;
    }
}
