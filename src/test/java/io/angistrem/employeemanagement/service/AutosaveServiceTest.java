package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.ProjectDocument;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AutosaveServiceTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            new JFXPanel();
        } catch (Exception ignored) {
            // Toolkit may already be initialized in this JVM.
        }
    }

    @Test
    void saveShouldDebounceAndPersistOnlyLastPendingDocument() throws Exception {
        List<ProjectDocument> saved = new CopyOnWriteArrayList<>();
        AutosaveService autosaveService = new AutosaveService(saved::add, Duration.millis(120));

        ProjectDocument first = new ProjectDocument();
        first.setProjectName("first");
        ProjectDocument second = new ProjectDocument();
        second.setProjectName("second");
        ProjectDocument third = new ProjectDocument();
        third.setProjectName("third");

        autosaveService.save(first);
        autosaveService.save(second);
        autosaveService.save(third);

        waitForFx(280);

        assertEquals(1, saved.size());
        assertSame(third, saved.get(0));
    }

    @Test
    void flushPendingShouldPersistImmediatelyAndAvoidDuplicateWrite() throws Exception {
        List<ProjectDocument> saved = new CopyOnWriteArrayList<>();
        AutosaveService autosaveService = new AutosaveService(saved::add, Duration.millis(220));

        ProjectDocument document = new ProjectDocument();
        document.setProjectName("flush-now");
        autosaveService.save(document);
        autosaveService.flushPending();

        assertEquals(1, saved.size());
        assertSame(document, saved.get(0));

        waitForFx(320);
        assertEquals(1, saved.size());
    }

    private void waitForFx(long millis) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < millis) {
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(() -> { });
            }
            Thread.sleep(20);
        }
    }
}

