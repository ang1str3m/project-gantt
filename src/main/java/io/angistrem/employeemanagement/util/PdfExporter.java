package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.controller.MainController;
import javafx.stage.Window;

public final class PdfExporter {

    private PdfExporter() {
    }

    public static boolean exportCurrentViewToPdf(MainController controller, Window window) {
        return ReportPrinter.showOutputCenter(controller, window, ReportPrinter.OutputTarget.PDF);
    }
}
