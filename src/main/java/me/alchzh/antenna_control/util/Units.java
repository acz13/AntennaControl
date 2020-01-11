package me.alchzh.antenna_control.util;

public class Units {
    public static int u(double degrees) {
        return (int) (degrees * (Integer.MAX_VALUE / 180.0));
    }

    public static double d(int units) {
        return units * 180.0 / Integer.MAX_VALUE;
    }
}
