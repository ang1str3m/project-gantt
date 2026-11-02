package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DependencySpecBuildService {

    public record DependencyRenderSpec(
            Object target,
            String predecessorKey,
            String targetKey,
            Rectangle predecessorBar,
            Rectangle targetBar,
            String type,
            int lagDays,
            boolean critical,
            TaskDependency dependency,
            int connectionIndex,
            boolean fromRight,
            boolean toRight,
            double sourceOffsetY,
            double targetOffsetY
    ) {
    }

    public List<DependencyRenderSpec> buildNormalizedSpecs(Map<String, Rectangle> taskBars,
                                                           Map<String, Object> itemsByKey,
                                                           Iterable<Task> tasks) {
        if (taskBars == null || taskBars.isEmpty() || tasks == null) {
            return List.of();
        }

        Map<String, Object> byPosition = new LinkedHashMap<>();
        for (Task task : tasks) {
            registerTaskPositions(task, byPosition);
        }

        List<DependencyRenderSpec> renderSpecs = new ArrayList<>();
        Map<String, Integer> edgeTotals = new HashMap<>();
        int connectionIndex = 0;

        for (Map.Entry<String, Rectangle> entry : taskBars.entrySet()) {
            Object target = itemsByKey.get(entry.getKey());
            if (target == null) {
                continue;
            }
            Rectangle targetBar = entry.getValue();
            for (TaskDependency dependency : getDependenciesForItem(target)) {
                String predecessorKey = normalizedPosition(dependency != null ? dependency.getPredecessorPosition() : null);
                if (predecessorKey == null) {
                    continue;
                }
                Object predecessor = byPosition.get(predecessorKey);
                if (predecessor == null) {
                    continue;
                }
                Rectangle predecessorBar = taskBars.get(predecessorKey);
                if (predecessorBar == null) {
                    continue;
                }

                int currentIndex = connectionIndex++;
                boolean critical = dependency.isCritical() || (target instanceof Task taskTarget && taskTarget.isCriticalDependency());
                String dependencyType = dependency.getType() == null ? "FS" : dependency.getType().trim().toUpperCase(Locale.ROOT);
                boolean fromRight = "FS".equals(dependencyType) || "FF".equals(dependencyType);
                boolean toRight = "FF".equals(dependencyType) || "SF".equals(dependencyType);
                String sourceEdgeKey = predecessorKey + "|" + (fromRight ? "R" : "L");
                String targetEdgeKey = entry.getKey() + "|" + (toRight ? "R" : "L");
                edgeTotals.merge(sourceEdgeKey, 1, Integer::sum);
                edgeTotals.merge(targetEdgeKey, 1, Integer::sum);

                renderSpecs.add(new DependencyRenderSpec(
                        target,
                        predecessorKey,
                        entry.getKey(),
                        predecessorBar,
                        targetBar,
                        dependencyType,
                        dependency.getLagDays(),
                        critical,
                        dependency,
                        currentIndex,
                        fromRight,
                        toRight,
                        0.0,
                        0.0
                ));
            }
        }

        Map<String, Integer> edgeSlots = new HashMap<>();
        List<DependencyRenderSpec> normalizedSpecs = new ArrayList<>(renderSpecs.size());
        for (DependencyRenderSpec spec : renderSpecs) {
            String sourceEdgeKey = spec.predecessorKey() + "|" + (spec.fromRight() ? "R" : "L");
            String targetEdgeKey = spec.targetKey() + "|" + (spec.toRight() ? "R" : "L");
            int sourceIndex = edgeSlots.merge(sourceEdgeKey, 1, Integer::sum) - 1;
            int targetIndex = edgeSlots.merge(targetEdgeKey, 1, Integer::sum) - 1;
            int sourceTotal = edgeTotals.getOrDefault(sourceEdgeKey, 1);
            int targetTotal = edgeTotals.getOrDefault(targetEdgeKey, 1);
            double sourceOffsetY = computeEdgeSlotOffset(sourceTotal, sourceIndex, spec.predecessorBar());
            double targetOffsetY = computeEdgeSlotOffset(targetTotal, targetIndex, spec.targetBar());
            normalizedSpecs.add(new DependencyRenderSpec(
                    spec.target(),
                    spec.predecessorKey(),
                    spec.targetKey(),
                    spec.predecessorBar(),
                    spec.targetBar(),
                    spec.type(),
                    spec.lagDays(),
                    spec.critical(),
                    spec.dependency(),
                    spec.connectionIndex(),
                    spec.fromRight(),
                    spec.toRight(),
                    sourceOffsetY,
                    targetOffsetY
            ));
        }
        return normalizedSpecs;
    }

    private String normalizedPosition(String position) {
        if (position == null) {
            return null;
        }
        String trimmed = position.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void registerTaskPositions(Task task, Map<String, Object> byPosition) {
        if (task == null) {
            return;
        }
        if (task.getPosition() != null && !task.getPosition().isBlank()) {
            byPosition.put(task.getPosition(), task);
        }
        if (task.isGroup()) {
            for (Task childTask : task.getObservableChildTasks()) {
                registerTaskPositions(childTask, byPosition);
            }
            return;
        }
        for (SubTask subTask : task.getObservableSubTasks()) {
            if (subTask != null && subTask.getSubPosition() != null && !subTask.getSubPosition().isBlank()) {
                byPosition.put(subTask.getSubPosition(), subTask);
            }
        }
    }

    private List<TaskDependency> getDependenciesForItem(Object item) {
        if (item instanceof Task task) {
            return task.getDependencies();
        }
        if (item instanceof SubTask subTask) {
            return subTask.getDependencies();
        }
        return List.of();
    }

    private double computeEdgeSlotOffset(int total, int index, Rectangle bar) {
        if (total <= 1 || bar == null) {
            return 0.0;
        }
        double available = Math.max(6.0, bar.getHeight() - 6.0);
        double spacing = Math.min(6.0, available / Math.max(total - 1, 1));
        double center = (total - 1) / 2.0;
        double offset = (index - center) * spacing;
        double maxOffset = Math.max(2.0, (bar.getHeight() / 2.0) - 3.0);
        return Math.max(-maxOffset, Math.min(maxOffset, offset));
    }
}
