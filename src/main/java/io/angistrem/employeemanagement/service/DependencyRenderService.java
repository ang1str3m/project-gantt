package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.TaskDependency;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DependencyRenderService {

    private final DependencyTooltipService dependencyTooltipService = new DependencyTooltipService();
    private final DependencyVisualTokensService dependencyVisualTokensService = new DependencyVisualTokensService();

    public record RenderRequest(
            Object targetTask,
            String predecessorKey,
            String targetKey,
            Rectangle from,
            Rectangle to,
            String typeRaw,
            int lagDays,
            boolean critical,
            TaskDependency dependency,
            GanttDependencyPathService.LineStyle lineStyle,
            double sourceOffsetY,
            double targetOffsetY,
            double rowHeight,
            double chartRightLimit,
            double chartHeight
    ) {
    }

    public void renderOrthogonalDependency(Pane chartPane,
                                           RenderRequest request,
                                           boolean highlighted,
                                           Color baseColor,
                                           List<BoundingBox> dependencyBadgeBounds,
                                           List<DependencyRoutingService.LaneReservation> laneReservations,
                                           DependencyRoutingService dependencyRoutingService,
                                           GanttDependencyPathService ganttDependencyPathService,
                                           DependencyInteractionService dependencyInteractionService,
                                           DependencyBadgeLayoutService dependencyBadgeLayoutService,
                                           Predicate<BoundingBox> overlapPredicate,
                                           Consumer<Object> editAction) {
        if (chartPane == null || request == null || dependencyRoutingService == null || ganttDependencyPathService == null) {
            return;
        }

        String type = normalizeType(request.typeRaw());
        Color color = dependencyVisualTokensService.resolveStrokeColor(baseColor, request.critical(), highlighted);
        boolean fromRight = fromRight(type);
        boolean toRight = toRight(type);

        DependencyRoutingService.DependencyRoute routeGeometry = dependencyRoutingService.buildRouteGeometry(
                new DependencyRoutingService.DependencyRouteRequest(
                        request.predecessorKey(),
                        request.targetKey(),
                        type,
                        request.lagDays(),
                        request.sourceOffsetY(),
                        request.targetOffsetY(),
                        request.lineStyle(),
                        fromRight,
                        toRight,
                        highlighted,
                        request.from().getX(),
                        request.from().getY(),
                        request.from().getWidth(),
                        request.from().getHeight(),
                        request.to().getX(),
                        request.to().getY(),
                        request.to().getWidth(),
                        request.to().getHeight(),
                        request.rowHeight(),
                        request.chartRightLimit(),
                        request.chartHeight()
                ),
                laneReservations
        );

        double toX = routeGeometry.toX();
        double toY = routeGeometry.toY();
        List<Point2D> routePoints = routeGeometry.routePoints();
        GanttDependencyPathService.VisualPaths route = ganttDependencyPathService.createVisualPaths(routePoints, color, request.lineStyle());
        Path haloPath = route.halo();
        Path coloredPath = route.colored();
        chartPane.getChildren().addAll(route.shadow(), route.halo(), route.colored());
        Path interactionPath = ganttDependencyPathService.buildHitPath(routePoints, request.lineStyle());
        chartPane.getChildren().add(interactionPath);
        if (highlighted) {
            haloPath.setStroke(dependencyVisualTokensService.highlightedHaloStroke());
            haloPath.setStrokeWidth(dependencyVisualTokensService.highlightedHaloWidth());
            coloredPath.setStrokeWidth(dependencyVisualTokensService.highlightedColoredPathWidth());
        }

        String lagText = lagText(request.lagDays());
        Tooltip tooltip = new Tooltip(dependencyTooltipService.tooltipText(type, request.lagDays()));
        tooltip.setShowDelay(Duration.millis(dependencyVisualTokensService.tooltipDelayMillis()));
        Tooltip.install(interactionPath, tooltip);

        double angle = dependencyRoutingService.resolveArrowAngle(routePoints, toRight);
        double arrowSize = routeGeometry.arrowSize();
        Polygon arrowHead = new Polygon(
                toX, toY,
                toX - arrowSize * Math.cos(angle - Math.PI / 6), toY - arrowSize * Math.sin(angle - Math.PI / 6),
                toX - arrowSize * Math.cos(angle + Math.PI / 6), toY - arrowSize * Math.sin(angle + Math.PI / 6)
        );
        arrowHead.setFill(color);
        arrowHead.setStroke(dependencyVisualTokensService.arrowStroke(highlighted));
        arrowHead.setStrokeWidth(dependencyVisualTokensService.arrowStrokeWidth(highlighted));
        Tooltip.install(arrowHead, tooltip);
        chartPane.getChildren().add(arrowHead);

        Label dependencyBadge = createDependencyBadge(
                type + lagText,
                routeGeometry.laneX(),
                routeGeometry.targetKneeX(),
                routeGeometry.laneY(),
                request.rowHeight(),
                request.chartRightLimit(),
                chartPane.getPrefHeight(),
                dependencyBadgeBounds,
                dependencyBadgeLayoutService,
                overlapPredicate
        );
        if (highlighted) {
            dependencyBadge.setStyle(dependencyInteractionService.highlightedBadgeStyle());
        }
        Tooltip.install(dependencyBadge, tooltip);
        dependencyBadge.setPickOnBounds(true);
        dependencyBadge.setMinWidth(dependencyVisualTokensService.badgeMinWidth(dependencyBadge.prefWidth(-1)));
        dependencyBadge.setMinHeight(dependencyVisualTokensService.badgeMinHeight(dependencyBadge.prefHeight(-1)));
        chartPane.getChildren().add(dependencyBadge);

        dependencyInteractionService.installHoverHandlers(
                interactionPath,
                arrowHead,
                dependencyBadge,
                coloredPath,
                highlighted
        );
        if (editAction != null) {
            dependencyInteractionService.installEditHandlers(
                    interactionPath,
                    dependencyBadge,
                    arrowHead,
                    () -> editAction.accept(request.targetTask())
            );
        }
    }

    public String normalizeType(String typeRaw) {
        if (typeRaw == null || typeRaw.isBlank()) {
            return "FS";
        }
        return typeRaw.trim().toUpperCase();
    }

    public boolean fromRight(String type) {
        return "FS".equals(type) || "FF".equals(type);
    }

    public boolean toRight(String type) {
        return "FF".equals(type) || "SF".equals(type);
    }

    public String lagText(int lagDays) {
        return lagDays == 0 ? "" : (lagDays > 0 ? "+" : "") + lagDays;
    }

    private Label createDependencyBadge(String text,
                                        double laneX,
                                        double targetKneeX,
                                        double laneY,
                                        double rowHeight,
                                        double chartRightLimit,
                                        double chartHeight,
                                        List<BoundingBox> occupiedBounds,
                                        DependencyBadgeLayoutService dependencyBadgeLayoutService,
                                        Predicate<BoundingBox> overlapPredicate) {
        Label label = new Label(text);
        label.setManaged(false);
        label.setStyle(dependencyBadgeLayoutService.baseBadgeStyle());
        label.autosize();

        DependencyBadgeLayoutService.BadgePlacement placement = dependencyBadgeLayoutService.computePlacement(
                laneX,
                targetKneeX,
                laneY,
                label.prefWidth(-1),
                label.prefHeight(-1),
                rowHeight,
                chartRightLimit,
                chartHeight,
                occupiedBounds,
                overlapPredicate
        );
        label.relocate(placement.x(), placement.y());
        if (occupiedBounds != null) {
            occupiedBounds.add(new BoundingBox(
                    placement.x(),
                    placement.y(),
                    placement.width(),
                    placement.height()
            ));
        }
        return label;
    }
}
