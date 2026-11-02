package io.angistrem.employeemanagement.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.angistrem.employeemanagement.model.ProjectDocument;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProjectFileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectFileManager.class);
    private static final int MAX_RECENT_PROJECTS = 8;

    public static final String AUTOSAVE_PATH = AppPaths.getAppDirectory()
            + File.separator + "autosave_project.json";

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public record LoadedProject(File file, ProjectDocument document) {
    }

    private ProjectFileManager() {
    }

    public static void autoSave(List<Task> tasks) {
        ProjectDocument document = new ProjectDocument();
        document.setTasks(tasks);
        autoSave(document);
    }

    public static void autoSave(ProjectDocument document) {
        try {
            File file = new File(AUTOSAVE_PATH);
            file.getParentFile().mkdirs();
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, document == null ? new ProjectDocument() : document);
        } catch (Exception e) {
            LOGGER.error("Failed to auto-save project to {}", AUTOSAVE_PATH, e);
        }
    }

    public static List<Task> autoLoad() {
        ProjectDocument document = autoLoadDocument();
        return document == null ? null : document.getTasks();
    }

    public static ProjectDocument autoLoadDocument() {
        try {
            File file = new File(AUTOSAVE_PATH);
            if (!file.exists()) {
                return null;
            }
            return readProjectDocument(file);
        } catch (Exception e) {
            LOGGER.error("Failed to auto-load project from {}", AUTOSAVE_PATH, e);
            return null;
        }
    }

    public static boolean hasAutoSave() {
        File file = new File(AUTOSAVE_PATH);
        return file.exists() && file.isFile() && file.length() > 0;
    }

    public static void clearAutoSave() {
        File file = new File(AUTOSAVE_PATH);
        if (file.exists() && !file.delete()) {
            LOGGER.warn("Failed to delete autosave file {}", AUTOSAVE_PATH);
        }
    }

    public static boolean saveTasksToFile(List<Task> tasks, Window window) {
        ProjectDocument document = new ProjectDocument();
        document.setTasks(tasks);
        return saveProjectDocumentToFile(document, window);
    }

    public static boolean saveProjectDocumentToFile(ProjectDocument document, Window window) {
        FileChooser chooser = createProjectChooser("Сохранить проект");
        File file = chooser.showSaveDialog(window);
        if (file == null) {
            return false;
        }

        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, document == null ? new ProjectDocument() : document);
            registerRecentProject(file);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save project to file {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    public static List<Task> loadTasksFromFile(Window window) {
        LoadedProject loadedProject = loadProjectDocumentFromFile(window);
        return loadedProject == null ? null : loadedProject.document().getTasks();
    }

    public static LoadedProject loadProjectDocumentFromFile(Window window) {
        FileChooser chooser = createProjectChooser("Открыть проект");
        File file = chooser.showOpenDialog(window);
        if (file == null) {
            return null;
        }
        return loadProjectDocument(file);
    }

    public static LoadedProject loadProjectDocument(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }

        try {
            ProjectDocument document = readProjectDocument(file);
            registerRecentProject(file);
            return new LoadedProject(file, document);
        } catch (IOException e) {
            LOGGER.error("Failed to load project from file {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    public static ProjectDocument readProjectDocument(File file) throws IOException {
        JsonNode root = MAPPER.readTree(file);
        if (root == null || root.isNull()) {
            return new ProjectDocument();
        }

        if (root.isArray()) {
            List<Task> tasks = MAPPER.convertValue(
                    root,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Task.class)
            );
            ProjectDocument document = new ProjectDocument();
            document.setTasks(tasks);
            return document;
        }

        ProjectDocument document = MAPPER.treeToValue(root, ProjectDocument.class);
        if (document == null) {
            return new ProjectDocument();
        }
        if (document.getTasks() == null) {
            document.setTasks(List.of());
        }
        return document;
    }

    public static void registerRecentProject(File file) {
        if (file == null) {
            return;
        }

        try {
            String absolutePath = file.getCanonicalPath();
            Settings settings = AppSettings.get();
            if (settings == null) {
                settings = new Settings();
            }

            Set<String> recent = new LinkedHashSet<>();
            recent.add(absolutePath);
            for (String path : settings.getRecentProjectPaths()) {
                if (path == null || path.isBlank()) {
                    continue;
                }
                recent.add(path);
                if (recent.size() >= MAX_RECENT_PROJECTS) {
                    break;
                }
            }

            settings.setRecentProjectPaths(new ArrayList<>(recent));
            AppSettings.save(settings);
        } catch (IOException e) {
            LOGGER.warn("Failed to register recent project {}", file.getAbsolutePath(), e);
        }
    }

    public static List<File> getRecentProjectFiles() {
        Settings settings = AppSettings.get();
        if (settings == null) {
            return List.of();
        }

        List<File> files = new ArrayList<>();
        boolean changed = false;
        for (String path : settings.getRecentProjectPaths()) {
            if (path == null || path.isBlank()) {
                changed = true;
                continue;
            }

            File file = new File(path);
            if (file.exists() && file.isFile()) {
                files.add(file);
            } else {
                changed = true;
            }
        }

        if (changed) {
            settings.setRecentProjectPaths(files.stream().map(File::getAbsolutePath).toList());
            AppSettings.save(settings);
        }
        return files;
    }

    private static FileChooser createProjectChooser(String title) {
        Settings settings = AppSettings.get();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));

        if (settings != null && settings.getDefaultProjectDirectory() != null && !settings.getDefaultProjectDirectory().isBlank()) {
            File initialDir = new File(settings.getDefaultProjectDirectory());
            if (initialDir.exists() && initialDir.isDirectory()) {
                chooser.setInitialDirectory(initialDir);
            }
        }
        return chooser;
    }
}
