package io.angistrem.employeemanagement.service;

import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyRoutingServiceTest {

    private final DependencyRoutingService service = new DependencyRoutingService();

    @Test
    void routeSeedShouldBeStableAndSensitiveToInput() {
        int a = service.routeSeed("1.1.", "2.1.", "FS", 0, 0.0, 0.0);
        int b = service.routeSeed("1.1.", "2.1.", "FS", 0, 0.0, 0.0);
        int c = service.routeSeed("1.1.", "2.1.", "SS", 0, 0.0, 0.0);

        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void cycleShouldHandleNegativeSeedsAndZeroModulo() {
        assertEquals(1, service.cycle(-5, 3));
        assertEquals(0, service.cycle(4, 0));
    }

    @Test
    void reserveLaneYShouldCreateReservationAndAvoidHardConflict() {
        List<DependencyRoutingService.LaneReservation> reservations = new ArrayList<>();
        reservations.add(new DependencyRoutingService.LaneReservation(
                10.0, 120.0, 60.0, 9.0, 40.0, 90.0, 70.0
        ));

        double laneY = service.reserveLaneY(
                60.0,
                20.0,
                110.0,
                GanttDependencyPathService.LineStyle.FLOW,
                45.0,
                95.0,
                75.0,
                400.0,
                reservations
        );

        assertTrue(Math.abs(laneY - 60.0) >= 8.0);
        assertEquals(2, reservations.size());
    }

    @Test
    void buildRouteGeometryShouldReturnRoutePointsAndReserveLane() {
        List<DependencyRoutingService.LaneReservation> reservations = new ArrayList<>();
        DependencyRoutingService.DependencyRoute route = service.buildRouteGeometry(
                new DependencyRoutingService.DependencyRouteRequest(
                        "1.1.",
                        "2.1.",
                        "FS",
                        0,
                        0.0,
                        0.0,
                        GanttDependencyPathService.LineStyle.ROUNDED,
                        true,
                        false,
                        false,
                        100.0,
                        20.0,
                        40.0,
                        24.0,
                        220.0,
                        90.0,
                        40.0,
                        24.0,
                        28.0,
                        500.0,
                        280.0
                ),
                reservations
        );

        assertTrue(route.routePoints().size() >= 6);
        assertTrue(route.fromX() >= 8.0);
        assertTrue(route.toX() >= 8.0);
        assertTrue(route.arrowSize() > 0.0);
        assertEquals(1, reservations.size());
    }

    @Test
    void resolveArrowAngleShouldUseLastNonZeroSegment() {
        double angleRight = service.resolveArrowAngle(
                List.of(
                        new Point2D(10, 10),
                        new Point2D(20, 10),
                        new Point2D(20, 30)
                ),
                false
        );
        double angleDefault = service.resolveArrowAngle(
                List.of(new Point2D(10, 10)),
                true
        );

        assertTrue(angleRight > 1.4 && angleRight < 1.8);
        assertEquals(Math.PI, angleDefault, 1e-6);
    }
}
