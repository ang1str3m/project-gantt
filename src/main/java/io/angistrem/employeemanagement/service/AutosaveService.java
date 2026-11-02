package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.ProjectDocument;
import io.angistrem.employeemanagement.util.ProjectFileManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AutosaveService {
    private static final Duration DEFAULT_AUTOSAVE_DEBOUNCE = Duration.millis(800);
    private final AtomicReference<ProjectDocument> pendingProject = new AtomicReference<>();
    private final PauseTransition debounce;
    private final Consumer<ProjectDocument> projectWriter;

    public AutosaveService() {
        this(ProjectFileManager::autoSave, DEFAULT_AUTOSAVE_DEBOUNCE);
    }

    AutosaveService(Consumer<ProjectDocument> projectWriter, Duration debounceDuration) {
        this.projectWriter = projectWriter == null ? ProjectFileManager::autoSave : projectWriter;
        Duration safeDuration = debounceDuration == null ? DEFAULT_AUTOSAVE_DEBOUNCE : debounceDuration;
        this.debounce = new PauseTransition(safeDuration);
        debounce.setOnFinished(event -> flushPending());
    }

    public void save(ProjectDocument projectDocument) {
        if (projectDocument == null) {
            return;
        }
        pendingProject.set(projectDocument);
        if (Platform.isFxApplicationThread()) {
            debounce.playFromStart();
            return;
        }
        Platform.runLater(debounce::playFromStart);
    }

    public void flushPending() {
        ProjectDocument document = pendingProject.getAndSet(null);
        if (document != null) {
            projectWriter.accept(document);
        }
    }
}
