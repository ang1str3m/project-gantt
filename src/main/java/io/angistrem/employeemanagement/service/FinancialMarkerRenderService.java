package io.angistrem.employeemanagement.service;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class FinancialMarkerRenderService {

    private final FinancialMarkerVisualTokensService tokens = new FinancialMarkerVisualTokensService();

    public record BadgeMetrics(double width, double height) {
    }

    public record MarkerGeometry(double x, double lineTop, double lineBottom, double hitX, double hitY, double hitWidth, double hitHeight) {
    }

    public double markerX(LocalDate chartStartDate, LocalDate markerDate, double dayWidth) {
        if (chartStartDate == null || markerDate == null) {
            return 0.0;
        }
        return ChronoUnit.DAYS.between(chartStartDate, markerDate) * dayWidth + (dayWidth / 2.0);
    }

    public MarkerGeometry geometry(double x, double rowY, double rowHeight) {
        return new MarkerGeometry(
                x,
                rowY + tokens.lineTopOffset(),
                rowY + rowHeight - tokens.lineBottomOffset(),
                x - tokens.hitXOffset(),
                rowY + tokens.hitYOffset(),
                tokens.hitWidth(),
                Math.max(tokens.hitMinHeight(), rowHeight - tokens.lineBottomOffset())
        );
    }

    public BadgeMetrics badgeMetrics(String shortLabel) {
        Text measureText = new Text(shortLabel);
        measureText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 9));
        double textWidth = Math.max(tokens.badgeMinTextWidth(), measureText.getLayoutBounds().getWidth());
        return new BadgeMetrics(textWidth + tokens.badgeHorizontalPadding(), tokens.badgeHeight());
    }

    public Line createGlowLine(MarkerGeometry geometry, Color color, boolean blocking) {
        Line glowLine = new Line(geometry.x(), geometry.lineTop(), geometry.x(), geometry.lineBottom());
        glowLine.setStroke(color.deriveColor(0, 1, 1, tokens.glowOpacity(blocking)));
        glowLine.setStrokeWidth(tokens.glowWidth(blocking));
        glowLine.setStrokeLineCap(StrokeLineCap.ROUND);
        glowLine.setManaged(false);
        glowLine.setMouseTransparent(true);
        return glowLine;
    }

    public Line createMarkerLine(MarkerGeometry geometry, Color color, boolean blocking) {
        Line markerLine = new Line(geometry.x(), geometry.lineTop(), geometry.x(), geometry.lineBottom());
        markerLine.setStroke(color.deriveColor(0, 1, 1, 0.98));
        markerLine.setStrokeWidth(tokens.markerWidth(blocking));
        markerLine.setStrokeLineCap(StrokeLineCap.ROUND);
        double[] dashPattern = tokens.markerDashPattern(blocking);
        markerLine.getStrokeDashArray().setAll(dashPattern[0], dashPattern[1]);
        markerLine.setManaged(false);
        return markerLine;
    }

    public Circle createHeadDot(MarkerGeometry geometry, double rowY, Color color, boolean blocking) {
        Circle headDot = new Circle(geometry.x(), tokens.headDotY(rowY), tokens.headDotRadius(blocking));
        headDot.setFill(color);
        headDot.setStroke(tokens.headDotStroke());
        headDot.setStrokeWidth(tokens.headDotStrokeWidth());
        headDot.setManaged(false);
        return headDot;
    }

    public StackPane createBadgePane(String shortLabel, Color color, double badgeX, double badgeY, BadgeMetrics metrics) {
        Rectangle badgeBg = new Rectangle(0, 0, metrics.width(), metrics.height());
        badgeBg.setArcWidth(tokens.badgeArc());
        badgeBg.setArcHeight(tokens.badgeArc());
        badgeBg.setFill(color);
        badgeBg.setStroke(tokens.badgeStroke());
        badgeBg.setStrokeWidth(tokens.badgeStrokeWidth());

        Label badgeLabel = new Label(shortLabel);
        badgeLabel.setMouseTransparent(true);
        badgeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: 900;");

        StackPane badgePane = new StackPane(badgeBg, badgeLabel);
        badgePane.setManaged(false);
        badgePane.setMouseTransparent(false);
        badgePane.setPickOnBounds(true);
        badgePane.resizeRelocate(badgeX, badgeY, metrics.width(), metrics.height());
        return badgePane;
    }

    public Label createBlockingAccent(double chartRightLimit, double badgeX, double badgeY, BadgeMetrics metrics, double rowY) {
        Label accent = new Label("!");
        accent.setManaged(false);
        accent.setStyle("-fx-font-size: 8px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #7f1d1d;"
                + "-fx-background-color: #fef2f2;"
                + "-fx-border-color: rgba(248,113,113,0.72);"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 8;"
                + "-fx-border-radius: 8;"
                + "-fx-padding: 0 4 0 4;");
        accent.autosize();
        double accentX = Math.min(chartRightLimit - accent.prefWidth(-1), badgeX + metrics.width() - accent.prefWidth(-1) / tokens.accentXShiftFactor());
        double accentY = Math.max(tokens.accentMinY(rowY), badgeY + metrics.height() + tokens.accentBelowBadgeGap());
        accent.relocate(accentX, accentY);
        return accent;
    }

    public Rectangle createHitHandle(MarkerGeometry geometry) {
        Rectangle hitHandle = new Rectangle(
                geometry.hitX(),
                geometry.hitY(),
                geometry.hitWidth(),
                geometry.hitHeight()
        );
        hitHandle.setArcWidth(tokens.hitArc());
        hitHandle.setArcHeight(tokens.hitArc());
        hitHandle.setFill(Color.TRANSPARENT);
        hitHandle.setManaged(false);
        hitHandle.setPickOnBounds(true);
        return hitHandle;
    }

    public String tooltipText(String title, LocalDate date, DateTimeFormatter formatter, boolean blocking) {
        return title + ": " + date.format(formatter)
                + (blocking
                ? "\n\u0411\u043b\u043e\u043a\u0438\u0440\u0443\u0435\u0442 \u0441\u0442\u0430\u0440\u0442 \u0437\u0430\u0434\u0430\u0447\u0438 \u0434\u043e \u043d\u0430\u0441\u0442\u0443\u043f\u043b\u0435\u043d\u0438\u044f \u044d\u0442\u043e\u0439 \u0434\u0430\u0442\u044b"
                : "\n\u0418\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u043e\u043d\u043d\u0430\u044f \u043a\u043e\u043d\u0442\u0440\u043e\u043b\u044c\u043d\u0430\u044f \u0434\u0430\u0442\u0430");
    }
}
