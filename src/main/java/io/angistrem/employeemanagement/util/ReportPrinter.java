package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.controller.MainController;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ReportPrinter {
    private ReportPrinter() {
    }

    public static void printFullProject(MainController controller, Window owner) {
        showOutputCenter(controller, owner, OutputTarget.PRINTER);
    }

    public static boolean exportProjectToPdf(MainController controller, Window owner, File outputFile) {
        if (!hasData(controller) || outputFile == null) {
            return false;
        }
        try {
            ProjectPdfReportBuilder.exportToPdf(controller, outputFile, new ProjectReportOptions(
                    ProjectReportOptions.ProjectPaperFormat.A3,
                    true,
                    ProjectReportOptions.ProjectReportScaleMode.AUTO_APP
            ));
            return true;
        } catch (IOException ex) {
            showAlert(owner, Alert.AlertType.ERROR, "PDF", "Не удалось сформировать PDF:\n" + ex.getMessage());
            return false;
        } finally {
            if (controller != null) {
                controller.recoverWindowAfterSystemDialog();
            }
        }
    }

    public static boolean showOutputCenter(MainController controller, Window owner, OutputTarget defaultTarget) {
        if (!hasData(controller)) {
            showAlert(owner, Alert.AlertType.INFORMATION, "Печать проекта", "Нет данных для вывода проекта.");
            return false;
        }

        OutputRequest request = showOutputDialog(owner, defaultTarget == null ? OutputTarget.PRINTER : defaultTarget);
        if (request == null) {
            return false;
        }

        return switch (request.target()) {
            case PRINTER -> printViaGeneratedPdf(controller, owner, request.options());
            case PDF -> exportViaOutputCenter(controller, owner, request.options());
        };
    }

    public enum OutputTarget {
        PRINTER("Принтер"),
        PDF("PDF");

        private final String label;

        OutputTarget(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record OutputRequest(OutputTarget target, ProjectReportOptions options) {
    }

    private static boolean printViaGeneratedPdf(MainController controller, Window owner, ProjectReportOptions options) {
        File tempFile = null;
        try {
            tempFile = Files.createTempFile("gantrem-print-", ".pdf").toFile();
            ProjectPdfReportBuilder.exportToPdf(controller, tempFile, options);

            try (PDDocument document = PDDocument.load(tempFile)) {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPageable(new PDFPageable(document));
                if (!job.printDialog()) {
                    return false;
                }
                job.print();
            }
            return true;
        } catch (Exception ex) {
            showAlert(owner, Alert.AlertType.ERROR, "Печать проекта", "Не удалось отправить проект на печать:\n" + ex.getMessage());
            return false;
        } finally {
            if (controller != null) {
                controller.recoverWindowAfterSystemDialog();
            }
            if (tempFile != null && tempFile.exists()) {
                tempFile.deleteOnExit();
            }
        }
    }

    private static boolean exportViaOutputCenter(MainController controller, Window owner, ProjectReportOptions options) {
        File outputFile = choosePdfDestination(owner);
        if (outputFile == null) {
            if (controller != null) {
                controller.recoverWindowAfterSystemDialog();
            }
            return false;
        }
        try {
            ProjectPdfReportBuilder.exportToPdf(controller, outputFile, options);
            return true;
        } catch (IOException ex) {
            showAlert(owner, Alert.AlertType.ERROR, "PDF", "Не удалось сформировать PDF:\n" + ex.getMessage());
            return false;
        } finally {
            if (controller != null) {
                controller.recoverWindowAfterSystemDialog();
            }
        }
    }

    private static OutputRequest showOutputDialog(Window owner, OutputTarget defaultTarget) {
        Dialog<OutputRequest> dialog = new Dialog<>();
        dialog.setTitle("Печать проекта");
        dialog.setHeaderText("Параметры печати и PDF");

        ChoiceBox<OutputTarget> targetBox = new ChoiceBox<>();
        targetBox.getItems().setAll(OutputTarget.PRINTER, OutputTarget.PDF);
        targetBox.setValue(defaultTarget);

        ChoiceBox<ProjectReportOptions.ProjectPaperFormat> paperBox = new ChoiceBox<>();
        paperBox.getItems().setAll(
                ProjectReportOptions.ProjectPaperFormat.A4,
                ProjectReportOptions.ProjectPaperFormat.A3,
                ProjectReportOptions.ProjectPaperFormat.A2,
                ProjectReportOptions.ProjectPaperFormat.A1
        );
        paperBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(ProjectReportOptions.ProjectPaperFormat format) {
                if (format == null) {
                    return "";
                }
                return switch (format) {
                    case A4 -> "A4 - стандартный лист";
                    case A3 -> "A3 - расширенный лист";
                    case A2 -> "A2 - большой формат";
                    case A1 -> "A1 - плоттер / широкий лист";
                };
            }

            @Override
            public ProjectReportOptions.ProjectPaperFormat fromString(String string) {
                return null;
            }
        });
        paperBox.setValue(ProjectReportOptions.ProjectPaperFormat.A3);

        ChoiceBox<String> orientationBox = new ChoiceBox<>();
        orientationBox.getItems().setAll("Альбомная", "Книжная");
        orientationBox.setValue("Альбомная");

        ChoiceBox<ProjectReportOptions.ProjectReportScaleMode> scaleBox = new ChoiceBox<>();
        scaleBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(ProjectReportOptions.ProjectReportScaleMode mode) {
                if (mode == null) {
                    return "";
                }
                return switch (mode) {
                    case AUTO_APP -> "Авто размер страницы (как в приложении)";
                    case CURRENT_TILED -> "Текущий масштаб";
                    case FIT_WIDTH -> "Уместить по ширине";
                    case FIT_HEIGHT -> "Уместить по высоте";
                    case FIT_PAGE -> "Уместить на один лист";
                };
            }

            @Override
            public ProjectReportOptions.ProjectReportScaleMode fromString(String string) {
                return null;
            }
        });

        Label description = new Label();
        description.setWrapText(true);
        description.setTextFill(Color.web("#64748b"));

        final boolean[] refreshing = {false};
        Runnable refreshModes = () -> {
            if (refreshing[0]) {
                return;
            }
            refreshing[0] = true;
            try {
                boolean pdfTarget = targetBox.getValue() == OutputTarget.PDF;
                ProjectReportOptions.ProjectReportScaleMode current = scaleBox.getValue();

                List<ProjectReportOptions.ProjectReportScaleMode> allowedModes = new ArrayList<>();
                if (pdfTarget) {
                    allowedModes.add(ProjectReportOptions.ProjectReportScaleMode.AUTO_APP);
                }
                allowedModes.add(ProjectReportOptions.ProjectReportScaleMode.CURRENT_TILED);
                allowedModes.add(ProjectReportOptions.ProjectReportScaleMode.FIT_WIDTH);
                allowedModes.add(ProjectReportOptions.ProjectReportScaleMode.FIT_HEIGHT);
                allowedModes.add(ProjectReportOptions.ProjectReportScaleMode.FIT_PAGE);

                scaleBox.getItems().setAll(allowedModes);
                if (current == null || !allowedModes.contains(current)) {
                    current = pdfTarget
                            ? ProjectReportOptions.ProjectReportScaleMode.AUTO_APP
                            : ProjectReportOptions.ProjectReportScaleMode.FIT_WIDTH;
                }
                if (scaleBox.getValue() != current) {
                    scaleBox.setValue(current);
                }

                boolean autoPdf = pdfTarget && current == ProjectReportOptions.ProjectReportScaleMode.AUTO_APP;
                paperBox.setDisable(autoPdf);
                orientationBox.setDisable(autoPdf);

                if (!autoPdf) {
                    ProjectReportOptions.ProjectPaperFormat recommendedPaper = recommendedPaper(current, pdfTarget);
                    String recommendedOrientation = recommendedOrientation(current, pdfTarget);
                    if (paperBox.getValue() != recommendedPaper) {
                        paperBox.setValue(recommendedPaper);
                    }
                    if (!recommendedOrientation.equals(orientationBox.getValue())) {
                        orientationBox.setValue(recommendedOrientation);
                    }
                }

                description.setText(switch (current) {
                    case AUTO_APP -> "Создаёт PDF по реальному размеру текущих таблицы и диаграммы. Это самый точный режим для сохранения текущего вида проекта.";
                    case CURRENT_TILED -> "Сохраняет текущий масштаб проекта. Если проект не помещается, вывод разбивается на несколько страниц без дополнительного ужатия.";
                    case FIT_WIDTH -> "Подгоняет проект по ширине страницы. Если по высоте места не хватает, документ автоматически делится на несколько страниц.";
                    case FIT_HEIGHT -> "Подгоняет проект по высоте страницы. Удобно, когда важно сохранить вертикальную читаемость, а по ширине можно разбить проект на несколько листов.";
                    case FIT_PAGE -> "Старается уместить всю таблицу и диаграмму на одном листе выбранного формата бумаги.";
                });
            } finally {
                refreshing[0] = false;
            }
        };

        targetBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshModes.run());
        scaleBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshModes.run());
        refreshModes.run();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Вывод:"), 0, 0);
        grid.add(targetBox, 1, 0);
        grid.add(new Label("Формат бумаги:"), 0, 1);
        grid.add(paperBox, 1, 1);
        grid.add(new Label("Ориентация:"), 0, 2);
        grid.add(orientationBox, 1, 2);
        grid.add(new Label("Масштаб:"), 0, 3);
        grid.add(scaleBox, 1, 3);
        grid.add(description, 0, 4, 2, 1);

        ButtonType okButton = new ButtonType("Продолжить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().setAll(okButton, cancelButton);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        SceneManager.applyThemeToDialog(dialog);
        dialog.setResultConverter(btn -> {
            if (btn != okButton) {
                return null;
            }
            boolean autoPdf = targetBox.getValue() == OutputTarget.PDF
                    && scaleBox.getValue() == ProjectReportOptions.ProjectReportScaleMode.AUTO_APP;
            ProjectReportOptions options = new ProjectReportOptions(
                    autoPdf ? ProjectReportOptions.ProjectPaperFormat.A3 : paperBox.getValue(),
                    autoPdf || !"Книжная".equals(orientationBox.getValue()),
                    scaleBox.getValue()
            );
            return new OutputRequest(targetBox.getValue(), options);
        });

        return dialog.showAndWait().orElse(null);
    }

    private static File choosePdfDestination(Window window) {
        var settings = AppSettings.get();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить PDF-отчёт");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-файлы (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("project_report_" + LocalDate.now() + ".pdf");
        if (settings != null && settings.getDefaultPdfDirectory() != null && !settings.getDefaultPdfDirectory().isBlank()) {
            File initialDir = new File(settings.getDefaultPdfDirectory());
            if (initialDir.exists() && initialDir.isDirectory()) {
                chooser.setInitialDirectory(initialDir);
            }
        }
        return chooser.showSaveDialog(window);
    }

    private static ProjectReportOptions.ProjectPaperFormat recommendedPaper(ProjectReportOptions.ProjectReportScaleMode mode, boolean pdfTarget) {
        return switch (mode) {
            case FIT_PAGE -> ProjectReportOptions.ProjectPaperFormat.A4;
            case FIT_WIDTH -> ProjectReportOptions.ProjectPaperFormat.A3;
            case FIT_HEIGHT -> ProjectReportOptions.ProjectPaperFormat.A3;
            case CURRENT_TILED -> pdfTarget ? ProjectReportOptions.ProjectPaperFormat.A3 : ProjectReportOptions.ProjectPaperFormat.A4;
            case AUTO_APP -> ProjectReportOptions.ProjectPaperFormat.A3;
        };
    }

    private static String recommendedOrientation(ProjectReportOptions.ProjectReportScaleMode mode, boolean pdfTarget) {
        return switch (mode) {
            case FIT_PAGE -> "Альбомная";
            case FIT_WIDTH -> "Альбомная";
            case FIT_HEIGHT -> "Книжная";
            case CURRENT_TILED -> pdfTarget ? "Альбомная" : "Книжная";
            case AUTO_APP -> "Альбомная";
        };
    }

    private static boolean hasData(MainController controller) {
        return controller != null && controller.getTasks() != null && !controller.getTasks().isEmpty();
    }

    private static void showAlert(Window owner, Alert.AlertType type, String title, String text) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        if (owner != null) {
            alert.initOwner(owner);
        }
        SceneManager.applyThemeToDialog(alert);
        alert.showAndWait();
    }
}
