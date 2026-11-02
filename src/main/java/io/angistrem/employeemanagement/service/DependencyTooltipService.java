package io.angistrem.employeemanagement.service;

public class DependencyTooltipService {

    public String tooltipText(String dependencyType, int lagDays) {
        String type = dependencyType == null || dependencyType.isBlank() ? "FS" : dependencyType.trim().toUpperCase();
        String lagText = lagDays == 0 ? "" : (lagDays > 0 ? "+" : "") + lagDays;
        return "\u0417\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u044c: " + type + lagText;
    }
}
