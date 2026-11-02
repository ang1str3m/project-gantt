package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.ProjectDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectFileManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void readProjectDocumentShouldSupportLegacyArrayRoot() throws Exception {
        Path json = tempDir.resolve("legacy-array.json");
        Files.writeString(json, """
                [
                  {"name":"Task A","position":"1."}
                ]
                """, StandardCharsets.UTF_8);

        ProjectDocument document = ProjectFileManager.readProjectDocument(json.toFile());

        assertNotNull(document);
        assertNotNull(document.getTasks());
        assertEquals(1, document.getTasks().size());
        assertEquals("Task A", document.getTasks().get(0).getName());
    }

    @Test
    void readProjectDocumentShouldHandleNullRootAsEmptyDocument() throws Exception {
        Path json = tempDir.resolve("null-root.json");
        Files.writeString(json, "null", StandardCharsets.UTF_8);

        ProjectDocument document = ProjectFileManager.readProjectDocument(json.toFile());

        assertNotNull(document);
        assertNotNull(document.getTasks());
        assertTrue(document.getTasks().isEmpty());
    }

    @Test
    void readProjectDocumentShouldInitializeMissingTasksList() throws Exception {
        Path json = tempDir.resolve("object-without-tasks.json");
        Files.writeString(json, """
                {
                  "projectName":"Demo",
                  "author":"QA"
                }
                """, StandardCharsets.UTF_8);

        ProjectDocument document = ProjectFileManager.readProjectDocument(json.toFile());

        assertEquals("Demo", document.getProjectName());
        assertEquals("QA", document.getAuthor());
        assertNotNull(document.getTasks());
        assertTrue(document.getTasks().isEmpty());
    }
}

