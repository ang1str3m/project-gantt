package io.angistrem.employeemanagement.util;

public record ProjectReportOptions(
        ProjectPaperFormat paperFormat,
        boolean landscape,
        ProjectReportScaleMode scaleMode
) {
    public ProjectReportOptions {
        paperFormat = paperFormat == null ? ProjectPaperFormat.A3 : paperFormat;
        scaleMode = scaleMode == null ? ProjectReportScaleMode.AUTO_APP : scaleMode;
    }

    public enum ProjectPaperFormat {
        A4, A3, A2, A1
    }

    public enum ProjectReportScaleMode {
        AUTO_APP,
        CURRENT_TILED,
        FIT_WIDTH,
        FIT_HEIGHT,
        FIT_PAGE
    }
}
