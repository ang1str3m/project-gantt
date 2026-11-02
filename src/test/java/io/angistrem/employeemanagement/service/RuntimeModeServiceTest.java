package io.angistrem.employeemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeModeServiceTest {

    private final RuntimeModeService service = new RuntimeModeService();

    @Test
    void isRunningFromIdeShouldDetectByIdeaProperty() {
        assertTrue(service.isRunningFromIde("", "true", ""));
    }

    @Test
    void isRunningFromIdeShouldDetectByClasspathOrCommand() {
        assertTrue(service.isRunningFromIde("C:\\lib\\idea_rt.jar", "", ""));
        assertTrue(service.isRunningFromIde("", "", "com.intellij.rt.execution.application.AppMainV2"));
    }

    @Test
    void isRunningFromIdeShouldReturnFalseForRegularRuntime() {
        assertFalse(service.isRunningFromIde("C:\\app\\gantrem.jar", "", "io.angistrem.employeemanagement.App"));
    }
}
