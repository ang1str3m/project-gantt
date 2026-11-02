package io.angistrem.employeemanagement.util;

public final class AppInfo {
    private static final String FALLBACK_VERSION = "2.3.local";
    private static final String VERSION_PROPERTY = "gantrem.version";

    private AppInfo() {
    }

    public static String currentVersion() {
        String propertyVersion = System.getProperty(VERSION_PROPERTY);
        if (propertyVersion != null && !propertyVersion.isBlank()) {
            return propertyVersion.trim();
        }

        String version = AppInfo.class.getPackage() == null ? null : AppInfo.class.getPackage().getImplementationVersion();
        if (version == null || version.isBlank()) {
            return FALLBACK_VERSION;
        }
        return version.trim();
    }
}
