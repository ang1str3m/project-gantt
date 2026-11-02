package io.angistrem.employeemanagement.service;

import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DependencyRoutingService {

    public record LaneReservation(
            double minX,
            double maxX,
            double laneY,
            double clearance,
            double sourceY,
            double targetY,
            double laneX
    ) {
    }

    public record DependencyRouteRequest(
            String predecessorKey,
            String targetKey,
            String type,
            int lagDays,
            double sourceOffsetY,
            double targetOffsetY,
            GanttDependencyPathService.LineStyle lineStyle,
            boolean fromRight,
            boolean toRight,
            boolean highlighted,
            double fromBarX,
            double fromBarY,
            double fromBarWidth,
            double fromBarHeight,
            double toBarX,
            double toBarY,
            double toBarWidth,
            double toBarHeight,
            double rowHeight,
            double chartRightLimit,
            double chartBottom
    ) {
    }

    public record DependencyRoute(
            double fromX,
            double fromY,
            double toX,
            double toY,
            double sourceKneeX,
            double targetKneeX,
            double laneX,
            double laneY,
            double arrowSize,
            List<Point2D> routePoints
    ) {
    }

    public int routeSeed(String predecessorKey, String targetKey, String type, int lagDays, double sourceOffsetY, double targetOffsetY) {
        return Math.abs(Objects.hash(
                predecessorKey,
                targetKey,
                type,
                lagDays,
                Math.round(sourceOffsetY * 10.0),
                Math.round(targetOffsetY * 10.0)
        ));
    }

    public int cycle(int seed, int modulo) {
        if (modulo <= 0) {
            return 0;
        }
        return Math.floorMod(seed, modulo);
    }

    public double reserveLaneY(double preferredY,
                               double routeMinX,
                               double routeMaxX,
                               GanttDependencyPathService.LineStyle lineStyle,
                               double sourceY,
                               double targetY,
                               double laneX,
                               double chartBottom,
                               List<LaneReservation> reservations) {
        double baseClearance = switch (lineStyle) {
            case CLASSIC -> 7.0;
            case METRO -> 8.0;
            case FLOW -> 9.0;
            case ROUNDED -> 10.0;
        };
        double clampMinY = 12.0;
        double clampMaxY = Math.max(clampMinY + 2.0, chartBottom);

        List<Double> candidates = new ArrayList<>();
        candidates.add(preferredY);
        for (int step = 1; step <= 6; step++) {
            double shift = step * baseClearance;
            candidates.add(preferredY - shift);
            candidates.add(preferredY + shift);
        }

        double bestCandidate = Math.max(clampMinY, Math.min(clampMaxY, preferredY));
        double bestScore = Double.POSITIVE_INFINITY;
        for (Double candidateRaw : candidates) {
            double candidate = Math.max(clampMinY, Math.min(clampMaxY, candidateRaw));
            double score = Math.abs(candidate - preferredY) * 0.45;
            for (LaneReservation reservation : reservations) {
                double overlap = overlapLength(routeMinX, routeMaxX, reservation.minX() - 10.0, reservation.maxX() + 10.0);
                if (overlap <= 0.0) {
                    continue;
                }

                double distance = Math.abs(candidate - reservation.laneY());
                double requiredGap = Math.max(baseClearance, reservation.clearance());
                if (distance < requiredGap) {
                    score += 10_000.0 + (requiredGap - distance) * 500.0;
                } else {
                    score += (40.0 / Math.max(1.0, distance)) * (overlap / 40.0);
                }

                double direction = Math.signum(targetY - sourceY);
                double otherDirection = Math.signum(reservation.targetY() - reservation.sourceY());
                boolean oppositeDirections = direction != 0.0 && otherDirection != 0.0 && direction != otherDirection;
                if (oppositeDirections) {
                    score += Math.max(0.0, (requiredGap * 1.7 - distance)) * 35.0;
                }

                boolean orderInverted = (sourceY < reservation.sourceY() && targetY > reservation.targetY())
                        || (sourceY > reservation.sourceY() && targetY < reservation.targetY());
                if (orderInverted) {
                    score += Math.max(0.0, (requiredGap * 2.0 - distance)) * 50.0;
                }
            }
            if (score < bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }

        reservations.add(new LaneReservation(
                routeMinX,
                routeMaxX,
                bestCandidate,
                baseClearance,
                sourceY,
                targetY,
                laneX
        ));
        return bestCandidate;
    }

    public DependencyRoute buildRouteGeometry(DependencyRouteRequest request, List<LaneReservation> reservations) {
        double fromY = request.fromBarY() + request.fromBarHeight() / 2.0 + request.sourceOffsetY();
        double toY = request.toBarY() + request.toBarHeight() / 2.0 + request.targetOffsetY();

        double sourceEdgeX = request.fromRight()
                ? request.fromBarX() + request.fromBarWidth()
                : request.fromBarX();
        double targetEdgeX = request.toRight()
                ? request.toBarX() + request.toBarWidth()
                : request.toBarX();

        int routeSeed = routeSeed(
                request.predecessorKey(),
                request.targetKey(),
                request.type(),
                request.lagDays(),
                request.sourceOffsetY(),
                request.targetOffsetY()
        );

        double arrowSize = switch (request.lineStyle()) {
            case METRO -> request.highlighted() ? 13.0 : 11.0;
            case FLOW -> request.highlighted() ? 14.0 : 12.0;
            case ROUNDED -> request.highlighted() ? 14.0 : 12.0;
            default -> request.highlighted() ? 14.0 : 12.0;
        };

        double sourceInset = switch (request.lineStyle()) {
            case CLASSIC -> 6.0;
            case METRO -> 7.0;
            case FLOW -> 7.5;
            case ROUNDED -> 8.0;
        };
        double targetInset = switch (request.lineStyle()) {
            case CLASSIC -> 5.0;
            case METRO -> 6.0;
            case FLOW -> 6.5;
            case ROUNDED -> 7.0;
        };

        double fromX = sourceEdgeX + (request.fromRight() ? sourceInset : -sourceInset);
        double toX = targetEdgeX + (request.toRight() ? targetInset : -targetInset);
        double minRouteX = 8.0;
        double maxRouteX = Math.max(minRouteX + 1.0, request.chartRightLimit() - arrowSize - 2.0);
        fromX = Math.max(minRouteX, Math.min(maxRouteX, fromX));
        toX = Math.max(minRouteX, Math.min(maxRouteX, toX));

        int sourceDir = request.fromRight() ? 1 : -1;
        int targetDir = request.toRight() ? 1 : -1;
        double elbow = switch (request.lineStyle()) {
            case CLASSIC -> 12.0 + cycle(routeSeed, 3) * 2.0;
            case METRO -> 14.0 + cycle(routeSeed + 1, 3) * 3.0;
            case FLOW -> 16.0 + cycle(routeSeed + 2, 4) * 3.0;
            case ROUNDED -> 18.0 + cycle(routeSeed + 3, 4) * 4.0;
        };

        double sourceKneeX = fromX + sourceDir * elbow;
        double targetKneeX = toX + targetDir * elbow;
        if ("FF".equals(request.type())) {
            targetKneeX = toX + Math.max(8.0, elbow * 0.95);
        }

        boolean compactFsRoute = request.fromRight() && !request.toRight() && fromX + elbow < toX - elbow;
        boolean compactSfRoute = !request.fromRight() && request.toRight() && fromX - elbow > toX + elbow;

        double laneY;
        double rowDistance = Math.abs(toY - fromY);
        if (rowDistance < request.rowHeight() * 1.35) {
            boolean routeAbove = cycle(routeSeed, 2) == 0;
            double clearance = switch (request.lineStyle()) {
                case CLASSIC -> request.rowHeight() * (0.92 + cycle(routeSeed + 1, 2) * 0.14);
                case METRO -> request.rowHeight() * (1.10 + cycle(routeSeed + 2, 2) * 0.18);
                case FLOW -> request.rowHeight() * (1.20 + cycle(routeSeed + 3, 3) * 0.18);
                case ROUNDED -> request.rowHeight() * (1.35 + cycle(routeSeed + 4, 3) * 0.28);
            };
            laneY = routeAbove
                    ? Math.min(fromY, toY) - clearance
                    : Math.max(fromY, toY) + clearance;
        } else {
            laneY = ((fromY + toY) / 2.0) + ((cycle(routeSeed + 5, 3) - 1) * request.rowHeight() * switch (request.lineStyle()) {
                case CLASSIC -> 0.30;
                case METRO -> 0.42;
                case FLOW -> 0.52;
                case ROUNDED -> 0.62;
            });
        }

        double laneNudge = switch (request.lineStyle()) {
            case CLASSIC -> 6.0 + cycle(routeSeed + 6, 3) * 1.5;
            case METRO -> 8.0 + cycle(routeSeed + 7, 3) * 1.5;
            case FLOW -> 10.0 + cycle(routeSeed + 8, 4) * 2.0;
            case ROUNDED -> 12.0 + cycle(routeSeed + 9, 4) * 2.5;
        };
        double laneX;
        if (compactFsRoute) {
            laneX = Math.max(fromX + elbow, Math.min(toX - elbow, (fromX + toX) / 2.0));
            sourceKneeX = laneX;
            targetKneeX = laneX;
        } else if (compactSfRoute) {
            laneX = Math.min(fromX - elbow, Math.max(toX + elbow, (fromX + toX) / 2.0));
            sourceKneeX = laneX;
            targetKneeX = laneX;
        } else if (sourceDir == targetDir) {
            laneX = sourceDir > 0
                    ? Math.max(sourceKneeX, targetKneeX) + laneNudge
                    : Math.min(sourceKneeX, targetKneeX) - laneNudge;
        } else if (sourceDir > 0) {
            laneX = Math.max(sourceKneeX, targetKneeX) + laneNudge;
        } else {
            laneX = Math.min(sourceKneeX, targetKneeX) - laneNudge;
        }
        laneX = Math.max(minRouteX, Math.min(maxRouteX, laneX));
        sourceKneeX = Math.max(minRouteX, Math.min(maxRouteX, sourceKneeX));
        targetKneeX = Math.max(minRouteX, Math.min(maxRouteX, targetKneeX));

        double routeMinX = Math.min(Math.min(fromX, toX), Math.min(Math.min(sourceKneeX, targetKneeX), laneX));
        double routeMaxX = Math.max(Math.max(fromX, toX), Math.max(Math.max(sourceKneeX, targetKneeX), laneX));
        laneY = reserveLaneY(
                laneY,
                routeMinX,
                routeMaxX,
                request.lineStyle(),
                fromY,
                toY,
                laneX,
                request.chartBottom(),
                reservations
        );

        if (request.lineStyle() == GanttDependencyPathService.LineStyle.CLASSIC
                || request.lineStyle() == GanttDependencyPathService.LineStyle.METRO) {
            laneY = snapToGrid(laneY, request.rowHeight());
        }
        laneY = Math.max(12.0, Math.min(request.chartBottom(), laneY));

        List<Point2D> routePoints;
        if (compactFsRoute || compactSfRoute) {
            boolean almostSameRow = rowDistance < request.rowHeight() * 0.8;
            if (almostSameRow) {
                routePoints = List.of(
                        new Point2D(fromX, fromY),
                        new Point2D(sourceKneeX, fromY),
                        new Point2D(laneX, laneY),
                        new Point2D(targetKneeX, laneY),
                        new Point2D(targetKneeX, toY),
                        new Point2D(toX, toY)
                );
            } else {
                routePoints = List.of(
                        new Point2D(fromX, fromY),
                        new Point2D(sourceKneeX, fromY),
                        new Point2D(laneX, fromY),
                        new Point2D(laneX, toY),
                        new Point2D(targetKneeX, toY),
                        new Point2D(toX, toY)
                );
            }
        } else {
            routePoints = List.of(
                    new Point2D(fromX, fromY),
                    new Point2D(sourceKneeX, fromY),
                    new Point2D(laneX, fromY),
                    new Point2D(laneX, laneY),
                    new Point2D(targetKneeX, laneY),
                    new Point2D(targetKneeX, toY),
                    new Point2D(toX, toY)
            );
        }

        return new DependencyRoute(
                fromX,
                fromY,
                toX,
                toY,
                sourceKneeX,
                targetKneeX,
                laneX,
                laneY,
                arrowSize,
                routePoints
        );
    }

    public double resolveArrowAngle(List<Point2D> routePoints, boolean toRight) {
        if (routePoints == null || routePoints.size() < 2) {
            return toRight ? Math.PI : 0.0;
        }

        Point2D last = routePoints.get(routePoints.size() - 1);
        Point2D prev = routePoints.get(routePoints.size() - 2);
        for (int i = routePoints.size() - 2; i >= 0; i--) {
            Point2D candidate = routePoints.get(i);
            if (candidate.distance(last) > 0.5) {
                prev = candidate;
                break;
            }
        }

        if (prev.distance(last) <= 0.5) {
            return toRight ? Math.PI : 0.0;
        }
        return Math.atan2(last.getY() - prev.getY(), last.getX() - prev.getX());
    }

    private double snapToGrid(double value, double rowHeight) {
        if (rowHeight <= 0.0) {
            return value;
        }
        double halfStep = rowHeight / 2.0;
        if (halfStep <= 0.0) {
            return value;
        }
        return Math.round(value / halfStep) * halfStep;
    }

    private double overlapLength(double a1, double a2, double b1, double b2) {
        double left = Math.max(Math.min(a1, a2), Math.min(b1, b2));
        double right = Math.min(Math.max(a1, a2), Math.max(b1, b2));
        return Math.max(0.0, right - left);
    }
}
