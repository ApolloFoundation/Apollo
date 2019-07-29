package com.apollocurrency.aplwallet.apl.util.task;

public class BackgroundTaskDispatcher implements TaskDispatcher {

    @Override
    public boolean schedule(Task task, TaskOrder order) {
        return false;
    }

    @Override
    public boolean cancel(String name) {
        return false;
    }

    @Override
    public boolean cancelAll() {
        return false;
    }

    @Override
    public void shutdown() {

    }
}
