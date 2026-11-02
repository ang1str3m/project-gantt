package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.TaskStatus;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencySpecBuildServiceTest {

    private final DependencySpecBuildService service = new DependencySpecBuildService();

    @Test
    void buildNormalizedSpecsShouldTrimPredecessorAndBuildDirectionFlags() {
        Task predecessor = task("1.");
        Task target = task("2.");
        target.setDependencies(List.of(new TaskDependency(" 1. ", "FS", 2)));

        Map<String, Rectangle> bars = new LinkedHashMap<>();
        bars.put("1.", new Rectangle(0, 0, 100, 20));
        bars.put("2.", new Rectangle(120, 0, 100, 20));

        Map<String, Object> itemsByKey = new LinkedHashMap<>();
        itemsByKey.put("1.", predecessor);
        itemsByKey.put("2.", target);

        List<DependencySpecBuildService.DependencyRenderSpec> specs =
                service.buildNormalizedSpecs(bars, itemsByKey, List.of(predecessor, target));

        assertEquals(1, specs.size());
        DependencySpecBuildService.DependencyRenderSpec spec = specs.get(0);
        assertEquals("1.", spec.predecessorKey());
        assertEquals("2.", spec.targetKey());
        assertEquals("FS", spec.type());
        assertEquals(2, spec.lagDays());
        assertTrue(spec.fromRight());
        assertFalse(spec.toRight());
        assertEquals(0.0, spec.sourceOffsetY());
        assertEquals(0.0, spec.targetOffsetY());
    }

    @Test
    void buildNormalizedSpecsShouldDistributeOffsetsWhenEdgeHasMultipleConnections() {
        Task predecessor = task("1.");
        Task targetA = task("2.");
        targetA.setDependencies(List.of(new TaskDependency("1.", "FS", 0)));
        Task targetB = task("3.");
        targetB.setDependencies(List.of(new TaskDependency("1.", "FS", 0)));

        Map<String, Rectangle> bars = new LinkedHashMap<>();
        bars.put("1.", new Rectangle(0, 0, 100, 20));
        bars.put("2.", new Rectangle(120, 0, 100, 20));
        bars.put("3.", new Rectangle(120, 30, 100, 20));

        Map<String, Object> itemsByKey = new LinkedHashMap<>();
        itemsByKey.put("1.", predecessor);
        itemsByKey.put("2.", targetA);
        itemsByKey.put("3.", targetB);

        List<DependencySpecBuildService.DependencyRenderSpec> specs =
                service.buildNormalizedSpecs(bars, itemsByKey, List.of(predecessor, targetA, targetB));

        assertEquals(2, specs.size());
        assertTrue(specs.get(0).sourceOffsetY() < 0.0);
        assertTrue(specs.get(1).sourceOffsetY() > 0.0);
    }

    @Test
    void buildNormalizedSpecsShouldResolveSubTaskPredecessorFromTaskTree() {
        Task parent = task("1.");
        SubTask subTask = new SubTask();
        subTask.setSubPosition("1.1.");
        parent.setSubTasks(List.of(subTask));

        Task target = task("2.");
        target.setDependencies(List.of(new TaskDependency("1.1.", "SS", 1)));

        Map<String, Rectangle> bars = new LinkedHashMap<>();
        bars.put("1.1.", new Rectangle(0, 0, 80, 16));
        bars.put("2.", new Rectangle(120, 0, 100, 20));

        Map<String, Object> itemsByKey = new LinkedHashMap<>();
        itemsByKey.put("1.1.", subTask);
        itemsByKey.put("2.", target);

        List<DependencySpecBuildService.DependencyRenderSpec> specs =
                service.buildNormalizedSpecs(bars, itemsByKey, List.of(parent, target));

        assertEquals(1, specs.size());
        assertEquals("1.1.", specs.get(0).predecessorKey());
        assertEquals("SS", specs.get(0).type());
        assertFalse(specs.get(0).fromRight());
        assertFalse(specs.get(0).toRight());
    }

    private Task task(String position) {
        Task task = new Task("T", "C", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), TaskStatus.PLANNED, "M");
        task.setPosition(position);
        return task;
    }
}
