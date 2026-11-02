package io.angistrem.employeemanagement.service;

import java.util.Locale;

public class RuntimeModeService {

    public boolean isRunningFromIde() {
        return isRunningFromIde(
                System.getProperty("java.class.path", ""),
                System.getProperty("idea.active", ""),
                System.getProperty("sun.java.command", "")
        );
    }

    boolean isRunningFromIde(String classPath, String ideaActiveProperty, String javaCommand) {
        if ("true".equalsIgnoreCase(safe(ideaActiveProperty))) {
            return true;
        }

        String normalizedClassPath = safe(classPath).toLowerCase(Locale.ROOT);
        if (normalizedClassPath.contains("idea_rt.jar") || normalizedClassPath.contains("idea_rt")) {
            return true;
        }

        String normalizedCommand = safe(javaCommand).toLowerCase(Locale.ROOT);
        return normalizedCommand.contains("com.intellij.rt")
                || normalizedCommand.contains("idea_rt")
                || normalizedCommand.contains("org.jetbrains");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
