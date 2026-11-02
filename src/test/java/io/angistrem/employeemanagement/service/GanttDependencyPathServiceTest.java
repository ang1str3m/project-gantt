package io.angistrem.employeemanagement.service;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttDependencyPathServiceTest {

    private final GanttDependencyPathService service = new GanttDependencyPathService();

    @Test
    void createVisualPathsShouldReturnConfiguredLayers() {
        List<Point2D> points = List.of(
                new Point2D(10, 10),
                new Point2D(30, 10),
                new Point2D(30, 20),
                new Point2D(50, 20)
        );

        GanttDependencyPathService.VisualPaths paths = service.createVisualPaths(
                points,
                Color.RED,
                GanttDependencyPathService.LineStyle.ROUNDED
        );

        assertTrue(paths.shadow().getElements().size() >= 2);
        assertTrue(paths.halo().getElements().size() >= 2);
        assertTrue(paths.colored().getElements().size() >= 2);
        assertEquals(Color.RED, paths.colored().getStroke());
    }

    @Test
    void buildHitPathShouldBeTransparentAndWide() {
        List<Point2D> points = List.of(
                new Point2D(0, 0),
                new Point2D(20, 0),
                new Point2D(20, 10)
        );

        Path hit = service.buildHitPath(points, GanttDependencyPathService.LineStyle.CLASSIC);

        assertEquals(Color.TRANSPARENT, hit.getStroke());
        assertEquals(12.0, hit.getStrokeWidth());
        assertTrue(hit.getElements().size() >= 2);
    }

    @Test
    void createVisualPathsShouldSupportFlowStyle() {
        List<Point2D> points = List.of(
                new Point2D(5, 5),
                new Point2D(40, 5),
                new Point2D(40, 24),
                new Point2D(80, 24)
        );

        GanttDependencyPathService.VisualPaths paths = service.createVisualPaths(
                points,
                Color.web("#2563eb"),
                GanttDependencyPathService.LineStyle.FLOW
        );

        assertTrue(paths.shadow().getElements().size() >= 2);
        assertTrue(paths.halo().getElements().size() >= 2);
        assertTrue(paths.colored().getElements().size() >= 2);
    }
}
