package io.angistrem.employeemanagement.util;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.Style;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.svg.converter.SvgConverter;
import io.angistrem.employeemanagement.controller.MainController;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.view.GanttChart;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProjectPdfReportBuilder {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Locale RU = Locale.forLanguageTag("ru");
    static final float DOC_MARGIN = 24f;
    private static final float FIT_PAGE_MARGIN = 8f;
    private static final float CHART_PAGE_TITLE_SPACE = 26f;
    private static final float LIVE_SECTION_GAP = 1f;
    private static final float MAX_AUTO_CONTENT_WIDTH = 4200f;
    private static final float FIT_WIDTH_MIN_COMFORT_SCALE = 0.48f;
    private static final float FIT_WIDTH_COMFORT_FILL = 0.82f;
    private static final float CARD_PADDING = 10f;
    private static final float CARD_TITLE_HEIGHT = 28f;
    private static final float ROW_HEIGHT = 24f;
    private static final float TOP_HEADER_HEIGHT = 20f;
    private static final float MIDDLE_HEADER_HEIGHT = 20f;
    private static final float BOTTOM_HEADER_HEIGHT = 24f;
    private static final float EXPORT_TABLE_HEADER_HEIGHT = 166f;
    private static final float[] TABLE_WIDTHS = {78f, 360f, 190f, 78f, 78f, 54f, 84f, 62f, 82f};
    private static final String PDF_FONT_REGULAR = "C:/Windows/Fonts/arial.ttf";
    private static final String PDF_FONT_BOLD = "C:/Windows/Fonts/arialbd.ttf";
    private static final String TABLE_CSS = ProjectPdfReportBuilder.class
            .getResource("/io/angistrem/employeemanagement/styles/table.css")
            .toExternalForm();
    private static final String INK = "#0f172a";
    private static final String MUTED = "#64748b";
    private static final String SURFACE = "#fbfdff";
    private static final String SURFACE_ALT = "#f4f8fc";
    private static final String BORDER = "#dbe5f0";
    private static final String ACCENT = "#1d4ed8";
    private static final String ACCENT_SOFT = "#eff6ff";
    private static final String ACCENT_DARK = "#0f172a";

    private ProjectPdfReportBuilder() {
    }

    public static void exportToPdf(MainController controller, File outputFile, ProjectReportOptions options) throws IOException {
        if (options.scaleMode() == ProjectReportOptions.ProjectReportScaleMode.AUTO_APP) {
            exportWorkspaceSnapshotPdf(controller, outputFile);
            return;
        }
        exportWorkspaceScaledPdf(controller, outputFile, options);
    }

    private static void exportWorkspaceSnapshotPdf(MainController controller, File outputFile) throws IOException {
        if (controller == null) {
            throw new IOException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u0434\u0433\u043e\u0442\u043e\u0432\u0438\u0442\u044c \u0442\u0435\u043a\u0443\u0449\u0438\u0439 \u0432\u0438\u0434 \u043f\u0440\u043e\u0435\u043a\u0442\u0430 \u0434\u043b\u044f \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430.");
        }

        List<ChartRow> rows = autoExportRows(controller);
        if (rows.isEmpty()) {
            return;
        }

        byte[] tableBytes = snapshotTablePng(controller);
        BufferedImage tablePreview = ImageIO.read(new java.io.ByteArrayInputStream(tableBytes));
        GanttChart ganttChart = controller.getGanttChart();
        WritableImage chartHeaderImageFx = ganttChart == null ? null : snapshotNode(ganttChart.getHeaderView());
        WritableImage chartBodyImageFx = ganttChart == null ? null : snapshotNode(ganttChart.getChartView());
        if (chartHeaderImageFx == null || chartBodyImageFx == null) {
            throw new IOException("Не удалось подготовить диаграмму Ганта для экспорта.");
        }
        BufferedImage chartHeaderPreview = SwingFXUtils.fromFXImage(chartHeaderImageFx, null);
        BufferedImage chartBodyPreview = SwingFXUtils.fromFXImage(chartBodyImageFx, null);
        float tableWidth = tablePreview == null ? 520f : tablePreview.getWidth();
        float rowHeight = controller.getTreeTableView() != null && controller.getTreeTableView().getFixedCellSize() > 0
                ? (float) controller.getTreeTableView().getFixedCellSize()
                : 28f;
        int rowCount = Math.max(1, rows.size());
        int tableHeaderPx = Math.max(1, Math.round(resolveExportTableHeaderHeight(
                controller,
                tablePreview == null ? 420f : tablePreview.getHeight()
        )));
        if (tablePreview != null) {
            tableHeaderPx = Math.min(tableHeaderPx, Math.max(1, tablePreview.getHeight() - 1));
        }
        int tableBodyPx = tablePreview == null
                ? Math.max(1, Math.round(rowCount * rowHeight))
                : Math.max(1, tablePreview.getHeight() - tableHeaderPx);
        float tableHeaderHeight = tableHeaderPx;
        float tableBodyHeight = tableBodyPx;
        float tableRenderHeight = tableHeaderHeight + tableBodyHeight;

        int logicalBodyHeight = Math.max(1, Math.min(chartBodyPreview.getHeight(), Math.round(rowCount * rowHeight)));
        if (chartBodyPreview.getHeight() > logicalBodyHeight) {
            chartBodyPreview = chartBodyPreview.getSubimage(0, 0, chartBodyPreview.getWidth(), logicalBodyHeight);
        }
        float chartHeaderHeight = Math.max(1f, chartHeaderPreview.getHeight());
        float chartBodyHeight = Math.max(1f, chartBodyPreview.getHeight());
        float chartBodyRenderHeight = tableBodyHeight;
        float chartScale = chartBodyHeight <= 0 ? 1f : tableBodyPx / chartBodyHeight;
        float chartRenderWidth = Math.max(1f, Math.max(chartHeaderPreview.getWidth(), chartBodyPreview.getWidth()) * chartScale);
        float chartHeaderRenderHeight = tableHeaderPx;
        float chartRenderHeight = Math.max(1f, chartHeaderRenderHeight + chartBodyRenderHeight);
        float contentWidth = tableWidth + LIVE_SECTION_GAP + chartRenderWidth;
        float layoutScale = contentWidth <= MAX_AUTO_CONTENT_WIDTH ? 1f : MAX_AUTO_CONTENT_WIDTH / contentWidth;
        tableWidth *= layoutScale;
        tableRenderHeight *= layoutScale;
        tableBodyHeight *= layoutScale;
        chartRenderWidth *= layoutScale;
        chartRenderHeight *= layoutScale;
        chartHeaderRenderHeight *= layoutScale;
        chartBodyRenderHeight *= layoutScale;
        contentWidth = tableWidth + LIVE_SECTION_GAP + chartRenderWidth;
        float pageWidth = contentWidth + DOC_MARGIN * 2;
        float pageHeight = Math.max(tableRenderHeight, chartRenderHeight) + DOC_MARGIN * 2;

        ByteArrayOutputStream chartHeaderBytes = new ByteArrayOutputStream();
        ImageIO.write(chartHeaderPreview, "png", chartHeaderBytes);
        ByteArrayOutputStream chartBodyBytes = new ByteArrayOutputStream();
        ImageIO.write(chartBodyPreview, "png", chartBodyBytes);

        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document document = new Document(pdfDocument, new PageSize(pageWidth, pageHeight))) {
            document.setMargins(DOC_MARGIN, DOC_MARGIN, DOC_MARGIN, DOC_MARGIN);
            float tableLeft = DOC_MARGIN;
            float chartLeft = DOC_MARGIN + tableWidth + LIVE_SECTION_GAP;
            float bodyBottom = DOC_MARGIN;
            float tableBottom = bodyBottom;
            float chartBottom = bodyBottom;

            Image tableImage = new Image(ImageDataFactory.create(tableBytes));
            tableImage.scaleAbsolute(tableWidth, tableRenderHeight);
            tableImage.setFixedPosition(tableLeft, tableBottom);

            Image chartHeaderImage = new Image(ImageDataFactory.create(chartHeaderBytes.toByteArray()));
            chartHeaderImage.scaleAbsolute(chartRenderWidth, chartHeaderRenderHeight);
            chartHeaderImage.setFixedPosition(chartLeft, chartBottom + chartBodyRenderHeight);

            Image chartBodyImage = new Image(ImageDataFactory.create(chartBodyBytes.toByteArray()));
            chartBodyImage.scaleAbsolute(chartRenderWidth, chartBodyRenderHeight);
            chartBodyImage.setFixedPosition(chartLeft, chartBottom);

            document.add(tableImage);
            document.add(chartHeaderImage);
            document.add(chartBodyImage);
        }
    }

    private static void exportWorkspaceScaledPdf(MainController controller, File outputFile, ProjectReportOptions options) throws IOException {
        if (controller == null) {
            throw new IOException("Не удалось подготовить текущий вид проекта для экспорта.");
        }

        WorkspaceSnapshot snapshot = buildWorkspaceSnapshot(controller);
        PageSize pageSize = resolvePageSize(options.paperFormat(), options.landscape());
        float activeMargin = options.scaleMode() == ProjectReportOptions.ProjectReportScaleMode.FIT_PAGE ? FIT_PAGE_MARGIN : DOC_MARGIN;
        float availableWidth = pageSize.getWidth() - activeMargin * 2;
        float availableHeight = pageSize.getHeight() - activeMargin * 2;

        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document document = new Document(pdfDocument, pageSize)) {
            document.setMargins(activeMargin, activeMargin, activeMargin, activeMargin);

            switch (options.scaleMode()) {
                case FIT_PAGE -> addSingleScaledSnapshotPage(document, snapshot.pngBytes(), snapshot.width(), snapshot.height(), availableWidth, availableHeight, activeMargin);
                case FIT_WIDTH -> addFitWidthSnapshotPages(document, pdfDocument, snapshot, pageSize, availableWidth, availableHeight);
                case FIT_HEIGHT -> addFitHeightSnapshotPages(document, pdfDocument, snapshot, pageSize, availableWidth, availableHeight);
                case CURRENT_TILED -> addTiledSnapshotPages(document, snapshot, availableWidth, availableHeight);
                case AUTO_APP -> throw new IllegalStateException("AUTO should be handled by dedicated export path");
            }
        }
    }

    private static WorkspaceSnapshot buildWorkspaceSnapshot(MainController controller) throws IOException {
        List<ChartRow> rows = autoExportRows(controller);
        if (rows.isEmpty()) {
            throw new IOException("Нет данных для экспорта.");
        }

        byte[] tableBytes = snapshotTablePng(controller);
        BufferedImage tablePreview = ImageIO.read(new java.io.ByteArrayInputStream(tableBytes));
        GanttChart ganttChart = controller.getGanttChart();
        WritableImage chartHeaderImageFx = ganttChart == null ? null : snapshotNode(ganttChart.getHeaderView());
        WritableImage chartBodyImageFx = ganttChart == null ? null : snapshotNode(ganttChart.getChartView());
        if (chartHeaderImageFx == null || chartBodyImageFx == null) {
            throw new IOException("Не удалось подготовить диаграмму Ганта для экспорта.");
        }

        BufferedImage chartHeaderPreview = SwingFXUtils.fromFXImage(chartHeaderImageFx, null);
        BufferedImage chartBodyPreview = SwingFXUtils.fromFXImage(chartBodyImageFx, null);

        int tableHeaderPx = Math.max(1, Math.round(resolveExportTableHeaderHeight(
                controller,
                tablePreview == null ? 420f : tablePreview.getHeight()
        )));
        if (tablePreview != null) {
            tableHeaderPx = Math.min(tableHeaderPx, Math.max(1, tablePreview.getHeight() - 1));
        }
        int tableBodyPx = tablePreview == null
                ? Math.max(1, Math.round(Math.max(1, rows.size()) * (controller.getTreeTableView() != null && controller.getTreeTableView().getFixedCellSize() > 0
                ? (float) controller.getTreeTableView().getFixedCellSize()
                : 28f)))
                : Math.max(1, tablePreview.getHeight() - tableHeaderPx);

        int logicalBodyHeight = Math.max(1, Math.min(chartBodyPreview.getHeight(), Math.round(tableBodyPx)));
        if (chartBodyPreview.getHeight() > logicalBodyHeight) {
            chartBodyPreview = chartBodyPreview.getSubimage(0, 0, chartBodyPreview.getWidth(), logicalBodyHeight);
        }

        float chartScale = chartBodyPreview.getHeight() <= 0 ? 1f : (float) tableBodyPx / chartBodyPreview.getHeight();
        int chartRenderWidth = Math.max(1, Math.round(Math.max(chartHeaderPreview.getWidth(), chartBodyPreview.getWidth()) * chartScale));
        int chartHeaderRenderHeight = Math.max(1, tableHeaderPx);
        int chartBodyRenderHeight = Math.max(1, tableBodyPx);

        int combinedWidth = Math.max(1, tablePreview.getWidth() + Math.round(LIVE_SECTION_GAP) + chartRenderWidth);
        int combinedHeight = Math.max(1, tableHeaderPx + tableBodyPx);
        BufferedImage combined = new BufferedImage(combinedWidth, combinedHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = combined.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, combinedWidth, combinedHeight);
        graphics.drawImage(tablePreview, 0, 0, null);
        graphics.drawImage(chartHeaderPreview, tablePreview.getWidth() + Math.round(LIVE_SECTION_GAP), 0, chartRenderWidth, chartHeaderRenderHeight, null);
        graphics.drawImage(chartBodyPreview, tablePreview.getWidth() + Math.round(LIVE_SECTION_GAP), chartHeaderRenderHeight, chartRenderWidth, chartBodyRenderHeight, null);
        graphics.dispose();

        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", pngBytes);
        return new WorkspaceSnapshot(pngBytes.toByteArray(), combinedWidth, combinedHeight);
    }

    private static void addSingleScaledSnapshotPage(
            Document document,
            byte[] pngBytes,
            float contentWidth,
            float contentHeight,
            float availableWidth,
            float availableHeight,
            float activeMargin
    ) {
        float scale = Math.min(availableWidth / contentWidth, availableHeight / contentHeight);
        float targetWidth = Math.max(1f, contentWidth * scale);
        float targetHeight = Math.max(1f, contentHeight * scale);
        Image image = new Image(ImageDataFactory.create(pngBytes));
        image.scaleAbsolute(targetWidth, targetHeight);

        float left = activeMargin + Math.max(0f, (availableWidth - targetWidth) / 2f);
        float bottom = activeMargin + Math.max(0f, (availableHeight - targetHeight) / 2f);
        image.setFixedPosition(left, bottom);
        document.add(image);
    }

    private static void addFitWidthSnapshotPages(
            Document document,
            PdfDocument pdfDocument,
            WorkspaceSnapshot snapshot,
            PageSize basePageSize,
            float availableWidth,
            float availableHeight
    ) throws IOException {
        float scale = Math.min(availableWidth / snapshot.width(), 1f);
        float fillFactor = scale < FIT_WIDTH_MIN_COMFORT_SCALE ? FIT_WIDTH_COMFORT_FILL : 1f;
        float viewHeight = (availableHeight * fillFactor) / scale;
        BufferedImage full = ImageIO.read(new java.io.ByteArrayInputStream(snapshot.pngBytes()));
        int targetWidth = Math.max(1, Math.round(full.getWidth() * scale));
        int y = 0;
        int pageIndex = 0;
        while (y < full.getHeight()) {
            int sliceHeight = Math.max(1, Math.min(full.getHeight() - y, Math.round(viewHeight)));
            BufferedImage slice = full.getSubimage(0, y, full.getWidth(), sliceHeight);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(slice, "png", out);
            float scaledSliceHeight = Math.max(1, Math.round(sliceHeight * scale));
            PageSize slicePageSize = new PageSize(
                    basePageSize.getWidth(),
                    Math.min(basePageSize.getHeight(), scaledSliceHeight + DOC_MARGIN * 2)
            );
            pdfDocument.setDefaultPageSize(slicePageSize);
            if (pageIndex++ > 0) {
                pdfDocument.setDefaultPageSize(slicePageSize);
                document.add(new AreaBreak(slicePageSize));
            }
            Image image = new Image(ImageDataFactory.create(out.toByteArray()));
            image.scaleAbsolute(targetWidth, scaledSliceHeight);
            document.add(image);
            y += sliceHeight;
        }
    }

    private static void addTiledSnapshotPages(
            Document document,
            WorkspaceSnapshot snapshot,
            float availableWidth,
            float availableHeight
    ) throws IOException {
        BufferedImage full = ImageIO.read(new java.io.ByteArrayInputStream(snapshot.pngBytes()));
        float scale = Math.min(availableWidth / snapshot.width(), 1f);
        int tileWidth = Math.max(1, Math.round(availableWidth / scale));
        int tileHeight = Math.max(1, Math.round(availableHeight / scale));
        int pageIndex = 0;
        for (int y = 0; y < full.getHeight(); y += tileHeight) {
            for (int x = 0; x < full.getWidth(); x += tileWidth) {
                int width = Math.max(1, Math.min(tileWidth, full.getWidth() - x));
                int height = Math.max(1, Math.min(tileHeight, full.getHeight() - y));
                BufferedImage slice = full.getSubimage(x, y, width, height);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(slice, "png", out);
                if (pageIndex++ > 0) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }
                Image image = new Image(ImageDataFactory.create(out.toByteArray()));
                image.scaleAbsolute(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
                document.add(image);
            }
        }
    }

    private static void addFitHeightSnapshotPages(
            Document document,
            PdfDocument pdfDocument,
            WorkspaceSnapshot snapshot,
            PageSize basePageSize,
            float availableWidth,
            float availableHeight
    ) throws IOException {
        float scale = Math.min(availableHeight / snapshot.height(), 1f);
        BufferedImage full = ImageIO.read(new java.io.ByteArrayInputStream(snapshot.pngBytes()));
        int targetHeight = Math.max(1, Math.round(full.getHeight() * scale));
        int viewWidth = Math.max(1, Math.round(availableWidth / scale));
        int x = 0;
        int pageIndex = 0;
        while (x < full.getWidth()) {
            int sliceWidth = Math.max(1, Math.min(full.getWidth() - x, viewWidth));
            BufferedImage slice = full.getSubimage(x, 0, sliceWidth, full.getHeight());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(slice, "png", out);
            float scaledSliceWidth = Math.max(1, Math.round(sliceWidth * scale));
            PageSize slicePageSize = new PageSize(
                    Math.min(basePageSize.getWidth(), scaledSliceWidth + DOC_MARGIN * 2),
                    basePageSize.getHeight()
            );
            pdfDocument.setDefaultPageSize(slicePageSize);
            if (pageIndex++ > 0) {
                document.add(new AreaBreak(slicePageSize));
            }
            Image image = new Image(ImageDataFactory.create(out.toByteArray()));
            image.scaleAbsolute(scaledSliceWidth, targetHeight);
            document.add(image);
            x += sliceWidth;
        }
    }

    static byte[] snapshotTablePng(MainController controller) throws IOException {
        if (controller == null || controller.getTreeTableView() == null) {
            throw new IOException("Р В Р’В Р вЂ™Р’В Р В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’Вµ Р В Р’В Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В Р В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚СљР В Р’В Р В Р вЂ№Р В Р’В Р В РІР‚В° Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р В Р вЂ№Р В Р’В Р В РІР‚В° Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎС™ Р В Р’В Р вЂ™Р’В Р В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р вЂ№Р В Р’В Р В Р РЏ Р В Р’В Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°.");
        }
        TreeTableView<Object> sourceTable = controller.getTreeTableView();
        List<LiveColumn> columns = captureVisibleColumns(controller);
        if (columns.isEmpty()) {
            columns = defaultLiveColumns();
        }

        TreeTableView<ExportTableRow> exportTable = new TreeTableView<>();
        exportTable.getStylesheets().add(TABLE_CSS);
        exportTable.getStyleClass().add("pdf-export-table");
        exportTable.setColumnResizePolicy(TreeTableView.UNCONSTRAINED_RESIZE_POLICY);
        exportTable.setShowRoot(false);
        exportTable.setFixedCellSize(sourceTable.getFixedCellSize() > 0 ? sourceTable.getFixedCellSize() : 28.0);
        exportTable.setFocusTraversable(false);
        exportTable.setEditable(false);

        Settings settings = AppSettings.get();
        if (settings != null) {
            String textStyle = settings.getTextStyle() == null ? "standard" : settings.getTextStyle().trim().toLowerCase(Locale.ROOT);
            switch (textStyle) {
                case "compact" -> exportTable.getStyleClass().add("text-compact");
                case "presentation" -> exportTable.getStyleClass().add("text-presentation");
                default -> exportTable.getStyleClass().add("text-standard");
            }
        } else {
            exportTable.getStyleClass().add("text-standard");
        }

        List<ChartRow> rows = autoExportRows(controller);
        columns = autoFitLiveColumns(columns, rows);

        TreeItem<ExportTableRow> root = new TreeItem<>(null);
        root.setExpanded(true);
        for (ChartRow row : rows) {
            root.getChildren().add(new TreeItem<>(ExportTableRow.from(row)));
        }
        exportTable.setRoot(root);

        for (LiveColumn liveColumn : columns) {
            TreeTableColumn<ExportTableRow, String> column = new TreeTableColumn<>(displayColumnTitle(liveColumn.key));
            column.setPrefWidth(liveColumn.width);
            column.setMinWidth(liveColumn.width);
            column.setMaxWidth(liveColumn.width);
            column.setSortable(false);
            column.setReorderable(false);
            column.setResizable(false);
            column.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue() == null || data.getValue().getValue() == null
                            ? ""
                            : data.getValue().getValue().value(liveColumn.key)
            ));
            column.setStyle(switch (liveColumn.alignment) {
                case LEFT -> "-fx-alignment: CENTER-LEFT;";
                case RIGHT -> "-fx-alignment: CENTER-RIGHT;";
                default -> "-fx-alignment: CENTER;";
            });
            exportTable.getColumns().add(column);
        }

        double tableWidth = columns.stream().mapToDouble(column -> column.width).sum() + 2.0;
        double headerHeight = 66.0;
        double tableHeight = headerHeight + root.getChildren().size() * exportTable.getFixedCellSize();
        exportTable.setPrefWidth(tableWidth);
        exportTable.setMinWidth(tableWidth);
        exportTable.setMaxWidth(tableWidth);
        exportTable.setPrefHeight(tableHeight);
        exportTable.setMinHeight(tableHeight);
        exportTable.setMaxHeight(tableHeight);

        StackPane container = new StackPane(exportTable);
        container.setStyle("-fx-background-color: white;");
        container.setPadding(new Insets(0));
        container.setPrefSize(tableWidth, tableHeight);
        container.setMinSize(tableWidth, tableHeight);
        container.setMaxSize(tableWidth, tableHeight);
        Scene exportScene = new Scene(container, tableWidth, tableHeight, Color.WHITE);
        exportScene.getStylesheets().add(TABLE_CSS);
        container.applyCss();
        container.layout();
        exportTable.applyCss();
        exportTable.layout();
        exportTable.lookupAll(".scroll-bar, .corner").forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
            node.setOpacity(0);
        });

        WritableImage tableImage = snapshotNode(container);
        if (tableImage == null) {
            throw new IOException("Р В Р’В Р вЂ™Р’В Р В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’Вµ Р В Р’В Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В Р В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚СљР В Р’В Р В Р вЂ№Р В Р’В Р В РІР‚В° Р В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚СљР В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В Р В Р Р‹Р вЂ™Р’ВР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р В Р вЂ№Р В Р’В Р В РІР‚В° Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р вЂ™Р’В Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’Вµ Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњ.");
        }
        BufferedImage tableBuffered = SwingFXUtils.fromFXImage(tableImage, null);
        javafx.scene.Node exportHeader = exportTable.lookup(".column-header-background");
        double actualHeaderHeight = exportHeader == null
                ? EXPORT_TABLE_HEADER_HEIGHT
                : Math.max(exportHeader.getBoundsInParent().getHeight(), exportHeader.getLayoutBounds().getHeight());
        int croppedHeight = (int) Math.round(Math.max(
                1d,
                Math.min(
                        tableBuffered.getHeight(),
                        actualHeaderHeight + root.getChildren().size() * exportTable.getFixedCellSize()
                )
        ));
        if (croppedHeight < tableBuffered.getHeight()) {
            tableBuffered = tableBuffered.getSubimage(0, 0, tableBuffered.getWidth(), croppedHeight);
        }
        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(tableBuffered, "png", pngBytes);
        return pngBytes.toByteArray();
    }

    private static float resolveExportTableHeaderHeight(MainController controller, float fallbackTableHeight) {
        TreeTableView<Object> tableView = controller == null ? null : controller.getTreeTableView();
        if (tableView != null) {
            javafx.scene.Node header = tableView.lookup(".column-header-background");
            if (header != null) {
                double height = Math.max(
                        header.getBoundsInParent().getHeight(),
                        header.getLayoutBounds().getHeight()
                );
                if (height > 1) {
                    return (float) Math.min(height, Math.max(1f, fallbackTableHeight - 1f));
                }
            }
        }
        return Math.min(EXPORT_TABLE_HEADER_HEIGHT, Math.max(1f, fallbackTableHeight - 1f));
    }

    private static Table buildLiveTaskTable(List<ChartRow> rows, List<LiveColumn> columns) {
        float[] widths = new float[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).width;
        }

        Table table = new Table(widths);
        table.setFixedLayout();
        table.setWidth(widthsSum(widths));

        Style headerStyle = new Style()
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor("#dbe7f3"))
                .setBold()
                .setFontSize(8.8f)
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor("#c7d3e3"), 0.55f))
                .setPaddingTop(6f)
                .setPaddingBottom(6f)
                .setPaddingLeft(5f)
                .setPaddingRight(5f);
        Style cellStyle = new Style()
                .setFontSize(8.3f)
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor("#dbe3ec"), 0.4f))
                .setPaddingTop(4f)
                .setPaddingBottom(4f)
                .setPaddingLeft(6f)
                .setPaddingRight(6f);

        for (LiveColumn column : columns) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(column.title).setMargin(0).setMultipliedLeading(1.0f))
                    .addStyle(headerStyle)
                    .setMinHeight(30f)
                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            ChartRow row = rows.get(rowIndex);
            boolean taskRow = !row.subTask;
            com.itextpdf.kernel.colors.Color background;
            if (taskRow) {
                background = com.itextpdf.kernel.colors.WebColors.getRGBColor("#ffffff");
            } else {
                background = rowIndex % 2 == 0
                        ? com.itextpdf.kernel.colors.WebColors.getRGBColor("#ffffff")
                        : com.itextpdf.kernel.colors.WebColors.getRGBColor("#f8fbff");
            }
            for (LiveColumn column : columns) {
                boolean emphasizeName = taskRow && "name".equals(column.key);
                float leftPadding = "name".equals(column.key)
                        ? (row.subTask ? 16f : 8f)
                        : 6f;
                table.addCell(cell(
                        resolveLiveColumnValue(row, column.key),
                        cellStyle,
                        background,
                        column.alignment,
                        emphasizeName,
                        leftPadding
                ));
            }
        }

        return table;
    }

    private static String resolveLiveColumnValue(ChartRow row, String key) {
        return switch (key) {
            case "position" -> row.position;
            case "name" -> row.label;
            case "company" -> row.company;
            case "start" -> formatDate(row.start);
            case "end" -> formatDate(row.end);
            case "unit" -> row.unit;
            case "volProject" -> row.projectVolume;
            case "volActual" -> row.actualVolume;
            case "percent" -> row.percentText;
            case "status" -> row.status;
            case "priority" -> row.priority;
            default -> "";
        };
    }

    private static List<LiveColumn> captureVisibleColumns(MainController controller) {
        TreeTableView<Object> tableView = controller.getTreeTableView();
        if (tableView == null) {
            return List.of();
        }

        List<LiveColumn> result = new ArrayList<>();
        for (TreeTableColumn<Object, ?> column : tableView.getVisibleLeafColumns()) {
            if (column == null || !column.isVisible()) {
                continue;
            }
            String key = controller.resolveColumnKey(column);
            if (key == null) {
                key = mapColumnTitleToKey(safe(column.getText()));
            }
            if (key == null) {
                continue;
            }
            float width = (float) Math.max(column.getWidth() > 0 ? column.getWidth() : column.getPrefWidth(), 46.0);
            result.add(new LiveColumn(displayColumnTitle(key), key, width, resolveAlignment(key)));
        }
        return result;
    }

    private static List<LiveColumn> autoFitLiveColumns(List<LiveColumn> columns, List<ChartRow> rows) {
        if (columns == null || columns.isEmpty()) {
            return List.of();
        }

        List<LiveColumn> adjusted = new ArrayList<>(columns.size());
        for (LiveColumn column : columns) {
            float minWidth = minColumnWidth(column.key, column.width);
            float maxWidth = maxColumnWidth(column.key, column.width);
            float width = Math.max(minWidth, estimateTextWidth(column.title, column.key, true));

            for (ChartRow row : rows) {
                String value = resolveLiveColumnValue(row, column.key);
                width = Math.max(width, estimateTextWidth(value, column.key, false));
            }

            adjusted.add(new LiveColumn(column.title, column.key, Math.min(width, maxWidth), column.alignment));
        }
        return adjusted;
    }

    private static float minColumnWidth(String key, float fallback) {
        return switch (key) {
            case "position" -> Math.max(fallback, 78f);
            case "name" -> Math.max(fallback, 360f);
            case "company" -> Math.max(fallback, 190f);
            case "start", "end" -> Math.max(fallback, 96f);
            case "unit" -> Math.max(fallback, 86f);
            case "volProject", "volActual" -> Math.max(fallback, 122f);
            case "percent" -> Math.max(fallback, 96f);
            case "status" -> Math.max(fallback, 100f);
            case "priority" -> Math.max(fallback, 90f);
            default -> Math.max(fallback, 72f);
        };
    }

    private static float maxColumnWidth(String key, float fallback) {
        return switch (key) {
            case "position" -> Math.max(fallback, 92f);
            case "name" -> Math.max(fallback, 520f);
            case "company" -> Math.max(fallback, 280f);
            case "start", "end" -> Math.max(fallback, 118f);
            case "unit" -> Math.max(fallback, 104f);
            case "volProject", "volActual" -> Math.max(fallback, 150f);
            case "percent" -> Math.max(fallback, 112f);
            case "status" -> Math.max(fallback, 150f);
            case "priority" -> Math.max(fallback, 132f);
            default -> Math.max(fallback, 180f);
        };
    }

    private static float estimateTextWidth(String text, String key, boolean header) {
        String value = safe(text);
        int length = value.length();
        if (length == 0) {
            return 24f;
        }

        float perChar = switch (key) {
            case "position", "percent" -> 6.8f;
            case "start", "end", "unit" -> 7.0f;
            case "volProject", "volActual" -> 7.2f;
            case "status", "priority" -> 7.4f;
            case "company" -> 7.5f;
            case "name" -> 7.8f;
            default -> 7.2f;
        };

        float basePadding = header ? 22f : 18f;
        return basePadding + length * perChar;
    }

    private static List<LiveColumn> defaultLiveColumns() {
        return List.of(
                new LiveColumn("\u2116 \u043f/\u043f", "position", 78f, TextAlignment.CENTER),
                new LiveColumn("\u041d\u0430\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442", "name", 360f, TextAlignment.LEFT),
                new LiveColumn("\u0418\u0441\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c", "company", 190f, TextAlignment.LEFT),
                new LiveColumn("\u041d\u0430\u0447\u0430\u043b\u043e", "start", 96f, TextAlignment.CENTER),
                new LiveColumn("\u041e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435", "end", 96f, TextAlignment.CENTER),
                new LiveColumn("\u0415\u0434. \u0438\u0437\u043c.", "unit", 86f, TextAlignment.CENTER),
                new LiveColumn("\u041f\u0440\u043e\u0435\u043a\u0442\u043d\u044b\u0439 \u043e\u0431\u044a\u0451\u043c", "volProject", 122f, TextAlignment.RIGHT),
                new LiveColumn("\u0424\u0430\u043a\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043e\u0431\u044a\u0451\u043c", "volActual", 122f, TextAlignment.RIGHT),
                new LiveColumn("\u041f\u0440\u043e\u0446\u0435\u043d\u0442", "percent", 96f, TextAlignment.CENTER),
                new LiveColumn("\u0421\u0442\u0430\u0442\u0443\u0441", "status", 100f, TextAlignment.CENTER),
                new LiveColumn("\u041f\u0440\u0438\u043e\u0440\u0438\u0442\u0435\u0442", "priority", 90f, TextAlignment.CENTER)
        );
    }

    private static String mapColumnTitleToKey(String title) {
        return switch (title) {
            case "\u2116 \u043f/\u043f" -> "position";
            case "\u041d\u0430\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442" -> "name";
            case "\u0418\u0441\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c" -> "company";
            case "\u041d\u0430\u0447\u0430\u043b\u043e" -> "start";
            case "\u041e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435" -> "end";
            case "\u0415\u0434. \u0438\u0437\u043c." -> "unit";
            case "\u041f\u0440\u043e\u0435\u043a\u0442\u043d\u044b\u0439 \u043e\u0431\u044a\u0451\u043c" -> "volProject";
            case "\u0424\u0430\u043a\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043e\u0431\u044a\u0451\u043c" -> "volActual";
            case "\u041f\u0440\u043e\u0446\u0435\u043d\u0442" -> "percent";
            case "\u0421\u0442\u0430\u0442\u0443\u0441" -> "status";
            case "\u041f\u0440\u0438\u043e\u0440\u0438\u0442\u0435\u0442" -> "priority";
            default -> null;
        };
    }

    private static String displayColumnTitle(String key) {
        return switch (key) {
            case "position" -> "\u2116 \u043f/\u043f";
            case "name" -> "\u041d\u0430\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442";
            case "company" -> "\u0418\u0441\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c";
            case "start" -> "\u041d\u0430\u0447\u0430\u043b\u043e";
            case "end" -> "\u041e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435";
            case "unit" -> "\u0415\u0434. \u0438\u0437\u043c.";
            case "volProject" -> "\u041f\u0440\u043e\u0435\u043a\u0442\u043d\u044b\u0439 \u043e\u0431\u044a\u0451\u043c";
            case "volActual" -> "\u0424\u0430\u043a\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u043e\u0431\u044a\u0451\u043c";
            case "percent" -> "\u041f\u0440\u043e\u0446\u0435\u043d\u0442";
            case "status" -> "\u0421\u0442\u0430\u0442\u0443\u0441";
            case "priority" -> "\u041f\u0440\u0438\u043e\u0440\u0438\u0442\u0435\u0442";
            default -> safe(key);
        };
    }

    private static TextAlignment resolveAlignment(String key) {
        return switch (key) {
            case "name", "company" -> TextAlignment.LEFT;
            case "volProject", "volActual" -> TextAlignment.CENTER;
            default -> TextAlignment.CENTER;
        };
    }

    private static float widthsSum(float[] widths) {
        float sum = 0f;
        for (float width : widths) {
            sum += width;
        }
        return sum;
    }

    static byte[] snapshotCombinedChartPng(MainController controller) throws IOException {
        if (controller == null || controller.getGanttChart() == null) {
            throw new IOException("Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р С—Р С•Р Т‘Р С–Р С•РЎвЂљР С•Р Р†Р С‘РЎвЂљРЎРЉ Р Т‘Р С‘Р В°Р С–РЎР‚Р В°Р СР СРЎС“ Р вЂњР В°Р Р…РЎвЂљР В° Р Т‘Р В»РЎРЏ РЎРЊР С”РЎРѓР С—Р С•РЎР‚РЎвЂљР В°.");
        }

        GanttChart liveChart = controller.getGanttChart();
        WritableImage headerImage = snapshotNode(liveChart.getHeaderView());
        WritableImage chartImage = snapshotNode(liveChart.getChartView());
        if (headerImage == null || chartImage == null) {
            throw new IOException("Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ РЎРѓРЎвЂћР С•РЎР‚Р СР С‘РЎР‚Р С•Р Р†Р В°РЎвЂљРЎРЉ Р С‘Р В·Р С•Р В±РЎР‚Р В°Р В¶Р ВµР Р…Р С‘Р Вµ Р Т‘Р С‘Р В°Р С–РЎР‚Р В°Р СР СРЎвЂ№ Р вЂњР В°Р Р…РЎвЂљР В°.");
        }

        BufferedImage headerBuffered = SwingFXUtils.fromFXImage(headerImage, null);
        BufferedImage chartBuffered = SwingFXUtils.fromFXImage(chartImage, null);

        int visibleRows = autoExportRows(controller).size();
        double exportRowHeight = controller.getTreeTableView() != null && controller.getTreeTableView().getFixedCellSize() > 0
                ? controller.getTreeTableView().getFixedCellSize()
                : 28.0;
        int logicalBodyHeight = (int) Math.max(1, Math.round(Math.max(1, visibleRows) * exportRowHeight));
        if (chartBuffered.getHeight() > logicalBodyHeight) {
            chartBuffered = chartBuffered.getSubimage(0, 0, chartBuffered.getWidth(), logicalBodyHeight);
        }

        int width = Math.max(headerBuffered.getWidth(), chartBuffered.getWidth());
        int height = headerBuffered.getHeight() + chartBuffered.getHeight();
        BufferedImage combined = new BufferedImage(Math.max(width, 1), Math.max(height, 1), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = combined.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, combined.getWidth(), combined.getHeight());
        graphics.drawImage(headerBuffered, 0, 0, null);
        graphics.drawImage(chartBuffered, 0, headerBuffered.getHeight(), null);
        graphics.dispose();

        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", pngBytes);
        return pngBytes.toByteArray();
    }

    private static WritableImage snapshotNode(javafx.scene.Node node) {
        if (node == null) {
            return null;
        }

        boolean attachedToScene = node.getScene() != null;
        if (!attachedToScene) {
            node.applyCss();
            if (node instanceof javafx.scene.Parent parent) {
                parent.layout();
            }
        }

        double width = resolveSnapshotWidth(node);
        double height = resolveSnapshotHeight(node);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.WHITE);
        WritableImage image = new WritableImage((int) width, (int) height);
        node.snapshot(parameters, image);
        return image;
    }

    private static double resolveSnapshotWidth(javafx.scene.Node node) {
        double width = 0;
        if (node instanceof Region region && region.getWidth() > 0) {
            width = region.getWidth();
        }
        width = Math.max(width, node.getBoundsInLocal().getWidth());
        width = Math.max(width, node.getLayoutBounds().getWidth());
        return Math.max(Math.ceil(width), 1);
    }

    private static double resolveSnapshotHeight(javafx.scene.Node node) {
        double height = 0;
        if (node instanceof Region region && region.getHeight() > 0) {
            height = region.getHeight();
        }
        height = Math.max(height, node.getBoundsInLocal().getHeight());
        height = Math.max(height, node.getLayoutBounds().getHeight());
        return Math.max(Math.ceil(height), 1);
    }

    private static void addReportHeader(Document document, MainController controller) {
        String projectName = controller == null ? "\u041f\u0440\u043e\u0435\u043a\u0442" : controller.getProjectName();
        String author = controller == null ? "-" : safe(controller.getProjectAuthor()).isBlank()
                ? safe(System.getProperty("user.name"))
                : controller.getProjectAuthor();
        List<ChartRow> rows = flattenRows(controller == null || controller.getTasks() == null ? List.of() : controller.getTasks());
        long taskCount = rows.stream().filter(row -> !row.subTask).count();
        long subTaskCount = rows.stream().filter(row -> row.subTask).count();
        long completedCount = rows.stream()
                .filter(row -> safe(row.status).equalsIgnoreCase("completed") || safe(row.status).equalsIgnoreCase("\u0417\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u043e"))
                .count();
        double avgProgress = rows.isEmpty() ? 0 : rows.stream().mapToDouble(ProjectPdfReportBuilder::extractPercentValue).average().orElse(0);
        LocalDate projectStart = rows.stream()
                .map(row -> row.start)
                .filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        document.add(new LineSeparator(new SolidLine(2f))
                .setStrokeColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(ACCENT))
                .setMarginBottom(8f));

        Table header = new Table(new float[]{118f, 1f, 118f, 1f});
        header.setWidth(UnitValue.createPercentValue(100));
        header.setBorder(Border.NO_BORDER);
        header.setMarginBottom(8f);
        header.addCell(createInfoLabelCell("\u041f\u0440\u043e\u0435\u043a\u0442"));
        header.addCell(createInfoValueCell(safe(projectName)));
        header.addCell(createInfoLabelCell("\u0410\u0432\u0442\u043e\u0440"));
        header.addCell(createInfoValueCell(safe(author)));
        header.addCell(createInfoLabelCell("\u0414\u0430\u0442\u0430 \u0441\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u044f"));
        header.addCell(createInfoValueCell(LocalDate.now().format(DATE_FORMAT)));
        header.addCell(createInfoLabelCell("\u0414\u0430\u0442\u0430 \u043d\u0430\u0447\u0430\u043b\u0430"));
        header.addCell(createInfoValueCell(projectStart == null ? "-" : projectStart.format(DATE_FORMAT)));
        document.add(header);

        Table metrics = new Table(new float[]{1f, 1f, 1f, 1f});
        metrics.setWidth(UnitValue.createPercentValue(100));
        metrics.setBorder(Border.NO_BORDER);
        metrics.setMarginBottom(8f);
        metrics.addCell(createMetricCard("\u0417\u0430\u0434\u0430\u0447", String.valueOf(taskCount)));
        metrics.addCell(createMetricCard("\u041f\u043e\u0434\u0437\u0430\u0434\u0430\u0447", String.valueOf(subTaskCount)));
        metrics.addCell(createMetricCard("\u0417\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u043e", String.valueOf(completedCount)));
        metrics.addCell(createMetricCard("\u0421\u0440\u0435\u0434\u043d\u0438\u0439 \u043f\u0440\u043e\u0433\u0440\u0435\u0441\u0441", Math.round(avgProgress) + "%"));
        document.add(metrics);
        document.add(new LineSeparator(new SolidLine(1f))
                .setStrokeColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(BORDER))
                .setMarginBottom(8f));
    }

    private static double extractPercentValue(ChartRow row) {
        String raw = safe(row.percentText).replace("%", "").replace(",", ".").trim();
        if (raw.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static Cell createHeaderMetaCell(String label, String value) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(3f)
                .setPaddingBottom(3f)
                .setPaddingLeft(0)
                .setPaddingRight(0);
        cell.add(applyPdfFont(new Paragraph(label).setMargin(0), false)
                .setFontSize(8.1f)
                .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(MUTED)));
        cell.add(applyPdfFont(new Paragraph(value).setMarginTop(1f).setMarginBottom(0), true)
                .setFontSize(10.1f)
                .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(INK)));
        return cell;
    }

    private static Cell createInfoLabelCell(String label) {
        return new Cell()
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor(BORDER), 0.7f))
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(SURFACE_ALT))
                .setPaddingTop(6f)
                .setPaddingBottom(6f)
                .setPaddingLeft(8f)
                .setPaddingRight(8f)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .add(applyPdfFont(new Paragraph(label).setMargin(0), true)
                        .setFontSize(8.6f)
                        .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(MUTED)));
    }

    private static Cell createInfoValueCell(String value) {
        return new Cell()
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor(BORDER), 0.7f))
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor("#ffffff"))
                .setPaddingTop(6f)
                .setPaddingBottom(6f)
                .setPaddingLeft(8f)
                .setPaddingRight(8f)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .add(applyPdfFont(new Paragraph(value).setMargin(0), true)
                        .setFontSize(9.4f)
                        .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(INK)));
    }

    private static Cell createMetricCard(String label, String value) {
        Cell card = new Cell()
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor(BORDER), 0.7f))
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor("#ffffff"))
                .setPaddingTop(6f)
                .setPaddingBottom(6f)
                .setPaddingLeft(10f)
                .setPaddingRight(10f)
                .setMinHeight(42f);
        card.add(applyPdfFont(new Paragraph(label).setMargin(0), false)
                .setFontSize(8.0f)
                .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(MUTED)));
        card.add(applyPdfFont(new Paragraph(value).setMarginTop(4f).setMarginBottom(0), true)
                .setFontSize(12.8f)
                .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(INK)));
        return card;
    }

    private static void addWorkspaceCard(Document document, float left, float bottom, float width, float height, String title, String subtitle) {
        Table frame = new Table(1);
        frame.setWidth(width);
        frame.setFixedPosition(left, bottom, width);
        frame.setBorder(Border.NO_BORDER);
        Cell card = new Cell()
                .setMinHeight(height)
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(SURFACE))
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor(BORDER), 0.8f))
                .setPadding(0);
        Table chrome = new Table(1);
        chrome.setWidth(UnitValue.createPercentValue(100));
        chrome.setBorder(Border.NO_BORDER);
        Cell titleCell = new Cell()
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor("#ffffff"))
                .setBorderBottom(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor(BORDER), 0.8f))
                .setPaddingTop(7f)
                .setPaddingBottom(6f)
                .setPaddingLeft(12f)
                .setPaddingRight(12f);
        titleCell.add(applyPdfFont(new Paragraph(title).setMargin(0), true)
                .setFontSize(10.2f)
                .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(INK)));
        titleCell.add(applyPdfFont(new Paragraph(subtitle).setMarginTop(6f).setMarginBottom(0), false)
                .setFontSize(7.9f)
                .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(MUTED)));
        chrome.addCell(titleCell);
        card.add(chrome);
        frame.addCell(card);
        document.add(frame);
    }

    private static void addPdfLegend(Document document, float left, float bottom, float width, float height) {
        Table legendTable = new Table(new float[]{308f, width - 308f});
        legendTable.setFixedLayout();
        legendTable.setWidth(width);
        legendTable.setFixedPosition(left, bottom, width);
        legendTable.setBorder(Border.NO_BORDER);

        Style objectStyle = new Style()
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor("#edf3fb"))
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor("#d3ddea"), 0.75f))
                .setPaddingTop(6f)
                .setPaddingBottom(6f)
                .setPaddingLeft(8f)
                .setPaddingRight(8f)
                .setFontSize(9.1f)
                .setBold();

        Style descriptionStyle = new Style()
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor("#ffffff"))
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor("#dbe5f0"), 0.75f))
                .setPaddingTop(6f)
                .setPaddingBottom(6f)
                .setPaddingLeft(10f)
                .setPaddingRight(10f)
                .setFontSize(8.7f)
                .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(INK));

        String[][] rows = {
                {"FS / SS / FF / SF", "\u0422\u0438\u043f\u044b \u0437\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u0435\u0439 \u043c\u0435\u0436\u0434\u0443 \u0437\u0430\u0434\u0430\u0447\u0430\u043c\u0438. \u041b\u0438\u043d\u0438\u0438 \u043f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u044e\u0442, \u043a\u0430\u043a \u0441\u043c\u0435\u0449\u0435\u043d\u0438\u0435 \u043e\u0434\u043d\u043e\u0439 \u0437\u0430\u0434\u0430\u0447\u0438 \u0432\u043b\u0438\u044f\u0435\u0442 \u043d\u0430 \u0434\u0440\u0443\u0433\u0443\u044e."},
                {"\u0410\u0412 / \u041a\u0421 / !", "\u0424\u0438\u043d\u0430\u043d\u0441\u043e\u0432\u044b\u0435 \u043c\u0430\u0440\u043a\u0435\u0440\u044b \u0438 \u0431\u043b\u043e\u043a\u0438\u0440\u0443\u044e\u0449\u0438\u0435 \u0434\u0430\u0442\u044b: \u0430\u0432\u0430\u043d\u0441, \u043e\u043f\u043b\u0430\u0442\u0430 \u041a\u0421 \u0438 \u043f\u0440\u0435\u0434\u0443\u043f\u0440\u0435\u0436\u0434\u0430\u044e\u0449\u0438\u0439 \u043c\u0430\u0440\u043a\u0435\u0440 \u0440\u0438\u0441\u043a\u0430."},
                {"\u041a\u0440\u0430\u0441\u043d\u044b\u0439 \u0431\u0430\u0440", "\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u0438\u043b\u0438 \u0432\u044b\u0441\u043e\u043a\u043e\u0440\u0438\u0441\u043a\u043e\u0432\u0430\u044f \u0437\u0430\u0434\u0430\u0447\u0430, \u0442\u0440\u0435\u0431\u0443\u044e\u0449\u0430\u044f \u043e\u0441\u043e\u0431\u043e\u0433\u043e \u043a\u043e\u043d\u0442\u0440\u043e\u043b\u044f."},
                {"\u0417\u0435\u043b\u0451\u043d\u044b\u0439 \u0431\u0430\u0440", "\u041e\u0431\u044b\u0447\u043d\u0430\u044f \u0437\u0430\u0434\u0430\u0447\u0430 \u0438\u043b\u0438 \u043f\u043e\u0434\u0437\u0430\u0434\u0430\u0447\u0430 \u0432 \u0440\u0430\u0431\u043e\u0447\u0435\u043c \u043f\u043b\u0430\u043d\u0435."},
                {"\u0421\u0438\u043d\u0438\u0439 \u0431\u0430\u0440", "\u0417\u0430\u0434\u0430\u0447\u0430 \u0441 \u0444\u0438\u043d\u0430\u043d\u0441\u043e\u0432\u044b\u043c \u043c\u0430\u0440\u043a\u0435\u0440\u043e\u043c \u0438\u043b\u0438 \u0432\u0430\u0436\u043d\u044b\u043c \u0430\u043a\u0446\u0435\u043d\u0442\u043e\u043c \u043d\u0430 \u0434\u0438\u0430\u0433\u0440\u0430\u043c\u043c\u0435."}
        };

        float rowHeight = Math.max(21f, height / rows.length);
        for (String[] row : rows) {
            legendTable.addCell(new Cell()
                    .setMinHeight(rowHeight)
                    .add(buildLegendObjectBlock(row[0]))
                    .addStyle(objectStyle)
                    .setPaddingLeft(6f)
                    .setPaddingRight(6f)
                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE));
            legendTable.addCell(new Cell()
                    .setMinHeight(rowHeight)
                    .add(applyPdfFont(new Paragraph(row[1]).setMargin(0).setMultipliedLeading(1.02f), false))
                    .addStyle(descriptionStyle)
                    .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE));
        }

        document.add(legendTable);
    }

    private static Table buildLegendObjectBlock(String key) {
        Table block = new Table(new float[]{112f, 1f});
        block.setWidth(UnitValue.createPercentValue(100));
        block.setBorder(Border.NO_BORDER);

        Cell sampleCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        Cell labelCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(0)
                .setPaddingBottom(0)
                .setPaddingLeft(10f)
                .setPaddingRight(0)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .add(applyPdfFont(new Paragraph(key).setMargin(0), true).setFontSize(9.1f)
                        .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(INK)));

        sampleCell.add(createLegendSample(key));
        block.addCell(sampleCell);
        block.addCell(labelCell);
        return block;
    }

    private static Table createLegendSample(String key) {
        return switch (key) {
            case "FS / SS / FF / SF" -> createDependencyLegendSample();
            case "\u0410\u0412 / \u041a\u0421 / !" -> createFinanceLegendSample();
            case "\u041a\u0440\u0430\u0441\u043d\u044b\u0439 \u0431\u0430\u0440" -> createBarLegendSample("#dc2626");
            case "\u0417\u0435\u043b\u0451\u043d\u044b\u0439 \u0431\u0430\u0440" -> createBarLegendSample("#16a34a");
            default -> createBarLegendSample("#2563eb");
        };
    }

    private static Table createDependencyLegendSample() {
        Table sample = new Table(new float[]{1f, 1f, 1f, 1f});
        sample.setBorder(Border.NO_BORDER);
        sample.setWidth(UnitValue.createPercentValue(100));
        sample.addCell(createLegendBadgeCell("FS", "#7c848f", "#ffffff"));
        sample.addCell(createLegendBadgeCell("SS", "#2563eb", "#ffffff"));
        sample.addCell(createLegendBadgeCell("FF", "#0f766e", "#ffffff"));
        sample.addCell(createLegendBadgeCell("SF", "#b45309", "#ffffff"));
        return sample;
    }

    private static Table createFinanceLegendSample() {
        Table sample = new Table(new float[]{1f, 1f, 1f});
        sample.setBorder(Border.NO_BORDER);
        sample.setWidth(UnitValue.createPercentValue(100));
        sample.addCell(createLegendBadgeCell("\u0410\u0412", "#2563eb", "#ffffff"));
        sample.addCell(createLegendBadgeCell("\u041a\u0421", "#f59e0b", "#ffffff"));
        sample.addCell(createLegendBadgeCell("!", "#dc2626", "#ffffff"));
        return sample;
    }

    private static Table createBarLegendSample(String color) {
        Table sample = new Table(new float[]{1f});
        sample.setBorder(Border.NO_BORDER);
        sample.setWidth(UnitValue.createPercentValue(100));
        sample.addCell(new Cell()
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor(darker(color)), 0.9f))
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(color))
                .setMinHeight(12f)
                .setPadding(0)
                .setMargin(0));
        return sample;
    }

    private static Cell createLegendBadgeCell(String text, String background, String foreground) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(1f)
                .setPaddingBottom(1f)
                .setPaddingLeft(2f)
                .setPaddingRight(2f)
                .add(applyPdfFont(new Paragraph(text).setMargin(0), true)
                        .setFontSize(7.4f)
                        .setFontColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(foreground))
                        .setTextAlignment(TextAlignment.CENTER))
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(com.itextpdf.kernel.colors.WebColors.getRGBColor(background))
                .setBorder(new SolidBorder(com.itextpdf.kernel.colors.WebColors.getRGBColor(darker(background)), 0.7f))
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
    }

    private static void addTaskTable(Document document, List<ChartRow> rows) {
        Table table = new Table(TABLE_WIDTHS.clone());
        table.setWidth(UnitValue.createPercentValue(100));

        Style headerStyle = new Style()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setFontSize(9)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.6f))
                .setPadding(4);
        Style cellStyle = new Style()
                .setFontSize(8.7f)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.45f))
                .setPadding(3);

        String[] headers = {"РІвЂћвЂ“", "Р СњР В°Р С‘Р СР ВµР Р…Р С•Р Р†Р В°Р Р…Р С‘Р Вµ РЎР‚Р В°Р В±Р С•РЎвЂљ", "Р ВРЎРѓР С—Р С•Р В»Р Р…Р С‘РЎвЂљР ВµР В»РЎРЉ", "Р СњР В°РЎвЂЎР В°Р В»Р С•", "Р С›Р С”Р С•Р Р…РЎвЂЎР В°Р Р…Р С‘Р Вµ", "Р вЂўР Т‘. Р С‘Р В·Р С.", "Р С›Р В±РЎР‰РЎвЂР С", "%", "Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ"};
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(applyPdfFont(new Paragraph(header), true)).addStyle(headerStyle).setTextAlignment(TextAlignment.CENTER));
        }

        for (ChartRow row : rows) {
            boolean taskRow = !row.subTask;
            com.itextpdf.kernel.colors.Color background = taskRow ? com.itextpdf.kernel.colors.WebColors.getRGBColor("#f8fafc") : ColorConstants.WHITE;
            table.addCell(cell(row.position, cellStyle, background, TextAlignment.CENTER, false));
            table.addCell(cell(row.label, cellStyle, background, TextAlignment.LEFT, taskRow));
            table.addCell(cell(row.company, cellStyle, background, TextAlignment.LEFT, false));
            table.addCell(cell(formatDate(row.start), cellStyle, background, TextAlignment.CENTER, false));
            table.addCell(cell(formatDate(row.end), cellStyle, background, TextAlignment.CENTER, false));
            table.addCell(cell(row.unit, cellStyle, background, TextAlignment.CENTER, false));
            table.addCell(cell(row.projectVolume, cellStyle, background, TextAlignment.RIGHT, false));
            table.addCell(cell(row.percentText, cellStyle, background, TextAlignment.CENTER, false));
            table.addCell(cell(row.status, cellStyle, background, TextAlignment.CENTER, false));
        }

        document.add(applyPdfFont(new Paragraph("\u0422\u0430\u0431\u043b\u0438\u0446\u0430 \u0437\u0430\u0434\u0430\u0447"), true).setBold().setFontSize(13).setMarginBottom(6));
        document.add(table);
    }

    private static Cell cell(
            String text,
            Style style,
            com.itextpdf.kernel.colors.Color background,
            TextAlignment align,
            boolean bold,
            float leftPadding
    ) {
        Paragraph p = applyPdfFont(new Paragraph(safe(text)).setMargin(0), bold);
        if (bold) {
            p.setBold();
        }
        return new Cell()
                .add(p)
                .addStyle(style)
                .setBackgroundColor(background)
                .setTextAlignment(align)
                .setPaddingLeft(leftPadding)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setMinHeight(24f);
    }

    private static Cell cell(
            String text,
            Style style,
            com.itextpdf.kernel.colors.Color background,
            TextAlignment align,
            boolean bold
    ) {
        return cell(text, style, background, align, bold, 6f);
    }

    private static Paragraph applyPdfFont(Paragraph paragraph, boolean bold) {
        try {
            paragraph.setFont(resolvePdfFont(bold));
        } catch (IOException ignored) {
            // Fallback to iText default font if system font is unavailable.
        }
        return paragraph;
    }

    private static PdfFont resolvePdfFont(boolean bold) throws IOException {
        return PdfFontFactory.createFont(
                bold ? PDF_FONT_BOLD : PDF_FONT_REGULAR,
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED,
                false
        );
    }

    private static void addChartPages(Document document, PdfDocument pdfDocument, ChartSpec spec, ProjectReportOptions.ProjectReportScaleMode scaleMode) {
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        Rectangle pageBox = pdfDocument.getDefaultPageSize();
        float availableWidth = pageBox.getWidth() - DOC_MARGIN * 2;
        float availableHeight = pageBox.getHeight() - DOC_MARGIN * 2 - CHART_PAGE_TITLE_SPACE;
        List<ChartSlice> slices = buildSlices(spec, scaleMode, availableWidth, availableHeight);

        for (int i = 0; i < slices.size(); i++) {
            if (i > 0) {
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }
            document.add(applyPdfFont(new Paragraph(slices.size() == 1 ? "\u0414\u0438\u0430\u0433\u0440\u0430\u043c\u043c\u0430 \u0413\u0430\u043d\u0442\u0430" : "\u0414\u0438\u0430\u0433\u0440\u0430\u043c\u043c\u0430 \u0413\u0430\u043d\u0442\u0430 \u2014 \u043b\u0438\u0441\u0442 " + (i + 1) + " \u0438\u0437 " + slices.size()), true)
                    .setBold().setFontSize(13).setMarginBottom(8));
            PdfFormXObject xObject = SvgConverter.convertToXObject(slices.get(i).svg(), pdfDocument);
            Image image = new Image(xObject);
            image.scale(slices.get(i).scale(), slices.get(i).scale());
            document.add(image);
        }
    }

    private static List<ChartSlice> buildSlices(ChartSpec spec, ProjectReportOptions.ProjectReportScaleMode scaleMode, float availableWidth, float availableHeight) {
        List<ChartSlice> slices = new ArrayList<>();
        switch (scaleMode) {
            case CURRENT_TILED -> {
                float segmentWidth = availableWidth;
                float segmentHeight = availableHeight;
                int cols = Math.max(1, (int) Math.ceil(spec.width / segmentWidth));
                int rows = Math.max(1, (int) Math.ceil(spec.height / segmentHeight));
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        float x = col * segmentWidth;
                        float y = row * segmentHeight;
                        float width = Math.min(segmentWidth, spec.width - x);
                        float height = Math.min(segmentHeight, spec.height - y);
                        slices.add(new ChartSlice(buildSvg(spec, x, y, width, height, width, height), 1f));
                    }
                }
            }
            case FIT_WIDTH -> {
                float scale = Math.min(availableWidth / spec.width, 1f);
                float viewHeight = availableHeight / scale;
                int rows = Math.max(1, (int) Math.ceil(spec.height / viewHeight));
                for (int row = 0; row < rows; row++) {
                    float y = row * viewHeight;
                    float height = Math.min(viewHeight, spec.height - y);
                    slices.add(new ChartSlice(buildSvg(spec, 0, y, spec.width, height, spec.width, height), scale));
                }
            }
            case FIT_PAGE -> {
                float scale = Math.min(Math.min(availableWidth / spec.width, availableHeight / spec.height), 1f);
                slices.add(new ChartSlice(buildSvg(spec, 0, 0, spec.width, spec.height, spec.width, spec.height), scale));
            }
        }
        return slices;
    }

    private static ChartSpec buildChartSpec(MainController controller, List<ChartRow> rows, DateRange range, ProjectReportOptions options) {
        double currentDayWidth = controller != null && controller.getGanttChart() != null
                ? controller.getGanttChart().getDayWidth()
                : ExportRenderEngine.getPreferredDayWidth();
        Settings settings = AppSettings.get();
        long totalDays = ChronoUnit.DAYS.between(range.start, range.end) + 1;

        float dayWidth = switch (options.scaleMode()) {
            case AUTO_APP, CURRENT_TILED -> (float) Math.max(1.5, currentDayWidth);
            case FIT_WIDTH -> Math.max(2.0f, 1100f / Math.max(totalDays, 1));
            case FIT_HEIGHT -> (float) Math.max(1.5, currentDayWidth);
            case FIT_PAGE -> (float) Math.max(2.0, Math.min(currentDayWidth, 1400.0 / Math.max(totalDays, 1)));
        };
        float chartWidth = totalDays * dayWidth;
        float chartHeight = TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + BOTTOM_HEADER_HEIGHT + rows.size() * ROW_HEIGHT;
        return new ChartSpec(rows, range, dayWidth, chartWidth, chartHeight, detectScaleMode(dayWidth), settings, new HashMap<>());
    }

    private static String buildSvg(ChartSpec spec, float viewX, float viewY, float viewWidth, float viewHeight, float renderWidth, float renderHeight) {
        StringBuilder svg = new StringBuilder(32000);
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(renderWidth)
                .append("' height='").append(renderHeight)
                .append("' viewBox='").append(viewX).append(' ').append(viewY).append(' ')
                .append(viewWidth).append(' ').append(viewHeight).append("'>");
        svg.append("<defs><marker id='arrow' markerWidth='8' markerHeight='8' refX='7' refY='4' orient='auto' markerUnits='strokeWidth'>")
                .append("<path d='M0,0 L8,4 L0,8 z' fill='#64748b'/></marker></defs>");
        svg.append("<rect x='0' y='0' width='").append(spec.width).append("' height='").append(spec.height).append("' fill='white'/>");
        appendHeaders(svg, spec);
        appendGrid(svg, spec);
        appendRowsAndBars(svg, spec);
        appendDependencies(svg, spec);
        appendFinancialMarkers(svg, spec);
        svg.append("</svg>");
        return svg.toString();
    }

    private static void appendHeaders(StringBuilder svg, ChartSpec spec) {
        LocalDate cursor = spec.range.start;
        while (!cursor.isAfter(spec.range.end)) {
            LocalDate blockEnd = LocalDate.of(cursor.getYear(), 12, 31);
            if (blockEnd.isAfter(spec.range.end)) blockEnd = spec.range.end;
            float x = xForDate(spec, cursor);
            float w = widthForDates(spec, cursor, blockEnd);
            rect(svg, x, 0, w, TOP_HEADER_HEIGHT, "#dde7f3", "#cbd5e1");
            text(svg, center(x, w), 14, String.valueOf(cursor.getYear()), "#0f172a", 11, true);
            cursor = blockEnd.plusDays(1);
        }

        cursor = spec.range.start;
        while (!cursor.isAfter(spec.range.end)) {
            LocalDate blockEnd;
            String label;
            if (spec.scale == ChartScale.YEAR) {
                int half = cursor.getMonthValue() <= 6 ? 1 : 2;
                blockEnd = LocalDate.of(cursor.getYear(), half == 1 ? 6 : 12, half == 1 ? 30 : 31);
                label = half + " Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В Р В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’Вµ";
            } else if (spec.scale == ChartScale.MONTH || spec.scale == ChartScale.QUARTER) {
                int quarter = ((cursor.getMonthValue() - 1) / 3) + 1;
                int endMonth = quarter * 3;
                blockEnd = LocalDate.of(cursor.getYear(), endMonth, YearMonth.of(cursor.getYear(), endMonth).lengthOfMonth());
                label = quarter + " Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В Р В Р’В Р Р†Р вЂљР’В .";
            } else {
                blockEnd = YearMonth.from(cursor).atEndOfMonth();
                String month = cursor.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, RU);
                label = month.substring(0, 1).toUpperCase(RU) + month.substring(1);
            }
            if (blockEnd.isAfter(spec.range.end)) blockEnd = spec.range.end;
            float x = xForDate(spec, cursor);
            float w = widthForDates(spec, cursor, blockEnd);
            rect(svg, x, TOP_HEADER_HEIGHT, w, MIDDLE_HEADER_HEIGHT, "#edf3f8", "#d7dee8");
            text(svg, center(x, w), TOP_HEADER_HEIGHT + 14, label, "#0f172a", 10, true);
            cursor = blockEnd.plusDays(1);
        }

        if (spec.scale == ChartScale.DAY) {
            cursor = spec.range.start;
            while (!cursor.isAfter(spec.range.end)) {
                float x = xForDate(spec, cursor);
                rect(svg, x, TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT, spec.dayWidth, BOTTOM_HEADER_HEIGHT, isWeekend(cursor) ? "#fef2f2" : "#ffffff", "#e5e7eb");
                text(svg, x + spec.dayWidth / 2f, TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + 10, String.valueOf(cursor.getDayOfMonth()), dayColor(cursor), 9, true);
                text(svg, x + spec.dayWidth / 2f, TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + 20, shortDayName(cursor.getDayOfWeek()), dayColor(cursor), 8, false);
                cursor = cursor.plusDays(1);
            }
        } else if (spec.scale == ChartScale.WEEK) {
            cursor = spec.range.start;
            while (!cursor.isAfter(spec.range.end)) {
                LocalDate end = cursor.plusDays(6);
                if (end.isAfter(spec.range.end)) end = spec.range.end;
                float x = xForDate(spec, cursor);
                float w = widthForDates(spec, cursor, end);
                rect(svg, x, TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT, w, BOTTOM_HEADER_HEIGHT, "#ffffff", "#e5e7eb");
                text(svg, center(x, w), TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + 15, "Р В Р’В Р вЂ™Р’В Р В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р вЂ™Р’В Р В РЎС›Р Р†Р вЂљР’В. " + cursor.get(ChronoField.ALIGNED_WEEK_OF_YEAR), "#0f172a", 9, false);
                cursor = end.plusDays(1);
            }
        } else {
            cursor = spec.range.start;
            while (!cursor.isAfter(spec.range.end)) {
                LocalDate end = YearMonth.from(cursor).atEndOfMonth();
                if (end.isAfter(spec.range.end)) end = spec.range.end;
                float x = xForDate(spec, cursor);
                float w = widthForDates(spec, cursor, end);
                rect(svg, x, TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT, w, BOTTOM_HEADER_HEIGHT, "#ffffff", "#e5e7eb");
                String month = cursor.getMonth().getDisplayName(TextStyle.SHORT_STANDALONE, RU).replace(".", "");
                text(svg, center(x, w), TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + 15, month, "#0f172a", 9, false);
                cursor = end.plusDays(1);
            }
        }
    }

    private static void appendGrid(StringBuilder svg, ChartSpec spec) {
        float gridTop = TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + BOTTOM_HEADER_HEIGHT;
        float gridHeight = spec.height - gridTop;
        LocalDate today = LocalDate.now();
        if (!today.isBefore(spec.range.start) && !today.isAfter(spec.range.end)) {
            float x = xForDate(spec, today);
            svg.append("<rect x='").append(x).append("' y='").append(gridTop).append("' width='").append(Math.max(spec.dayWidth, 2f))
                    .append("' height='").append(gridHeight).append("' fill='#fee2e2' opacity='0.55'/>");
            svg.append("<line x1='").append(x + spec.dayWidth / 2f).append("' y1='").append(gridTop).append("' x2='")
                    .append(x + spec.dayWidth / 2f).append("' y2='").append(spec.height).append("' stroke='#ef4444' stroke-width='1.2'/>");
        }
        for (int i = 0; i < spec.rows.size(); i++) {
            float y = gridTop + i * ROW_HEIGHT;
            if (i % 2 == 1) {
                svg.append("<rect x='0' y='").append(y).append("' width='").append(spec.width).append("' height='").append(ROW_HEIGHT).append("' fill='#f8fafc'/>");
            }
            svg.append("<line x1='0' y1='").append(y).append("' x2='").append(spec.width).append("' y2='").append(y).append("' stroke='#e9eef7' stroke-width='1'/>");
        }
        svg.append("<line x1='0' y1='").append(spec.height).append("' x2='").append(spec.width).append("' y2='").append(spec.height).append("' stroke='#e9eef7' stroke-width='1'/>");
        LocalDate cursor = spec.range.start;
        while (!cursor.isAfter(spec.range.end)) {
            float x = xForDate(spec, cursor);
            if (isWeekend(cursor)) {
                svg.append("<rect x='").append(x).append("' y='").append(gridTop).append("' width='").append(spec.dayWidth).append("' height='").append(gridHeight).append("' fill='#fefbf3' opacity='0.7'/>");
            }
            svg.append("<line x1='").append(x).append("' y1='").append(gridTop).append("' x2='").append(x).append("' y2='").append(spec.height).append("' stroke='#dbe5f1' stroke-width='1'/>");
            cursor = cursor.plusDays(1);
        }
    }

    private static void appendRowsAndBars(StringBuilder svg, ChartSpec spec) {
        float gridTop = TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + BOTTOM_HEADER_HEIGHT;
        for (int index = 0; index < spec.rows.size(); index++) {
            ChartRow row = spec.rows.get(index);
            float y = gridTop + index * ROW_HEIGHT;
            float centerY = y + ROW_HEIGHT / 2f;
            float x = xForDate(spec, row.start);
            float w = widthForDates(spec, row.start, row.end);

            if (row.task != null && row.task.isMilestone()) {
                float centerX = x + spec.dayWidth / 2f;
                float half = Math.max(4f, Math.min(9f, spec.dayWidth * 0.45f));
                String color = resolveTaskColor(spec.settings, row.task);
                svg.append("<polygon points='").append(centerX).append(',').append(centerY - half).append(' ')
                        .append(centerX + half).append(',').append(centerY).append(' ')
                        .append(centerX).append(',').append(centerY + half).append(' ')
                        .append(centerX - half).append(',').append(centerY)
                        .append("' fill='").append(color).append("' stroke='").append(darker(color)).append("' stroke-width='1'/>");
                spec.anchors.put(row.position, new BarAnchor(centerX, centerX, centerY));
                continue;
            }

            float barHeight = row.subTask ? 12f : 13f;
            float top = y + (ROW_HEIGHT - barHeight) / 2f;
            String fill = row.subTask ? resolveSubTaskColor(spec.settings) : resolveTaskColor(spec.settings, row.task);
            float width = Math.max(w, row.subTask ? 5f : 7f);
            svg.append("<rect x='").append(x).append("' y='").append(top).append("' width='").append(width)
                    .append("' height='").append(barHeight).append("' rx='").append(row.subTask ? 4 : 5).append("' ry='").append(row.subTask ? 4 : 5)
                    .append("' fill='").append(fill).append("'/>");
            spec.anchors.put(row.position, new BarAnchor(x, x + width, centerY));
        }
    }

    private static void appendDependencies(StringBuilder svg, ChartSpec spec) {
        int connectionIndex = 0;
        for (ChartRow row : spec.rows) {
            for (TaskDependency dependency : row.dependencies) {
                if (dependency == null || dependency.getPredecessorPosition() == null || dependency.getPredecessorPosition().isBlank()) {
                    continue;
                }
                BarAnchor source = spec.anchors.get(dependency.getPredecessorPosition().trim());
                BarAnchor target = spec.anchors.get(row.position);
                if (source == null || target == null) {
                    continue;
                }

                String type = normalizeType(dependency.getType());
                boolean sourceUsesEnd = "FS".equals(type) || "FF".equals(type);
                boolean targetUsesEnd = "FF".equals(type) || "SF".equals(type);
                float fromX = sourceUsesEnd ? source.endX : source.startX;
                float toX = targetUsesEnd ? target.endX : target.startX;
                float fromY = source.centerY;
                float toY = target.centerY;
                float laneX = (toX >= fromX + 24f)
                        ? fromX + 18f + (connectionIndex % 3) * 8f
                        : Math.max(fromX, toX) + 18f + (connectionIndex % 3) * 10f;

                String color = dependencyColor(spec.settings, type);
                svg.append("<polyline points='").append(fromX).append(',').append(fromY).append(' ')
                        .append(laneX).append(',').append(fromY).append(' ')
                        .append(laneX).append(',').append(toY).append(' ')
                        .append(toX).append(',').append(toY)
                        .append("' fill='none' stroke='").append(color)
                        .append("' stroke-width='1.35' marker-end='url(#arrow)'/>");
                connectionIndex++;
            }
        }
    }

    private static void appendFinancialMarkers(StringBuilder svg, ChartSpec spec) {
        float gridTop = TOP_HEADER_HEIGHT + MIDDLE_HEADER_HEIGHT + BOTTOM_HEADER_HEIGHT;
        for (int index = 0; index < spec.rows.size(); index++) {
            ChartRow row = spec.rows.get(index);
            if (row.task == null || row.subTask) {
                continue;
            }
            float rowY = gridTop + index * ROW_HEIGHT;
            appendMarker(svg, spec, row.task.getContractAdvanceDate(), rowY, "#2563eb", "Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›");
            appendMarker(svg, spec, row.task.getKsPaymentDate(), rowY, "#d97706", "Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В Р В Р’В Р В РІР‚в„–");
        }
    }

    private static void appendMarker(StringBuilder svg, ChartSpec spec, LocalDate date, float rowY, String color, String label) {
        if (date == null || date.isBefore(spec.range.start) || date.isAfter(spec.range.end)) {
            return;
        }
        float x = xForDate(spec, date) + spec.dayWidth / 2f;
        float top = rowY + 2f;
        float bottom = rowY + ROW_HEIGHT - 2f;
        svg.append("<line x1='").append(x).append("' y1='").append(top).append("' x2='").append(x).append("' y2='").append(bottom)
                .append("' stroke='").append(color).append("' stroke-width='1.5'/>");
        svg.append("<circle cx='").append(x).append("' cy='").append(top + 3).append("' r='3.5' fill='").append(color).append("'/>");
        float badgeX = x + 4f;
        float badgeY = rowY + 4f;
        svg.append("<rect x='").append(badgeX).append("' y='").append(badgeY).append("' width='18' height='12' rx='6' ry='6' fill='").append(color).append("'/>");
        text(svg, badgeX + 9, badgeY + 8.6f, label, "#ffffff", 7.2f, true);
    }

    private static ChartScale detectScaleMode(float dayWidth) {
        if (dayWidth >= 18f) return ChartScale.DAY;
        if (dayWidth >= 8f) return ChartScale.WEEK;
        if (dayWidth >= 3.5f) return ChartScale.MONTH;
        if (dayWidth >= 2.2f) return ChartScale.QUARTER;
        return ChartScale.YEAR;
    }

    private static float xForDate(ChartSpec spec, LocalDate date) {
        return (float) ChronoUnit.DAYS.between(spec.range.start, date) * spec.dayWidth;
    }

    private static float widthForDates(ChartSpec spec, LocalDate start, LocalDate end) {
        return Math.max((float) (ChronoUnit.DAYS.between(start, end) + 1) * spec.dayWidth, spec.dayWidth);
    }

    private static float center(float x, float w) {
        return x + w / 2f;
    }

    private static void rect(StringBuilder svg, float x, float y, float w, float h, String fill, String stroke) {
        svg.append("<rect x='").append(x).append("' y='").append(y).append("' width='").append(w).append("' height='").append(h)
                .append("' fill='").append(fill).append("' stroke='").append(stroke).append("' stroke-width='0.6'/>");
    }

    private static void text(StringBuilder svg, float x, float y, String text, String color, float fontSize, boolean bold) {
        svg.append("<text x='").append(x).append("' y='").append(y)
                .append("' fill='").append(color)
                .append("' font-size='").append(fontSize)
                .append("' text-anchor='middle'")
                .append(bold ? " font-weight='700'" : "")
                .append(" font-family='Segoe UI, Arial, sans-serif'>")
                .append(escape(text)).append("</text>");
    }

    private static String resolveTaskColor(Settings settings, Task task) {
        if (task != null && task.isCriticalDependency()) {
            return settings == null ? "#dc2626" : safeColor(settings.getCriticalTaskColor(), "#dc2626");
        }
        return settings == null ? "#2563eb" : safeColor(settings.getTaskColor(), "#2563eb");
    }

    private static String resolveSubTaskColor(Settings settings) {
        return settings == null ? "#16a34a" : safeColor(settings.getSubTaskColor(), "#16a34a");
    }

    private static String dependencyColor(Settings settings, String type) {
        if (settings == null) {
            return switch (normalizeType(type)) {
                case "SS" -> "#2563eb";
                case "FF" -> "#0f766e";
                case "SF" -> "#b45309";
                default -> "#7c848f";
            };
        }
        return switch (normalizeType(type)) {
            case "SS" -> safeColor(settings.getDependencySsColor(), "#2563eb");
            case "FF" -> safeColor(settings.getDependencyFfColor(), "#0f766e");
            case "SF" -> safeColor(settings.getDependencySfColor(), "#b45309");
            default -> safeColor(settings.getDependencyFsColor(), "#7c848f");
        };
    }

    private static String normalizeType(String type) {
        return type == null ? "FS" : type.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeColor(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String dayColor(LocalDate date) {
        return isWeekend(date) ? "#dc2626" : "#0f172a";
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static String shortDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Р В Р’В Р вЂ™Р’В Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’В Р В Р’В Р Р†Р вЂљР’В¦";
            case TUESDAY -> "Р В Р’В Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ";
            case WEDNESDAY -> "Р В Р’В Р вЂ™Р’В Р В Р’В Р В РІР‚в„–Р В Р’В Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎв„ў";
            case THURSDAY -> "Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ";
            case FRIDAY -> "Р В Р’В Р вЂ™Р’В Р В Р Р‹Р РЋРЎСџР В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ";
            case SATURDAY -> "Р В Р’В Р вЂ™Р’В Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В±";
            case SUNDAY -> "Р В Р’В Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚Сљ";
        };
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String darker(String ignored) {
        return "#1f2937";
    }

    private static String escape(String value) {
        return safe(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static PageSize resolvePageSize(ProjectReportOptions.ProjectPaperFormat paper, boolean landscape) {
        PageSize size = switch (paper) {
            case A4 -> PageSize.A4;
            case A2 -> PageSize.A2;
            case A1 -> PageSize.A1;
            default -> PageSize.A3;
        };
        return landscape ? size.rotate() : size;
    }

    private static DateRange computeRange(List<ChartRow> rows) {
        LocalDate min = rows.stream().map(ChartRow::start).filter(d -> d != null).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate max = rows.stream().map(ChartRow::end).filter(d -> d != null).max(LocalDate::compareTo).orElse(min);
        return new DateRange(min, max);
    }

    private static List<ChartRow> flattenRows(List<Task> tasks) {
        List<ChartRow> rows = new ArrayList<>();
        if (tasks == null) {
            return rows;
        }
        for (Task task : tasks) {
            if (task == null) continue;
            rows.add(ChartRow.task(task));
            if (task.getSubTasks() != null) {
                for (SubTask subTask : task.getSubTasks()) {
                    rows.add(ChartRow.subTask(task, subTask));
                }
            }
        }
        return rows;
    }

    private static List<ChartRow> autoExportRows(MainController controller) {
        if (controller == null) {
            return List.of();
        }

        List<Object> orderedItems = controller.getDisplayOrderSnapshot();
        if (orderedItems != null && !orderedItems.isEmpty()) {
            List<ChartRow> rows = new ArrayList<>(orderedItems.size());
            for (Object item : orderedItems) {
                if (item instanceof Task task) {
                    rows.add(ChartRow.task(task));
                } else if (item instanceof SubTask subTask) {
                    Task parentTask = subTask.getParentTask();
                    if (parentTask != null) {
                        rows.add(ChartRow.subTask(parentTask, subTask));
                    }
                }
            }
            if (!rows.isEmpty()) {
                return rows;
            }
        }

        return flattenRows(controller.getTasks() == null ? List.of() : controller.getTasks());
    }

    private enum ChartScale { DAY, WEEK, MONTH, QUARTER, YEAR }
    private record DateRange(LocalDate start, LocalDate end) {}
    private record WorkspaceSnapshot(byte[] pngBytes, float width, float height) {}
    private record BarAnchor(float startX, float endX, float centerY) {}
    private record ChartSlice(String svg, float scale) {}
    private record ChartSpec(List<ChartRow> rows, DateRange range, float dayWidth, float width, float height, ChartScale scale, Settings settings, Map<String, BarAnchor> anchors) {}
    private record LiveColumn(String title, String key, float width, TextAlignment alignment) {}
    private record ExportTableRow(Map<String, String> values) {
        static ExportTableRow from(ChartRow row) {
            Map<String, String> values = new HashMap<>();
            values.put("position", safe(row.position));
            values.put("name", safe(row.label));
            values.put("company", safe(row.company));
            values.put("start", formatDate(row.start));
            values.put("end", formatDate(row.end));
            values.put("unit", safe(row.unit));
            values.put("volProject", safe(row.projectVolume));
            values.put("volActual", safe(row.actualVolume));
            values.put("percent", safe(row.percentText));
            values.put("status", safe(row.status));
            values.put("priority", safe(row.priority));
            return new ExportTableRow(values);
        }

        String value(String key) {
            return values.getOrDefault(key, "");
        }
    }

    private record ChartRow(
            boolean subTask,
            String position,
            String label,
            String company,
            LocalDate start,
            LocalDate end,
            String unit,
            String projectVolume,
            String actualVolume,
            String percentText,
            String status,
            String priority,
            List<TaskDependency> dependencies,
            Task task
    ) {
        private static ChartRow task(Task task) {
            LocalDate end = task.isMilestone() ? (task.getStart() != null ? task.getStart() : task.getEnd()) : task.getEnd();
            return new ChartRow(
                    false,
                    safe(task.getPosition()),
                    safe(task.getName()),
                    safe(task.getCompany()),
                    task.getStart(),
                    end,
                    "",
                    "",
                    "",
                    formatPercent(task.getPercent()),
                    safe(task.getStatus()),
                    safe(task.getPriority()),
                    task.getDependencies() == null ? List.of() : task.getDependencies(),
                    task
            );
        }

        private static ChartRow subTask(Task task, SubTask subTask) {
            return new ChartRow(
                    true,
                    safe(subTask.getSubPosition()),
                    "\u2003\u2003" + safe(subTask.getName()),
                    safe(subTask.getCompany()),
                    subTask.getStart(),
                    subTask.getEnd(),
                    safe(subTask.getUnitOfMeasurement()),
                    formatInt(subTask.getVolumeProject()),
                    formatInt(subTask.getVolumeActual()),
                    formatPercent(subTask.getPercent()),
                    safe(subTask.getStatus()),
                    "",
                    subTask.getDependencies() == null ? List.of() : subTask.getDependencies(),
                    task
            );
        }
    }

    private static String formatInt(int value) {
        return value == 0 ? "" : String.valueOf(value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.US, "%.0f%%", value);
    }
}

