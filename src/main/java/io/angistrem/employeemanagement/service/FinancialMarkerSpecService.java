package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.Task;

import java.time.LocalDate;
import java.util.List;

public class FinancialMarkerSpecService {

    public record MarkerSpec(
            String code,
            LocalDate date,
            String shortLabel,
            String colorHex,
            String title,
            boolean blocking
    ) {
    }

    public List<MarkerSpec> buildSpecs(Task task) {
        if (task == null) {
            return List.of();
        }
        return List.of(
                new MarkerSpec(
                        "ADVANCE",
                        task.getContractAdvanceDate(),
                        "\u0410\u0412",
                        "#0f6adf",
                        "\u0410\u0432\u0430\u043d\u0441\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
                        task.isRequireAdvanceBeforeStart()
                ),
                new MarkerSpec(
                        "KS",
                        task.getKsPaymentDate(),
                        "\u041a\u0421",
                        "#d97706",
                        "\u041e\u043f\u043b\u0430\u0442\u0430 \u041a\u0421",
                        task.isRequireKsBeforeStart()
                )
        );
    }
}
