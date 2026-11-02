package io.angistrem.employeemanagement.service;

public class MainControllerUiSyncService {

    public void syncAfterExpansionChange(
            Runnable updateVisibleItemsForGantt,
            Runnable updateGanttHeight,
            Runnable refreshTree,
            Runnable refreshGantt
    ) {
        run(updateVisibleItemsForGantt);
        run(updateGanttHeight);
        run(refreshTree);
        run(refreshGantt);
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
