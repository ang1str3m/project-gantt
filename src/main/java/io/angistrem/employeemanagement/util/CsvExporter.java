package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

public final class CsvExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvExporter.class);

    private CsvExporter() {
    }

    public static boolean exportToCsv(List<Task> tasks, Window window) {
        Settings settings = AppSettings.get();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт проекта в CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        chooser.setInitialFileName("project_export_" + LocalDate.now() + ".csv");
        if (settings != null && settings.getDefaultCsvDirectory() != null && !settings.getDefaultCsvDirectory().isBlank()) {
            File initialDir = new File(settings.getDefaultCsvDirectory());
            if (initialDir.exists() && initialDir.isDirectory()) {
                chooser.setInitialDirectory(initialDir);
            }
        }

        File file = chooser.showSaveDialog(window);
        if (file == null) {
            return false;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("type,position,name,company,start,end,status,priority,dependencies,critical,advance_date,ks_date,require_advance,require_ks,unit,volume_project,volume_actual,percent");
            writer.newLine();

            for (Task task : tasks) {
                writer.write(String.join(",",
                        "task",
                        csv(task.getPosition()),
                        csv(task.getName()),
                        csv(task.getCompany()),
                        csv(date(task.getStart())),
                        csv(date(task.getEnd())),
                        csv(task.getStatus()),
                        csv(task.getPriority()),
                        csv(formatDependencies(task.getDependencies())),
                        csv(String.valueOf(task.isCriticalDependency())),
                        csv(date(task.getContractAdvanceDate())),
                        csv(date(task.getKsPaymentDate())),
                        csv(String.valueOf(task.isRequireAdvanceBeforeStart())),
                        csv(String.valueOf(task.isRequireKsBeforeStart())),
                        csv(""),
                        csv(""),
                        csv(""),
                        csv(String.valueOf(task.getPercent()))
                ));
                writer.newLine();

                for (SubTask subTask : task.getObservableSubTasks()) {
                    writer.write(String.join(",",
                            "subtask",
                            csv(subTask.getSubPosition()),
                            csv(subTask.getName()),
                            csv(subTask.getCompany()),
                            csv(date(subTask.getStart())),
                            csv(date(subTask.getEnd())),
                            csv(subTask.getStatus()),
                            csv(task.getPriority()),
                            csv(formatDependencies(task.getDependencies())),
                            csv(String.valueOf(task.isCriticalDependency())),
                            csv(date(task.getContractAdvanceDate())),
                            csv(date(task.getKsPaymentDate())),
                            csv(String.valueOf(task.isRequireAdvanceBeforeStart())),
                            csv(String.valueOf(task.isRequireKsBeforeStart())),
                            csv(subTask.getUnitOfMeasurement()),
                            csv(String.valueOf(subTask.getVolumeProject())),
                            csv(String.valueOf(subTask.getVolumeActual())),
                            csv(String.valueOf(subTask.getPercent()))
                    ));
                    writer.newLine();
                }
            }

            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to export CSV to {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    private static String date(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private static String csv(String value) {
        String raw = value == null ? "" : value;
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    private static String formatDependencies(List<TaskDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return "";
        }

        return dependencies.stream()
                .map(d -> {
                    String predecessor = d.getPredecessorPosition() == null ? "" : d.getPredecessorPosition();
                    String type = d.getType() == null ? "FS" : d.getType();
                    int lag = d.getLagDays();
                    String lagPart = lag == 0 ? "" : (lag > 0 ? "+" : "") + lag;
                    return predecessor + ":" + type + lagPart;
                })
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
    }
}
