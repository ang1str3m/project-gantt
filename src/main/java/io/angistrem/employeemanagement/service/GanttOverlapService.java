package io.angistrem.employeemanagement.service;

import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

import java.util.List;

public class GanttOverlapService {

    public boolean overlapsTaskBar(BoundingBox bounds, List<Node> children, Node dragStartGuide, Node dragEndGuide) {
        if (bounds == null || children == null) {
            return false;
        }
        for (Node child : children) {
            if (child == null || !child.isVisible()) {
                continue;
            }
            if (!(child instanceof Rectangle || child instanceof SVGPath || child instanceof Polygon)) {
                continue;
            }
            if (child == dragStartGuide || child == dragEndGuide) {
                continue;
            }
            if (child instanceof Rectangle rect) {
                if (rect.getWidth() < 18.0 || rect.getHeight() < 8.0) {
                    continue;
                }
                if (rect.getFill() == null || Color.TRANSPARENT.equals(rect.getFill())) {
                    continue;
                }
            }
            if (child.getBoundsInParent().intersects(bounds)) {
                return true;
            }
        }
        return false;
    }
}
