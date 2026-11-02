package io.angistrem.employeemanagement.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskDependencyMappingTest {

    @Test
    void legacyDependencyPositionsAreMappedToEditableDependencies() {
        Task task = new Task();
        task.setDependencyPositions(List.of("1.", "2."));

        assertEquals(2, task.getDependencies().size());
        assertEquals("FS", task.getDependencies().get(0).getType());
        assertEquals(0, task.getDependencies().get(0).getLagDays());

        assertDoesNotThrow(() -> task.getDependencies().add(new TaskDependency("3.", "SS", 1)));
        assertEquals(3, task.getDependencies().size());
    }
}
