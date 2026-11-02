package io.angistrem.employeemanagement.service;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.StrokeLineCap;

import java.util.List;

public class GanttDependencyPathService {

    public enum LineStyle {
        CLASSIC,
        METRO,
        FLOW,
        ROUNDED
    }

    public record VisualPaths(Path shadow, Path halo, Path colored) {
    }

    public VisualPaths createVisualPaths(List<Point2D> points, Color color, LineStyle lineStyle) {
        double radius = switch (lineStyle) {
            case CLASSIC -> 0.0;
            case METRO -> 5.5;
            case FLOW -> 11.0;
            case ROUNDED -> 18.0;
        };
        double shadowWidth = switch (lineStyle) {
            case CLASSIC -> 2.4;
            case METRO -> 3.1;
            case FLOW -> 3.8;
            case ROUNDED -> 4.9;
        };
        double haloWidth = switch (lineStyle) {
            case CLASSIC -> 1.2;
            case METRO -> 1.45;
            case FLOW -> 1.9;
            case ROUNDED -> 2.5;
        };
        double coloredWidth = switch (lineStyle) {
            case CLASSIC -> 1.0;
            case METRO -> 0.95;
            case FLOW -> 1.15;
            case ROUNDED -> 1.28;
        };

        Path shadow = buildDependencyPath(points, radius);
        shadow.setFill(null);
        shadow.setStroke(Color.rgb(15, 23, 42, lineStyle == LineStyle.ROUNDED ? 0.12 : (lineStyle == LineStyle.FLOW ? 0.10 : 0.08)));
        shadow.setStrokeWidth(shadowWidth);
        shadow.setStrokeLineCap(lineStyle == LineStyle.CLASSIC ? StrokeLineCap.BUTT : StrokeLineCap.ROUND);
        shadow.setMouseTransparent(true);

        Path halo = buildDependencyPath(points, radius);
        halo.setFill(null);
        halo.setStroke(Color.rgb(255, 255, 255, lineStyle == LineStyle.CLASSIC ? 0.88 : (lineStyle == LineStyle.FLOW ? 0.93 : 0.95)));
        halo.setStrokeWidth(haloWidth);
        halo.setStrokeLineCap(lineStyle == LineStyle.CLASSIC ? StrokeLineCap.BUTT : StrokeLineCap.ROUND);
        halo.setMouseTransparent(true);

        Path colored = buildDependencyPath(points, radius);
        colored.setFill(null);
        colored.setStroke(color);
        colored.setStrokeWidth(coloredWidth);
        colored.setStrokeLineCap(lineStyle == LineStyle.CLASSIC ? StrokeLineCap.BUTT : StrokeLineCap.ROUND);
        return new VisualPaths(shadow, halo, colored);
    }

    public Path buildHitPath(List<Point2D> points, LineStyle lineStyle) {
        double radius = switch (lineStyle) {
            case CLASSIC -> 0.0;
            case METRO -> 5.5;
            case FLOW -> 11.0;
            case ROUNDED -> 18.0;
        };
        Path hitPath = buildDependencyPath(points, radius);
        hitPath.setFill(null);
        hitPath.setStroke(Color.TRANSPARENT);
        hitPath.setStrokeWidth(12.0);
        hitPath.setStrokeLineCap(lineStyle == LineStyle.CLASSIC ? StrokeLineCap.BUTT : StrokeLineCap.ROUND);
        return hitPath;
    }

    private Path buildDependencyPath(List<Point2D> points, double radius) {
        return radius <= 0.01 ? buildPolylinePath(points) : buildRoundedPath(points, radius);
    }

    private Path buildPolylinePath(List<Point2D> points) {
        Path path = new Path();
        if (points == null || points.size() < 2) {
            return path;
        }
        Point2D first = points.get(0);
        path.getElements().add(new MoveTo(first.getX(), first.getY()));
        for (int i = 1; i < points.size(); i++) {
            Point2D point = points.get(i);
            path.getElements().add(new LineTo(point.getX(), point.getY()));
        }
        return path;
    }

    private Path buildRoundedPath(List<Point2D> points, double radius) {
        Path path = new Path();
        if (points == null || points.size() < 2) {
            return path;
        }

        path.getElements().add(new MoveTo(points.get(0).getX(), points.get(0).getY()));
        for (int i = 1; i < points.size() - 1; i++) {
            Point2D prev = points.get(i - 1);
            Point2D current = points.get(i);
            Point2D next = points.get(i + 1);

            if (i == points.size() - 2) {
                path.getElements().add(new LineTo(current.getX(), current.getY()));
                continue;
            }

            Point2D v1 = current.subtract(prev);
            Point2D v2 = next.subtract(current);
            double len1 = v1.magnitude();
            double len2 = v2.magnitude();

            if (len1 < 0.001 || len2 < 0.001) {
                path.getElements().add(new LineTo(current.getX(), current.getY()));
                continue;
            }

            double cornerRadius = Math.min(radius, Math.min(len1, len2) / 2.0);
            Point2D before = current.subtract(v1.normalize().multiply(cornerRadius));
            Point2D after = current.add(v2.normalize().multiply(cornerRadius));

            path.getElements().add(new LineTo(before.getX(), before.getY()));
            path.getElements().add(new QuadCurveTo(current.getX(), current.getY(), after.getX(), after.getY()));
        }

        Point2D last = points.get(points.size() - 1);
        path.getElements().add(new LineTo(last.getX(), last.getY()));
        return path;
    }
}
