/*
 * Copyright (c) 2026.
 */

package io.angistrem.employeemanagement.model;

import java.util.ArrayList;
import java.util.List;

public class Settings {
    private String projectName = "\u041d\u043e\u0432\u044b\u0439 \u043f\u0440\u043e\u0435\u043a\u0442";
    private String authorProject = "";
    private String theme = "light";
    private boolean autoLogin = false;
    private String lastUser = "";
    private String autoLoginVersion = "";
    private String autoLoginMachineId = "";
    private String defaultProjectDirectory = "";
    private String defaultPdfDirectory = "";
    private String defaultCsvDirectory = "";
    private boolean keepDialogsInLastDir = true;

    private String taskColor = "#2563EB";
    private String criticalTaskColor = "#DC2626";
    private String subTaskColor = "#16A34A";

    private String priorityHighColor = "#DC2626";
    private String priorityMediumColor = "#2563EB";
    private String priorityLowColor = "#0F766E";

    private String dependencyFsColor = "#7C848F";
    private String dependencySsColor = "#2563EB";
    private String dependencyFfColor = "#0F766E";
    private String dependencySfColor = "#B45309";
    private String dependencyLineStyle = "rounded";
    private String textStyle = "standard";
    private String fontFamily = "Segoe UI";

    private String hotkeyNewProject = "Ctrl+N";
    private String hotkeyOpenProject = "Ctrl+O";
    private String hotkeySaveProject = "Ctrl+S";
    private String hotkeySaveAsProject = "Ctrl+Shift+S";
    private String hotkeyPrintProject = "Ctrl+P";
    private String hotkeyExportPdf = "Ctrl+Shift+P";
    private String hotkeyExportCsv = "Ctrl+Shift+E";
    private String hotkeyFocusSearch = "Ctrl+F";
    private String hotkeyRecalculate = "F5";
    private String hotkeyZoomIn = "Ctrl+Equals";
    private String hotkeyZoomOut = "Ctrl+Minus";
    private String hotkeyZoomReset = "Ctrl+0";
    private String hotkeyShowCriticalPath = "Ctrl+Shift+C";
    private String hotkeyShowHelp = "F1";
    private String hotkeyUndoDelete = "Ctrl+Z";
    private List<String> recentProjectPaths = new ArrayList<>();

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public String getLastUser() {
        return lastUser;
    }

    public void setLastUser(String lastUser) {
        this.lastUser = lastUser;
    }

    public String getAutoLoginVersion() {
        return autoLoginVersion;
    }

    public void setAutoLoginVersion(String autoLoginVersion) {
        this.autoLoginVersion = autoLoginVersion == null ? "" : autoLoginVersion.trim();
    }

    public String getAutoLoginMachineId() {
        return autoLoginMachineId;
    }

    public void setAutoLoginMachineId(String autoLoginMachineId) {
        this.autoLoginMachineId = autoLoginMachineId == null ? "" : autoLoginMachineId.trim();
    }

    public String getAuthorProject() {
        return authorProject;
    }

    public void setAuthorProject(String authorProject) {
        this.authorProject = authorProject;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName == null ? "" : projectName.trim();
    }

    public String getDefaultProjectDirectory() {
        return defaultProjectDirectory;
    }

    public void setDefaultProjectDirectory(String defaultProjectDirectory) {
        this.defaultProjectDirectory = defaultProjectDirectory == null ? "" : defaultProjectDirectory;
    }

    public String getDefaultPdfDirectory() {
        return defaultPdfDirectory;
    }

    public void setDefaultPdfDirectory(String defaultPdfDirectory) {
        this.defaultPdfDirectory = defaultPdfDirectory == null ? "" : defaultPdfDirectory;
    }

    public String getDefaultCsvDirectory() {
        return defaultCsvDirectory;
    }

    public void setDefaultCsvDirectory(String defaultCsvDirectory) {
        this.defaultCsvDirectory = defaultCsvDirectory == null ? "" : defaultCsvDirectory;
    }

    public boolean isKeepDialogsInLastDir() {
        return keepDialogsInLastDir;
    }

    public void setKeepDialogsInLastDir(boolean keepDialogsInLastDir) {
        this.keepDialogsInLastDir = keepDialogsInLastDir;
    }

    public String getTaskColor() {
        return taskColor;
    }

    public void setTaskColor(String taskColor) {
        this.taskColor = normalizeColor(taskColor, "#2563EB");
    }

    public String getCriticalTaskColor() {
        return criticalTaskColor;
    }

    public void setCriticalTaskColor(String criticalTaskColor) {
        this.criticalTaskColor = normalizeColor(criticalTaskColor, "#DC2626");
    }

    public String getSubTaskColor() {
        return subTaskColor;
    }

    public void setSubTaskColor(String subTaskColor) {
        this.subTaskColor = normalizeColor(subTaskColor, "#16A34A");
    }

    public String getPriorityHighColor() {
        return priorityHighColor;
    }

    public void setPriorityHighColor(String priorityHighColor) {
        this.priorityHighColor = normalizeColor(priorityHighColor, "#DC2626");
    }

    public String getPriorityMediumColor() {
        return priorityMediumColor;
    }

    public void setPriorityMediumColor(String priorityMediumColor) {
        this.priorityMediumColor = normalizeColor(priorityMediumColor, "#2563EB");
    }

    public String getPriorityLowColor() {
        return priorityLowColor;
    }

    public void setPriorityLowColor(String priorityLowColor) {
        this.priorityLowColor = normalizeColor(priorityLowColor, "#0F766E");
    }

    public String getDependencyFsColor() {
        return dependencyFsColor;
    }

    public void setDependencyFsColor(String dependencyFsColor) {
        this.dependencyFsColor = normalizeColor(dependencyFsColor, "#7C848F");
    }

    public String getDependencySsColor() {
        return dependencySsColor;
    }

    public void setDependencySsColor(String dependencySsColor) {
        this.dependencySsColor = normalizeColor(dependencySsColor, "#2563EB");
    }

    public String getDependencyFfColor() {
        return dependencyFfColor;
    }

    public void setDependencyFfColor(String dependencyFfColor) {
        this.dependencyFfColor = normalizeColor(dependencyFfColor, "#0F766E");
    }

    public String getDependencySfColor() {
        return dependencySfColor;
    }

    public void setDependencySfColor(String dependencySfColor) {
        this.dependencySfColor = normalizeColor(dependencySfColor, "#B45309");
    }

    public String getDependencyLineStyle() {
        return dependencyLineStyle;
    }

    public void setDependencyLineStyle(String dependencyLineStyle) {
        if (dependencyLineStyle == null || dependencyLineStyle.isBlank()) {
            this.dependencyLineStyle = "rounded";
            return;
        }
        String normalized = dependencyLineStyle.trim().toLowerCase();
        this.dependencyLineStyle = switch (normalized) {
            case "classic", "rounded", "metro", "flow" -> normalized;
            case "premium" -> "rounded";
            default -> "rounded";
        };
    }

    public String getTextStyle() {
        return textStyle;
    }

    public void setTextStyle(String textStyle) {
        if (textStyle == null || textStyle.isBlank()) {
            this.textStyle = "standard";
            return;
        }
        String normalized = textStyle.trim().toLowerCase();
        this.textStyle = switch (normalized) {
            case "compact", "standard", "presentation" -> normalized;
            default -> "standard";
        };
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        if (fontFamily == null || fontFamily.isBlank()) {
            this.fontFamily = "Segoe UI";
            return;
        }
        this.fontFamily = fontFamily.trim();
    }

    public String getHotkeyNewProject() {
        return hotkeyNewProject;
    }

    public void setHotkeyNewProject(String hotkeyNewProject) {
        this.hotkeyNewProject = normalizeShortcut(hotkeyNewProject, "Ctrl+N");
    }

    public String getHotkeyOpenProject() {
        return hotkeyOpenProject;
    }

    public void setHotkeyOpenProject(String hotkeyOpenProject) {
        this.hotkeyOpenProject = normalizeShortcut(hotkeyOpenProject, "Ctrl+O");
    }

    public String getHotkeySaveProject() {
        return hotkeySaveProject;
    }

    public void setHotkeySaveProject(String hotkeySaveProject) {
        this.hotkeySaveProject = normalizeShortcut(hotkeySaveProject, "Ctrl+S");
    }

    public String getHotkeySaveAsProject() {
        return hotkeySaveAsProject;
    }

    public void setHotkeySaveAsProject(String hotkeySaveAsProject) {
        this.hotkeySaveAsProject = normalizeShortcut(hotkeySaveAsProject, "Ctrl+Shift+S");
    }

    public String getHotkeyPrintProject() {
        return hotkeyPrintProject;
    }

    public void setHotkeyPrintProject(String hotkeyPrintProject) {
        this.hotkeyPrintProject = normalizeShortcut(hotkeyPrintProject, "Ctrl+P");
    }

    public String getHotkeyExportPdf() {
        return hotkeyExportPdf;
    }

    public void setHotkeyExportPdf(String hotkeyExportPdf) {
        this.hotkeyExportPdf = normalizeShortcut(hotkeyExportPdf, "Ctrl+Shift+P");
    }

    public String getHotkeyExportCsv() {
        return hotkeyExportCsv;
    }

    public void setHotkeyExportCsv(String hotkeyExportCsv) {
        this.hotkeyExportCsv = normalizeShortcut(hotkeyExportCsv, "Ctrl+Shift+E");
    }

    public String getHotkeyFocusSearch() {
        return hotkeyFocusSearch;
    }

    public void setHotkeyFocusSearch(String hotkeyFocusSearch) {
        this.hotkeyFocusSearch = normalizeShortcut(hotkeyFocusSearch, "Ctrl+F");
    }

    public String getHotkeyRecalculate() {
        return hotkeyRecalculate;
    }

    public void setHotkeyRecalculate(String hotkeyRecalculate) {
        this.hotkeyRecalculate = normalizeShortcut(hotkeyRecalculate, "F5");
    }

    public String getHotkeyZoomIn() {
        return hotkeyZoomIn;
    }

    public void setHotkeyZoomIn(String hotkeyZoomIn) {
        this.hotkeyZoomIn = normalizeShortcut(hotkeyZoomIn, "Ctrl+Equals");
    }

    public String getHotkeyZoomOut() {
        return hotkeyZoomOut;
    }

    public void setHotkeyZoomOut(String hotkeyZoomOut) {
        this.hotkeyZoomOut = normalizeShortcut(hotkeyZoomOut, "Ctrl+Minus");
    }

    public String getHotkeyZoomReset() {
        return hotkeyZoomReset;
    }

    public void setHotkeyZoomReset(String hotkeyZoomReset) {
        this.hotkeyZoomReset = normalizeShortcut(hotkeyZoomReset, "Ctrl+0");
    }

    public String getHotkeyShowHelp() {
        return hotkeyShowHelp;
    }

    public String getHotkeyShowCriticalPath() {
        return hotkeyShowCriticalPath;
    }

    public void setHotkeyShowCriticalPath(String hotkeyShowCriticalPath) {
        this.hotkeyShowCriticalPath = normalizeShortcut(hotkeyShowCriticalPath, "Ctrl+Shift+C");
    }

    public void setHotkeyShowHelp(String hotkeyShowHelp) {
        this.hotkeyShowHelp = normalizeShortcut(hotkeyShowHelp, "F1");
    }

    public String getHotkeyUndoDelete() {
        return hotkeyUndoDelete;
    }

    public void setHotkeyUndoDelete(String hotkeyUndoDelete) {
        this.hotkeyUndoDelete = normalizeShortcut(hotkeyUndoDelete, "Ctrl+Z");
    }

    public List<String> getRecentProjectPaths() {
        if (recentProjectPaths == null) {
            recentProjectPaths = new ArrayList<>();
        }
        return new ArrayList<>(recentProjectPaths);
    }

    public void setRecentProjectPaths(List<String> recentProjectPaths) {
        this.recentProjectPaths = recentProjectPaths == null ? new ArrayList<>() : new ArrayList<>(recentProjectPaths);
    }

    private String normalizeColor(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("#")) {
            trimmed = "#" + trimmed;
        }
        if (trimmed.length() != 7) {
            return fallback;
        }
        return trimmed.toUpperCase();
    }

    private String normalizeShortcut(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
