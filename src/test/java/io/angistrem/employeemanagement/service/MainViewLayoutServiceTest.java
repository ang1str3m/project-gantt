package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainViewLayoutServiceTest {

    private final MainViewLayoutService service = new MainViewLayoutService();

    @Test
    void resolveTableRowHeightShouldRespectTextStyle() {
        assertEquals(25.0, service.resolveTableRowHeight("compact"));
        assertEquals(31.0, service.resolveTableRowHeight("presentation"));
        assertEquals(28.0, service.resolveTableRowHeight("standard"));
    }

    @Test
    void resolveHeaderAndChartHeightShouldUseMinimums() {
        assertEquals(64.0, service.resolveHeaderHeight(0));
        assertEquals(70.0, service.resolveHeaderHeight(70));
        assertEquals(120.0, service.resolveChartHeight(1, 20.0));
        assertEquals(200.0, service.resolveChartHeight(10, 20.0));
    }

    @Test
    void accumulateHorizontalScrollbarHeightShouldKeepMaximumVisibleHorizontal() {
        double height = service.accumulateHorizontalScrollbarHeight(0, false, true, 12);
        height = service.accumulateHorizontalScrollbarHeight(height, true, false, 14);
        height = service.accumulateHorizontalScrollbarHeight(height, true, true, 10);
        height = service.accumulateHorizontalScrollbarHeight(height, true, true, 16);
        assertEquals(16.0, height);
    }
}
