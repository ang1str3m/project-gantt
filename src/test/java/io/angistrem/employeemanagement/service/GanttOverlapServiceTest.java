package io.angistrem.employeemanagement.service;

import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttOverlapServiceTest {

    private final GanttOverlapService service = new GanttOverlapService();

    @Test
    void overlapsTaskBarShouldIgnoreInvisibleAndNonBarShapes() {
        Rectangle invisible = new Rectangle(10, 10, 100, 20);
        invisible.setVisible(false);
        Line line = new Line(0, 0, 100, 100);
        BoundingBox bounds = new BoundingBox(20, 12, 30, 12);

        boolean overlaps = service.overlapsTaskBar(bounds, List.of(invisible, line), null, null);

        assertFalse(overlaps);
    }

    @Test
    void overlapsTaskBarShouldDetectFilledTaskRectangles() {
        Rectangle bar = new Rectangle(10, 10, 120, 18);
        bar.setFill(Color.web("#2563eb"));
        BoundingBox bounds = new BoundingBox(20, 12, 30, 12);

        boolean overlaps = service.overlapsTaskBar(bounds, List.of(bar), null, null);

        assertTrue(overlaps);
    }

    @Test
    void overlapsTaskBarShouldIgnoreTinyOrTransparentRectangles() {
        Rectangle tiny = new Rectangle(10, 10, 10, 6);
        tiny.setFill(Color.web("#2563eb"));
        Rectangle transparent = new Rectangle(10, 10, 40, 20);
        transparent.setFill(Color.TRANSPARENT);
        BoundingBox bounds = new BoundingBox(12, 12, 10, 10);

        boolean overlaps = service.overlapsTaskBar(bounds, List.of(tiny, transparent), null, null);

        assertFalse(overlaps);
    }

    @Test
    void overlapsTaskBarShouldIgnoreDragGuides() {
        Rectangle bar = new Rectangle(10, 10, 120, 18);
        bar.setFill(Color.web("#2563eb"));
        Rectangle dragGuide = new Rectangle(10, 10, 120, 18);
        dragGuide.setFill(Color.web("#ff0000"));
        BoundingBox bounds = new BoundingBox(20, 12, 30, 12);
        List<Node> nodes = List.of(dragGuide, bar);

        boolean overlapsIgnoringGuide = service.overlapsTaskBar(bounds, nodes, dragGuide, null);
        boolean overlapsWithoutIgnoring = service.overlapsTaskBar(bounds, nodes, null, null);

        assertTrue(overlapsIgnoringGuide);
        assertTrue(overlapsWithoutIgnoring);
    }
}
