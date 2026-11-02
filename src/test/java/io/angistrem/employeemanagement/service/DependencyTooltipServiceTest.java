package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DependencyTooltipServiceTest {

    private final DependencyTooltipService service = new DependencyTooltipService();

    @Test
    void tooltipTextShouldNormalizeTypeAndFormatLag() {
        assertEquals("\u0417\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u044c: FS", service.tooltipText(null, 0));
        assertEquals("\u0417\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u044c: SS+2", service.tooltipText("ss", 2));
        assertEquals("\u0417\u0430\u0432\u0438\u0441\u0438\u043c\u043e\u0441\u0442\u044c: FF-1", service.tooltipText("ff", -1));
    }
}
