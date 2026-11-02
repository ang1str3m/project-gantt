package io.angistrem.employeemanagement.service;

import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialMarkerSpecServiceTest {

    private final FinancialMarkerSpecService service = new FinancialMarkerSpecService();

    @Test
    void buildSpecsShouldReturnAdvanceAndKsSpecs() {
        Task task = new Task("T", "C", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), TaskStatus.PLANNED, "M");
        task.setContractAdvanceDate(LocalDate.of(2026, 4, 5));
        task.setKsPaymentDate(LocalDate.of(2026, 4, 9));
        task.setRequireAdvanceBeforeStart(true);
        task.setRequireKsBeforeStart(false);

        List<FinancialMarkerSpecService.MarkerSpec> specs = service.buildSpecs(task);

        assertEquals(2, specs.size());
        assertEquals("ADVANCE", specs.get(0).code());
        assertEquals("KS", specs.get(1).code());
        assertEquals("#0f6adf", specs.get(0).colorHex());
        assertEquals("#d97706", specs.get(1).colorHex());
        assertTrue(specs.get(0).blocking());
    }
}
