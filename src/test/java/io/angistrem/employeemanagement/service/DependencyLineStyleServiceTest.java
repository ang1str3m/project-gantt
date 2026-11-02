package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DependencyLineStyleServiceTest {

    private final DependencyLineStyleService service = new DependencyLineStyleService();

    @Test
    void resolveShouldReturnRoundedByDefault() {
        assertEquals(GanttDependencyPathService.LineStyle.ROUNDED, service.resolve((Settings) null));
        assertEquals(GanttDependencyPathService.LineStyle.ROUNDED, service.resolve((String) null));
        assertEquals(GanttDependencyPathService.LineStyle.ROUNDED, service.resolve("unknown"));
    }

    @Test
    void resolveShouldMapKnownStyleValues() {
        assertEquals(GanttDependencyPathService.LineStyle.CLASSIC, service.resolve("classic"));
        assertEquals(GanttDependencyPathService.LineStyle.METRO, service.resolve("metro"));
        assertEquals(GanttDependencyPathService.LineStyle.FLOW, service.resolve("flow"));
        assertEquals(GanttDependencyPathService.LineStyle.ROUNDED, service.resolve("premium"));
        assertEquals(GanttDependencyPathService.LineStyle.ROUNDED, service.resolve("rounded"));
    }

    @Test
    void resolveShouldUseSettingsValue() {
        Settings settings = new Settings();
        settings.setDependencyLineStyle("flow");
        assertEquals(GanttDependencyPathService.LineStyle.FLOW, service.resolve(settings));
    }
}
