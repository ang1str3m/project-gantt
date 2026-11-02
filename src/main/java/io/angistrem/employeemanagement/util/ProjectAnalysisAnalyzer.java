package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProjectAnalysisAnalyzer {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru"));

    private ProjectAnalysisAnalyzer() {
    }

    public record ImpactItem(
            String key,
            String label,
            String assignee,
            LocalDate start,
            LocalDate end,
            long durationDays,
            boolean criticalPath,
            int slackDays,
            int oneDayImpact,
            int threeDayImpact,
            double plannedBudget,
            double actualBudget,
            int assignedResources,
            boolean overloadedByResources
    ) {
        public boolean isCriticalInfluencer() {
            return oneDayImpact > 0 || threeDayImpact > 0;
        }

        public double budgetVariance() {
            return actualBudget - plannedBudget;
        }
    }

    public record BudgetPressure(
            double plannedTotal,
            double actualTotal,
            double variance,
            String peakMonthLabel,
            double peakMonthAmount,
            int scheduledPayments
    ) {
    }

    public record ResourcePressure(
            long resourcesAssigned,
            long overloadedResources,
            String hottestResourceName,
            int hottestResourceConflictDays
    ) {
    }

    public record Analysis(
            LocalDate projectStart,
            LocalDate projectEnd,
            long totalDays,
            String finalMilestoneLabel,
            CriticalPathAnalyzer.Analysis criticalPath,
            List<ImpactItem> rankedItems,
            ImpactItem primaryItem,
            BudgetPressure budgetPressure,
            ResourcePressure resourcePressure
    ) {
        public boolean isMeaningful() {
            return projectStart != null
                    && projectEnd != null
                    && totalDays > 0
                    && primaryItem != null
                    && rankedItems != null
                    && !rankedItems.isEmpty();
        }
    }

    private record NodeRef(
            String key,
            String label,
            String assignee,
            LocalDate start,
            LocalDate end,
            long durationDays,
            boolean criticalPath
    ) {
    }

    public static Analysis analyze(List<Task> tasks) {
        return analyze(tasks, List.of(), List.of());
    }

    public static Analysis analyze(List<Task> tasks, List<ProjectResource> resources, List<BudgetItem> budgetItems) {
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }

        CriticalPathAnalyzer.Analysis critical = CriticalPathAnalyzer.analyze(tasks);
        LocalDate baselineStart = projectStart(tasks);
        LocalDate baselineEnd = projectEnd(tasks);
        long totalDays = durationInclusive(baselineStart, baselineEnd);
        if (baselineStart == null || baselineEnd == null || totalDays <= 0) {
            return null;
        }

        Set<String> criticalKeys = new HashSet<>();
        if (critical != null && critical.steps() != null) {
            critical.steps().forEach(step -> criticalKeys.add(step.key()));
        }

        BudgetPressure budgetPressure = analyzeBudgetPressure(budgetItems);
        ResourcePressure resourcePressure = analyzeResourcePressure(tasks, resources);

        List<NodeRef> nodes = collectNodes(tasks, criticalKeys);
        List<ImpactItem> rankedItems = new ArrayList<>();
        int maxProbe = (int) Math.max(7, Math.min(totalDays, 45));

        for (NodeRef node : nodes) {
            int oneDayImpact = simulateImpact(tasks, node.key(), baselineEnd, 1);
            int threeDayImpact = simulateImpact(tasks, node.key(), baselineEnd, 3);
            int slackDays = findSlackDays(tasks, node.key(), baselineEnd, maxProbe);
            double plannedBudget = budgetForNode(budgetItems, node.key(), true);
            double actualBudget = budgetForNode(budgetItems, node.key(), false);
            int assignedResources = assignedResourcesForNode(tasks, node.key());
            boolean overloadedByResources = nodeHasResourceOverload(tasks, node.key());

            rankedItems.add(new ImpactItem(
                    node.key(),
                    node.label(),
                    node.assignee(),
                    node.start(),
                    node.end(),
                    node.durationDays(),
                    node.criticalPath(),
                    slackDays,
                    oneDayImpact,
                    threeDayImpact,
                    plannedBudget,
                    actualBudget,
                    assignedResources,
                    overloadedByResources
            ));
        }

        rankedItems.sort(Comparator
                .comparingInt(ImpactItem::threeDayImpact).reversed()
                .thenComparingInt(ImpactItem::oneDayImpact).reversed()
                .thenComparing((ImpactItem item) -> item.overloadedByResources() ? 1 : 0, Comparator.reverseOrder())
                .thenComparingDouble(ImpactItem::budgetVariance).reversed()
                .thenComparingInt(ImpactItem::slackDays)
                .thenComparingLong(ImpactItem::durationDays).reversed()
                .thenComparing(ImpactItem::end, Comparator.nullsLast(Comparator.reverseOrder())));

        ImpactItem primary = rankedItems.stream()
                .filter(ImpactItem::isCriticalInfluencer)
                .findFirst()
                .orElseGet(() -> rankedItems.isEmpty() ? null : rankedItems.get(0));

        return new Analysis(
                baselineStart,
                baselineEnd,
                totalDays,
                findFinalMilestoneLabel(tasks, baselineEnd),
                critical,
                rankedItems.stream().limit(8).toList(),
                primary,
                budgetPressure,
                resourcePressure
        );
    }

    private static BudgetPressure analyzeBudgetPressure(List<BudgetItem> budgetItems) {
        if (budgetItems == null || budgetItems.isEmpty()) {
            return new BudgetPressure(0.0, 0.0, 0.0, "—", 0.0, 0);
        }

        double plannedTotal = 0.0;
        double actualTotal = 0.0;
        int scheduledPayments = 0;
        Map<YearMonth, Double> monthlyPlan = new LinkedHashMap<>();

        for (BudgetItem item : budgetItems) {
            if (item == null) {
                continue;
            }
            plannedTotal += item.getPlannedAmount();
            actualTotal += item.getActualAmount();
            if (item.getPlannedDate() != null) {
                scheduledPayments++;
                YearMonth month = YearMonth.from(item.getPlannedDate());
                monthlyPlan.merge(month, item.getPlannedAmount(), Double::sum);
            }
            if (item.getActualDate() != null && item.getPlannedDate() == null) {
                scheduledPayments++;
            }
        }

        YearMonth peakMonth = null;
        double peakMonthAmount = 0.0;
        for (Map.Entry<YearMonth, Double> entry : monthlyPlan.entrySet()) {
            if (entry.getValue() >= peakMonthAmount) {
                peakMonth = entry.getKey();
                peakMonthAmount = entry.getValue();
            }
        }

        return new BudgetPressure(
                plannedTotal,
                actualTotal,
                actualTotal - plannedTotal,
                peakMonth == null ? "—" : capitalizeMonth(peakMonth),
                peakMonthAmount,
                scheduledPayments
        );
    }

    private static ResourcePressure analyzeResourcePressure(List<Task> tasks, List<ProjectResource> resources) {
        ResourceLoadAnalyzer.Analysis resourceAnalysis = ResourceLoadAnalyzer.analyze(resources == null ? List.of() : resources, tasks);
        if (resourceAnalysis == null || !resourceAnalysis.hasData()) {
            return new ResourcePressure(0, 0, "—", 0);
        }

        String hottestResource = "—";
        int hottestDays = 0;
        for (ResourceLoadAnalyzer.ResourceLoadItem item : resourceAnalysis.items()) {
            int conflictDays = item.conflicts().size();
            if (conflictDays >= hottestDays) {
                hottestDays = conflictDays;
                hottestResource = item.resourceName();
            }
        }

        return new ResourcePressure(
                resourceAnalysis.assignedResources(),
                resourceAnalysis.overloadedResources(),
                hottestResource,
                hottestDays
        );
    }

    private static List<NodeRef> collectNodes(List<Task> tasks, Set<String> criticalKeys) {
        List<NodeRef> nodes = new ArrayList<>();
        for (Task task : tasks) {
            collectTaskNodeRecursive(task, criticalKeys, nodes);
        }
        return nodes;
    }

    private static void collectTaskNodeRecursive(Task task, Set<String> criticalKeys, List<NodeRef> nodes) {
        if (task == null) {
            return;
        }
        if (task.getPosition() != null && task.getStart() != null && task.getEnd() != null) {
            nodes.add(new NodeRef(
                    task.getPosition().trim(),
                    safe(task.getName()),
                    safeAssignee(task.getCompany()),
                    task.getStart(),
                    task.getEnd(),
                    durationInclusive(task.getStart(), task.getEnd()),
                    criticalKeys.contains(task.getPosition().trim())
            ));
        }

        if (task.isGroup()) {
            for (Task childTask : task.getObservableChildTasks()) {
                collectTaskNodeRecursive(childTask, criticalKeys, nodes);
            }
            return;
        }

        for (SubTask subTask : task.getObservableSubTasks()) {
            if (subTask == null || subTask.getSubPosition() == null || subTask.getStart() == null || subTask.getEnd() == null) {
                continue;
            }
            nodes.add(new NodeRef(
                    subTask.getSubPosition().trim(),
                    safe(subTask.getName()),
                    safeAssignee(subTask.getCompany()),
                    subTask.getStart(),
                    subTask.getEnd(),
                    durationInclusive(subTask.getStart(), subTask.getEnd()),
                    criticalKeys.contains(subTask.getSubPosition().trim())
            ));
        }
    }

    private static int findSlackDays(List<Task> tasks, String key, LocalDate baselineEnd, int maxProbe) {
        for (int delay = 1; delay <= maxProbe; delay++) {
            if (simulateImpact(tasks, key, baselineEnd, delay) > 0) {
                return delay - 1;
            }
        }
        return maxProbe;
    }

    private static int simulateImpact(List<Task> tasks, String key, LocalDate baselineEnd, int delayDays) {
        if (key == null || baselineEnd == null || delayDays <= 0) {
            return 0;
        }

        List<Task> clonedTasks = cloneTasks(tasks);
        try {
            shiftNode(clonedTasks, key, delayDays);
            TaskDependencyScheduler.applyConstraints(clonedTasks);
            LocalDate shiftedEnd = projectEnd(clonedTasks);
            if (shiftedEnd == null || !shiftedEnd.isAfter(baselineEnd)) {
                return 0;
            }
            return (int) ChronoUnit.DAYS.between(baselineEnd, shiftedEnd);
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    private static void shiftNode(List<Task> tasks, String key, int delayDays) {
        for (Task task : tasks) {
            if (task.getPosition() != null && key.equals(task.getPosition().trim())) {
                shiftTask(task, delayDays);
                return;
            }
            for (SubTask subTask : task.getObservableSubTasks()) {
                if (subTask.getSubPosition() != null && key.equals(subTask.getSubPosition().trim())) {
                    shiftSubTask(subTask, delayDays);
                    return;
                }
            }
            for (Task childTask : task.getObservableChildTasks()) {
                if (shiftNodeRecursive(childTask, key, delayDays)) {
                    return;
                }
            }
        }
    }

    private static boolean shiftNodeRecursive(Task task, String key, int delayDays) {
        if (task.getPosition() != null && key.equals(task.getPosition().trim())) {
            shiftTask(task, delayDays);
            return true;
        }
        for (SubTask subTask : task.getObservableSubTasks()) {
            if (subTask.getSubPosition() != null && key.equals(subTask.getSubPosition().trim())) {
                shiftSubTask(subTask, delayDays);
                return true;
            }
        }
        for (Task childTask : task.getObservableChildTasks()) {
            if (shiftNodeRecursive(childTask, key, delayDays)) {
                return true;
            }
        }
        return false;
    }

    private static void shiftTask(Task task, int delayDays) {
        if (task.getStart() != null) {
            task.setStart(task.getStart().plusDays(delayDays));
        }
        if (task.getEnd() != null) {
            task.setEnd(task.getEnd().plusDays(delayDays));
        }
        for (SubTask subTask : task.getObservableSubTasks()) {
            shiftSubTask(subTask, delayDays);
        }
        for (Task childTask : task.getObservableChildTasks()) {
            shiftTask(childTask, delayDays);
        }
    }

    private static void shiftSubTask(SubTask subTask, int delayDays) {
        if (subTask.getStart() != null) {
            subTask.setStart(subTask.getStart().plusDays(delayDays));
        }
        if (subTask.getEnd() != null) {
            subTask.setEnd(subTask.getEnd().plusDays(delayDays));
        }
    }

    private static List<Task> cloneTasks(List<Task> tasks) {
        List<Task> cloned = new ArrayList<>();
        Map<Task, Task> taskMap = new HashMap<>();

        for (Task original : tasks) {
            Task copy = cloneTaskWithoutChildren(original);
            cloned.add(copy);
            taskMap.put(original, copy);
        }

        for (Task original : tasks) {
            Task copyParent = taskMap.get(original);
            if (copyParent == null) {
                continue;
            }
            copyParent.setSubTasks(cloneSubTasks(original, copyParent));
            copyParent.getObservableChildTasks().setAll(cloneChildTasks(original, copyParent));
        }

        Task.syncCounterFromTasks(tasks);
        return cloned;
    }

    private static Task cloneTaskWithoutChildren(Task original) {
        Task copy = new Task();
        copy.setPosition(original.getPosition());
        copy.setName(original.getName());
        copy.setCompany(original.getCompany());
        copy.setStart(original.getStart());
        copy.setEnd(original.getEnd());
        copy.setStatus(original.getStatus());
        copy.setPriority(original.getPriority());
        copy.setTaskType(original.getTaskType());
        copy.setCriticalDependency(original.isCriticalDependency());
        copy.setContractAdvanceDate(original.getContractAdvanceDate());
        copy.setKsPaymentDate(original.getKsPaymentDate());
        copy.setRequireAdvanceBeforeStart(original.isRequireAdvanceBeforeStart());
        copy.setRequireKsBeforeStart(original.isRequireKsBeforeStart());
        copy.setDependencies(cloneDependencies(original.getDependencies()));
        copy.setPlannedCost(original.getPlannedCost());
        copy.setActualCost(original.getActualCost());
        copy.setResourceOverloaded(original.isResourceOverloaded());
        copy.setResourceLoadSummary(original.getResourceLoadSummary());
        return copy;
    }

    private static List<Task> cloneChildTasks(Task original, Task copyParent) {
        List<Task> copies = new ArrayList<>();
        for (Task child : original.getObservableChildTasks()) {
            Task childCopy = cloneTaskWithoutChildren(child);
            childCopy.setSubTasks(cloneSubTasks(child, childCopy));
            childCopy.getObservableChildTasks().setAll(cloneChildTasks(child, childCopy));
            copies.add(childCopy);
        }
        return copies;
    }

    private static List<SubTask> cloneSubTasks(Task original, Task copyParent) {
        List<SubTask> copySubTasks = new ArrayList<>();
        for (SubTask originalSubTask : original.getObservableSubTasks()) {
            SubTask copySubTask = new SubTask();
            copySubTask.setParentTask(copyParent);
            copySubTask.setSubPosition(originalSubTask.getSubPosition());
            copySubTask.setName(originalSubTask.getName());
            copySubTask.setCompany(originalSubTask.getCompany());
            copySubTask.setStart(originalSubTask.getStart());
            copySubTask.setEnd(originalSubTask.getEnd());
            copySubTask.setUnitOfMeasurement(originalSubTask.getUnitOfMeasurement());
            copySubTask.setVolumeProject(originalSubTask.getVolumeProject());
            copySubTask.setVolumeActual(originalSubTask.getVolumeActual());
            copySubTask.setStatus(originalSubTask.getStatus());
            copySubTask.setDependencies(cloneDependencies(originalSubTask.getDependencies()));
            copySubTasks.add(copySubTask);
        }
        return copySubTasks;
    }

    private static List<TaskDependency> cloneDependencies(List<TaskDependency> dependencies) {
        List<TaskDependency> cloned = new ArrayList<>();
        if (dependencies == null) {
            return cloned;
        }
        for (TaskDependency dependency : dependencies) {
            if (dependency == null) {
                continue;
            }
            cloned.add(new TaskDependency(
                    dependency.getPredecessorPosition(),
                    dependency.getType(),
                    dependency.getLagDays(),
                    dependency.isCritical()
            ));
        }
        return cloned;
    }

    private static LocalDate projectStart(List<Task> tasks) {
        LocalDate start = null;
        for (Task task : tasks) {
            start = minDateRecursive(task, start, true);
        }
        return start;
    }

    private static LocalDate projectEnd(List<Task> tasks) {
        LocalDate end = null;
        for (Task task : tasks) {
            end = maxDateRecursive(task, end, true);
        }
        return end;
    }

    private static String findFinalMilestoneLabel(List<Task> tasks, LocalDate baselineEnd) {
        if (baselineEnd == null) {
            return "Финальная веха";
        }
        for (Task task : tasks) {
            String label = findFinalMilestoneRecursive(task, baselineEnd);
            if (label != null) {
                return label;
            }
        }
        return "Финальная веха";
    }

    private static String findFinalMilestoneRecursive(Task task, LocalDate baselineEnd) {
        if (task == null) {
            return null;
        }
        if (baselineEnd.equals(task.getEnd())) {
            return safe(task.getName());
        }
        for (SubTask subTask : task.getObservableSubTasks()) {
            if (baselineEnd.equals(subTask.getEnd())) {
                return safe(subTask.getName());
            }
        }
        for (Task child : task.getObservableChildTasks()) {
            String label = findFinalMilestoneRecursive(child, baselineEnd);
            if (label != null) {
                return label;
            }
        }
        return null;
    }

    private static double budgetForNode(List<BudgetItem> budgetItems, String key, boolean planned) {
        if (budgetItems == null || key == null || key.isBlank()) {
            return 0.0;
        }
        double amount = 0.0;
        for (BudgetItem item : budgetItems) {
            if (item == null || item.getTaskPosition() == null || item.getTaskPosition().isBlank()) {
                continue;
            }
            String position = item.getTaskPosition().trim();
            if (position.equals(key) || key.startsWith(position + ".") || position.startsWith(key + ".")) {
                amount += planned ? item.getPlannedAmount() : item.getActualAmount();
            }
        }
        return amount;
    }

    private static int assignedResourcesForNode(List<Task> tasks, String key) {
        Task task = findTaskByKey(tasks, key);
        return task == null ? 0 : task.getResourceAssignments().size();
    }

    private static boolean nodeHasResourceOverload(List<Task> tasks, String key) {
        Task task = findTaskByKey(tasks, key);
        return task != null && task.isResourceOverloaded();
    }

    private static Task findTaskByKey(List<Task> tasks, String key) {
        if (tasks == null || key == null) {
            return null;
        }
        for (Task task : tasks) {
            Task match = findTaskRecursive(task, key);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static Task findTaskRecursive(Task task, String key) {
        if (task == null) {
            return null;
        }
        if (task.getPosition() != null && key.equals(task.getPosition().trim())) {
            return task;
        }
        for (Task child : task.getObservableChildTasks()) {
            Task match = findTaskRecursive(child, key);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static LocalDate minDateRecursive(Task task, LocalDate current, boolean includeChildren) {
        if (task == null) {
            return current;
        }
        LocalDate result = minDate(current, task.getStart());
        for (SubTask subTask : task.getObservableSubTasks()) {
            result = minDate(result, subTask.getStart());
        }
        if (includeChildren) {
            for (Task child : task.getObservableChildTasks()) {
                result = minDateRecursive(child, result, true);
            }
        }
        return result;
    }

    private static LocalDate maxDateRecursive(Task task, LocalDate current, boolean includeChildren) {
        if (task == null) {
            return current;
        }
        LocalDate result = maxDate(current, task.getEnd());
        for (SubTask subTask : task.getObservableSubTasks()) {
            result = maxDate(result, subTask.getEnd());
        }
        if (includeChildren) {
            for (Task child : task.getObservableChildTasks()) {
                result = maxDateRecursive(child, result, true);
            }
        }
        return result;
    }

    private static String capitalizeMonth(YearMonth month) {
        String text = month.format(MONTH_FORMAT);
        if (text.isBlank()) {
            return "—";
        }
        return text.substring(0, 1).toUpperCase(new Locale("ru")) + text.substring(1);
    }

    private static LocalDate minDate(LocalDate current, LocalDate candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isBefore(current) ? candidate : current;
    }

    private static LocalDate maxDate(LocalDate current, LocalDate candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isAfter(current) ? candidate : current;
    }

    private static long durationInclusive(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Без названия" : value.trim();
    }

    private static String safeAssignee(String value) {
        return value == null || value.isBlank() ? "Команда проекта" : value.trim();
    }
}
