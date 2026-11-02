package io.angistrem.employeemanagement.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class GanttDateService {

    public int weekNumberInMonth(LocalDate date) {
        LocalDate firstOfMonth = date.withDayOfMonth(1);
        LocalDate firstWeekStart = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate dateWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return (int) ChronoUnit.WEEKS.between(firstWeekStart, dateWeekStart) + 1;
    }

    public String shortDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Пн";
            case TUESDAY -> "Вт";
            case WEDNESDAY -> "Ср";
            case THURSDAY -> "Чт";
            case FRIDAY -> "Пт";
            case SATURDAY -> "Сб";
            case SUNDAY -> "Вс";
        };
    }

    public boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public boolean containsWeekend(LocalDate start, LocalDate end) {
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (isWeekend(d)) {
                return true;
            }
            d = d.plusDays(1);
        }
        return false;
    }

    public String buildDateBlockLabel(LocalDate start, LocalDate end) {
        return buildDateBlockLabel(start, end, ChronoUnit.DAYS.between(start, end) + 1);
    }

    public String buildDateBlockLabel(LocalDate start, LocalDate end, long daysInBlock) {
        if (start.equals(end) || daysInBlock <= 1) {
            return String.valueOf(start.getDayOfMonth());
        }
        if (daysInBlock >= 9) {
            return start.getDayOfMonth() + "." + start.getMonthValue()
                    + "-" + end.getDayOfMonth() + "." + end.getMonthValue();
        }
        if (start.getMonthValue() == end.getMonthValue() && start.getYear() == end.getYear()) {
            return start.getDayOfMonth() + "-" + end.getDayOfMonth();
        }
        return start.getDayOfMonth() + "/" + start.getMonthValue()
                + "-" + end.getDayOfMonth() + "/" + end.getMonthValue();
    }

    public String formatPeriod(LocalDate start, LocalDate end, DateTimeFormatter formatter) {
        if (start == null && end == null) {
            return "Не задан";
        }
        if (start == null) {
            return "до " + end.format(formatter);
        }
        if (end == null) {
            return "с " + start.format(formatter);
        }
        return start.format(formatter) + " - " + end.format(formatter);
    }
}
