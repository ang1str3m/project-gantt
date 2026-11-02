package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttPresentationServiceTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final GanttPresentationService service = new GanttPresentationService();
    private final GanttDateService dateService = new GanttDateService();

    @Test
    void colorByDependencyTypeShouldUseSettingsAndFallback() {
        Settings settings = new Settings();
        settings.setDependencyFsColor("invalid-color");
        settings.setDependencySsColor("#123456");

        Color ss = service.colorByDependencyType("SS", settings);
        Color fsFallback = service.colorByDependencyType("FS", settings);

        assertEquals(Color.web("#123456"), ss);
        assertEquals(Color.web("#7C848F"), fsFallback);
    }

    @Test
    void resolveTaskColorShouldRespectPriorityAndRiskFlags() {
        Task task = createTask("Task", "Высокий");
        task.setBudgetOverrun(true);
        assertEquals(Color.web("#7c3aed"), service.resolveTaskColor(task, new Settings()));

        task.setBudgetOverrun(false);
        task.setCriticalDependency(true);
        assertEquals(Color.web("#dc2626"), service.resolveTaskColor(task, null));

        task.setCriticalDependency(false);
        assertEquals(Color.web("#dc2626"), service.resolveTaskColor(task, null));
    }

    @Test
    void buildTaskTooltipShouldIncludeCoreFieldsAndNotes() {
        Task task = createTask("Foundation", "Средний");
        task.setCompany("Stroy");
        task.setResourceOverloaded(true);
        task.setResourceLoadSummary("Электрики 140%");
        task.setBudgetOverrun(true);
        task.setBudgetSummary("Бюджет: +12%");

        String tooltip = service.buildTaskTooltip(task, dateService, FORMATTER);

        assertTrue(tooltip.contains("Foundation"));
        assertTrue(tooltip.contains("Stroy"));
        assertTrue(tooltip.contains("01.04.2026 - 05.04.2026"));
        assertTrue(tooltip.contains("Электрики 140%"));
        assertTrue(tooltip.contains("Бюджет: +12%"));
    }

    @Test
    void buildSubTaskTooltipShouldIncludePeriodAndProgress() {
        Task parent = createTask("Parent", "Низкий");
        SubTask subTask = new SubTask(parent, "Install", "Crew A",
                LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3), "шт", 10, 5);

        String tooltip = service.buildSubTaskTooltip(subTask, dateService, FORMATTER);

        assertTrue(tooltip.contains("Install"));
        assertTrue(tooltip.contains("Crew A"));
        assertTrue(tooltip.contains("02.04.2026 - 03.04.2026"));
        assertTrue(tooltip.contains("50.0%"));
    }

    private Task createTask(String name, String priority) {
        Task task = new Task(name, "Company", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5), TaskStatus.PLANNED, priority);
        task.setPriority(priority);
        task.setStatus(TaskStatus.PLANNED);
        return task;
    }
}
