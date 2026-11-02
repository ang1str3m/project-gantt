package io.angistrem.employeemanagement.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.stream.Stream;

public final class AppPaths {
    public static final String APP_DIR_NAME = ".gantrem_app";
    public static final String LEGACY_APP_DIR_NAME = ".awm_app";

    private AppPaths() {
    }

    public static String getAppDirectory() {
        Path currentDir = Path.of(System.getProperty("user.home"), APP_DIR_NAME);
        Path legacyDir = Path.of(System.getProperty("user.home"), LEGACY_APP_DIR_NAME);

        migrateLegacyDirectory(legacyDir, currentDir);
        return currentDir.toString();
    }

    public static String getWorkspaceDirectory() {
        Path documents = resolveDocumentsDirectory();
        Path workspace = documents.resolve("Gantrem");
        ensureDirectoryExists(workspace);
        return workspace.toString();
    }

    public static String getDefaultProjectsDirectory() {
        Path path = Path.of(getWorkspaceDirectory(), "Projects");
        ensureDirectoryExists(path);
        return path.toString();
    }

    public static String getDefaultPdfDirectory() {
        Path path = Path.of(getWorkspaceDirectory(), "PDF");
        ensureDirectoryExists(path);
        return path.toString();
    }

    public static String getDefaultCsvDirectory() {
        Path path = Path.of(getWorkspaceDirectory(), "CSV");
        ensureDirectoryExists(path);
        return path.toString();
    }

    public static String getCurrentMachineId() {
        String computer = firstNonBlank(
                System.getenv("COMPUTERNAME"),
                System.getenv("HOSTNAME")
        );
        String user = firstNonBlank(
                System.getProperty("user.name"),
                System.getenv("USERNAME")
        );
        return (safe(computer) + "::" + safe(user)).toLowerCase(Locale.ROOT);
    }

    private static void migrateLegacyDirectory(Path legacyDir, Path currentDir) {
        if (!Files.exists(legacyDir)) {
            ensureDirectoryExists(currentDir);
            return;
        }

        ensureDirectoryExists(currentDir);

        try (Stream<Path> paths = Files.walk(legacyDir)) {
            paths.forEach(source -> copyToCurrentDir(legacyDir, currentDir, source));
        } catch (IOException ignored) {
            // Fallback to current directory even if migration fails.
        }
    }

    private static void copyToCurrentDir(Path legacyDir, Path currentDir, Path source) {
        try {
            Path relative = legacyDir.relativize(source);
            Path target = currentDir.resolve(relative);
            if (Files.isDirectory(source)) {
                ensureDirectoryExists(target);
                return;
            }
            if (Files.notExists(target)) {
                ensureDirectoryExists(target.getParent());
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (IOException ignored) {
            // Keep startup resilient if a single file cannot be copied.
        }
    }

    private static void ensureDirectoryExists(Path dir) {
        if (dir == null) {
            return;
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create application directory: " + dir, e);
        }
    }

    private static Path resolveDocumentsDirectory() {
        Path userHome = Path.of(System.getProperty("user.home"));
        Path documents = userHome.resolve("Documents");
        if (Files.exists(documents) || Files.notExists(documents)) {
            ensureDirectoryExists(documents);
            return documents;
        }
        return userHome;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
