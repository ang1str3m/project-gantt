package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTaskBarPathServiceTest {

    private final MainTaskBarPathService service = new MainTaskBarPathService();

    @Test
    void buildPathShouldIncludeExpectedCommandsAndCloseShape() {
        String path = service.buildPath(10.0, 20.0, 100.0, 14.0);
        assertTrue(path.startsWith("M 10.0 20.0 H 110.0"));
        assertTrue(path.contains(" Q "));
        assertTrue(path.contains(" L "));
        assertTrue(path.endsWith(" Z"));
    }

    @Test
    void buildPathShouldClampNotchForSmallAndLargeHeights() {
        String small = service.buildPath(0.0, 0.0, 50.0, 5.0);
        String large = service.buildPath(0.0, 0.0, 50.0, 40.0);
        assertTrue(small.contains(" V 1.0"));
        assertTrue(large.contains(" V 33.0"));
    }
}
