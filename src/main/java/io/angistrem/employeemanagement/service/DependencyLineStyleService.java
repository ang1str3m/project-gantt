package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.Settings;

import java.util.Locale;

public class DependencyLineStyleService {

    public GanttDependencyPathService.LineStyle resolve(Settings settings) {
        if (settings == null) {
            return GanttDependencyPathService.LineStyle.ROUNDED;
        }
        return resolve(settings.getDependencyLineStyle());
    }

    public GanttDependencyPathService.LineStyle resolve(String styleValue) {
        if (styleValue == null) {
            return GanttDependencyPathService.LineStyle.ROUNDED;
        }
        return switch (styleValue.trim().toLowerCase(Locale.ROOT)) {
            case "classic" -> GanttDependencyPathService.LineStyle.CLASSIC;
            case "metro" -> GanttDependencyPathService.LineStyle.METRO;
            case "flow" -> GanttDependencyPathService.LineStyle.FLOW;
            case "premium", "rounded" -> GanttDependencyPathService.LineStyle.ROUNDED;
            default -> GanttDependencyPathService.LineStyle.ROUNDED;
        };
    }
}
