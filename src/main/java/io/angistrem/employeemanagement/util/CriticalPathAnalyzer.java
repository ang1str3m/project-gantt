package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CriticalPathAnalyzer {

    private CriticalPathAnalyzer() {
    }

    public record PathStep(
            String key,
            String label,
            LocalDate start,
            LocalDate end,
            String incomingDependency
    ) {
    }

    public record Analysis(
            List<PathStep> steps,
            LocalDate start,
            LocalDate end,
            long totalDays
    ) {
        public boolean isMeaningful() {
            return steps != null && steps.size() > 1 && start != null && end != null && totalDays > 0;
        }
    }

    private record NodeData(String key, String label, LocalDate start, LocalDate end, Object source) {
    }

    private record PathState(LocalDate start, List<PathStep> steps, long totalDays) {
    }

    public static Analysis analyze(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }

        Map<String, NodeData> nodes = new LinkedHashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (Task task : tasks) {
            registerTask(nodes, predecessors, inDegree, task);
            for (SubTask subTask : task.getObservableSubTasks()) {
                registerSubTask(nodes, predecessors, inDegree, subTask);
            }
        }

        if (nodes.isEmpty()) {
            return null;
        }

        for (NodeData node : nodes.values()) {
            for (TaskDependency dependency : dependenciesOf(node.source())) {
                if (dependency == null || dependency.getPredecessorPosition() == null) {
                    continue;
                }
                String predecessorKey = dependency.getPredecessorPosition().trim();
                if (!nodes.containsKey(predecessorKey) || predecessorKey.equals(node.key())) {
                    continue;
                }
                predecessors.computeIfAbsent(node.key(), ignored -> new ArrayList<>()).add(predecessorKey);
                inDegree.put(node.key(), inDegree.getOrDefault(node.key(), 0) + 1);
            }
        }

        List<String> topoOrder = topologicalOrder(nodes, predecessors, inDegree);
        Map<String, PathState> bestStates = new HashMap<>();
        Analysis bestAnalysis = null;

        for (String key : topoOrder) {
            NodeData node = nodes.get(key);
            if (node == null || node.start() == null || node.end() == null) {
                continue;
            }

            List<PathStep> standalone = new ArrayList<>();
            standalone.add(new PathStep(node.key(), node.label(), node.start(), node.end(), null));
            PathState bestForNode = new PathState(node.start(), standalone, durationInclusive(node.start(), node.end()));

            for (String predecessorKey : predecessors.getOrDefault(key, List.of())) {
                PathState predecessorState = bestStates.get(predecessorKey);
                if (predecessorState == null || predecessorState.start() == null) {
                    continue;
                }

                String incomingDependency = dependencyLabel(node.source(), predecessorKey);
                List<PathStep> extendedSteps = new ArrayList<>(predecessorState.steps());
                extendedSteps.add(new PathStep(node.key(), node.label(), node.start(), node.end(), incomingDependency));

                long candidateTotal = durationInclusive(predecessorState.start(), node.end());
                if (candidateTotal > bestForNode.totalDays()) {
                    bestForNode = new PathState(predecessorState.start(), extendedSteps, candidateTotal);
                }
            }

            bestStates.put(key, bestForNode);
            Analysis candidate = new Analysis(bestForNode.steps(), bestForNode.start(), node.end(), bestForNode.totalDays());
            if (isBetter(candidate, bestAnalysis)) {
                bestAnalysis = candidate;
            }
        }

        return bestAnalysis != null && bestAnalysis.isMeaningful() ? bestAnalysis : null;
    }

    private static void registerTask(
            Map<String, NodeData> nodes,
            Map<String, List<String>> predecessors,
            Map<String, Integer> inDegree,
            Task task
    ) {
        if (task == null || task.getPosition() == null) {
            return;
        }
        String key = task.getPosition().trim();
        nodes.put(key, new NodeData(key, safeLabel(task.getName()), task.getStart(), task.getEnd(), task));
        predecessors.putIfAbsent(key, new ArrayList<>());
        inDegree.putIfAbsent(key, 0);
    }

    private static void registerSubTask(
            Map<String, NodeData> nodes,
            Map<String, List<String>> predecessors,
            Map<String, Integer> inDegree,
            SubTask subTask
    ) {
        if (subTask == null || subTask.getSubPosition() == null) {
            return;
        }
        String key = subTask.getSubPosition().trim();
        nodes.put(key, new NodeData(key, safeLabel(subTask.getName()), subTask.getStart(), subTask.getEnd(), subTask));
        predecessors.putIfAbsent(key, new ArrayList<>());
        inDegree.putIfAbsent(key, 0);
    }

    private static List<String> topologicalOrder(
            Map<String, NodeData> nodes,
            Map<String, List<String>> predecessors,
            Map<String, Integer> inDegree
    ) {
        Map<String, List<String>> forward = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : predecessors.entrySet()) {
            for (String predecessor : entry.getValue()) {
                forward.computeIfAbsent(predecessor, ignored -> new ArrayList<>()).add(entry.getKey());
            }
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        nodes.keySet().stream()
                .filter(key -> inDegree.getOrDefault(key, 0) == 0)
                .sorted(Comparator.naturalOrder())
                .forEach(queue::add);

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            order.add(current);
            for (String next : forward.getOrDefault(current, List.of())) {
                int degree = inDegree.merge(next, -1, Integer::sum);
                if (degree == 0) {
                    queue.addLast(next);
                }
            }
        }

        if (order.size() != nodes.size()) {
            return new ArrayList<>(nodes.keySet());
        }
        return order;
    }

    private static boolean isBetter(Analysis candidate, Analysis current) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        if (candidate.totalDays() != current.totalDays()) {
            return candidate.totalDays() > current.totalDays();
        }
        return Objects.compare(candidate.end(), current.end(), Comparator.nullsLast(Comparator.naturalOrder())) > 0;
    }

    private static long durationInclusive(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
    }

    private static List<TaskDependency> dependenciesOf(Object source) {
        if (source instanceof Task task) {
            return task.getDependencies();
        }
        if (source instanceof SubTask subTask) {
            return subTask.getDependencies();
        }
        return List.of();
    }

    private static String dependencyLabel(Object source, String predecessorKey) {
        for (TaskDependency dependency : dependenciesOf(source)) {
            if (dependency == null || dependency.getPredecessorPosition() == null) {
                continue;
            }
            if (!predecessorKey.equals(dependency.getPredecessorPosition().trim())) {
                continue;
            }
            String type = dependency.getType() == null ? "FS" : dependency.getType().trim().toUpperCase();
            int lag = dependency.getLagDays();
            return lag == 0 ? type : type + (lag > 0 ? "+" : "") + lag;
        }
        return "";
    }

    private static String safeLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Без названия";
        }
        return value.trim();
    }
}
