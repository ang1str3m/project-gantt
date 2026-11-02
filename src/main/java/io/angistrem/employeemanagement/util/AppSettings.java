package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.repository.SettingRepository;

public final class AppSettings {
    private static final SettingRepository REPOSITORY = new SettingRepository();
    private static Settings cached = normalize(REPOSITORY.load());

    private AppSettings() {
    }

    public static synchronized Settings get() {
        cached = normalize(cached);
        return cached;
    }

    public static synchronized void save(Settings settings) {
        cached = normalize(settings);
        REPOSITORY.save(cached);
    }

    public static synchronized void reload() {
        cached = normalize(REPOSITORY.load());
    }

    private static Settings normalize(Settings settings) {
        Settings normalized = settings == null ? new Settings() : settings;

        if (normalized.getDefaultProjectDirectory() == null || normalized.getDefaultProjectDirectory().isBlank()) {
            normalized.setDefaultProjectDirectory(AppPaths.getDefaultProjectsDirectory());
        }
        if (normalized.getDefaultPdfDirectory() == null || normalized.getDefaultPdfDirectory().isBlank()) {
            normalized.setDefaultPdfDirectory(AppPaths.getDefaultPdfDirectory());
        }
        if (normalized.getDefaultCsvDirectory() == null || normalized.getDefaultCsvDirectory().isBlank()) {
            normalized.setDefaultCsvDirectory(AppPaths.getDefaultCsvDirectory());
        }
        if (normalized.getAuthorProject() == null || normalized.getAuthorProject().isBlank()) {
            String userName = System.getProperty("user.name");
            if (userName != null && !userName.isBlank()) {
                normalized.setAuthorProject(userName.trim());
            }
        }

        return normalized;
    }
}
