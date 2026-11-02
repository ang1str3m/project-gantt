package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.service.GanttItemDateInteractionService.DragSession;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GanttTextService {

    public String dragActionLabel(DragSession session) {
        if (session == null || session.mode() == null) {
            return "\u0421\u0434\u0432\u0438\u0433";
        }
        return switch (session.mode()) {
            case MOVE -> "\u0421\u0434\u0432\u0438\u0433";
            case RESIZE_START -> "\u041d\u0430\u0447\u0430\u043b\u043e";
            case RESIZE_END -> "\u041e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435";
        };
    }

    public String dragBadgeText(DragSession session, LocalDate newStart, LocalDate newEnd, DateTimeFormatter formatter) {
        if (formatter == null) {
            formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        }
        String action = dragActionLabel(session);
        if (session != null && session.milestone()) {
            return action + ": " + newEnd.format(formatter);
        }
        return action + ": " + newStart.format(formatter) + " - " + newEnd.format(formatter);
    }

    public String financialMarkerDragText(String shortLabel, LocalDate date, DateTimeFormatter formatter) {
        if (formatter == null) {
            formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        }
        return shortLabel + ": " + date.format(formatter);
    }

    public String milestoneTooltip(String baseTooltipText, LocalDate date, DateTimeFormatter formatter) {
        if (baseTooltipText == null || baseTooltipText.isBlank() || date == null) {
            return baseTooltipText;
        }
        if (formatter == null) {
            formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        }
        return baseTooltipText + "\n\u0412\u0435\u0445\u0430: " + date.format(formatter);
    }
}
