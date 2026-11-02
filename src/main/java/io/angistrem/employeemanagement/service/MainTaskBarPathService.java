package io.angistrem.employeemanagement.service;

public class MainTaskBarPathService {

    public String buildPath(double x, double y, double width, double height) {
        double notch = Math.min(7.0, Math.max(4.0, height * 0.55));
        double radius = Math.min(2.8, notch * 0.45);
        double bottom = y + height;
        double right = x + width;
        double innerLeftX = x + notch;
        double innerRightX = right - notch;
        double innerY = bottom - notch;

        return "M " + x + " " + y
                + " H " + right
                + " V " + innerY
                + " Q " + right + " " + bottom + " " + (innerRightX + radius) + " " + bottom
                + " Q " + innerRightX + " " + bottom + " " + innerRightX + " " + (bottom - radius)
                + " L " + innerLeftX + " " + (bottom - radius)
                + " Q " + innerLeftX + " " + bottom + " " + (innerLeftX - radius) + " " + bottom
                + " Q " + x + " " + bottom + " " + x + " " + innerY
                + " Z";
    }
}
