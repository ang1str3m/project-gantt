package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainControllerUiSyncServiceTest {

    private final MainControllerUiSyncService service = new MainControllerUiSyncService();

    @Test
    void syncAfterExpansionChangeShouldRunActionsInOrderAndSkipNulls() {
        List<String> steps = new ArrayList<>();

        service.syncAfterExpansionChange(
                () -> steps.add("visible"),
                () -> steps.add("height"),
                null,
                () -> steps.add("gantt")
        );

        assertEquals(List.of("visible", "height", "gantt"), steps);
    }
}
