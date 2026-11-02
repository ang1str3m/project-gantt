package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyRenderServiceTest {

    private final DependencyRenderService service = new DependencyRenderService();

    @Test
    void normalizeTypeShouldFallbackToFs() {
        assertEquals("FS", service.normalizeType(null));
        assertEquals("FS", service.normalizeType(" "));
        assertEquals("SS", service.normalizeType("ss"));
    }

    @Test
    void directionShouldMatchDependencyType() {
        assertTrue(service.fromRight("FS"));
        assertTrue(service.fromRight("FF"));
        assertFalse(service.fromRight("SS"));

        assertTrue(service.toRight("FF"));
        assertTrue(service.toRight("SF"));
        assertFalse(service.toRight("FS"));
    }

    @Test
    void lagTextShouldFormatSign() {
        assertEquals("", service.lagText(0));
        assertEquals("+3", service.lagText(3));
        assertEquals("-2", service.lagText(-2));
    }
}
