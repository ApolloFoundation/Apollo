/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Long-running task info and other node status information
 *
 * @author alukin@gmail.com
 */
@Singleton
public class AplAppStatus {

    private static final Logger LOG = getLogger(AplAppStatus.class);

    private static final long ONE_DAY = 3600 * 24;
    private final Map<String, DurableTaskInfo> tasks = new HashMap<>();
    private NumberFormat formatter = new DecimalFormat("#000.000");

    @Inject
    public AplAppStatus() {
    }


    /**
     * Create new long running task indicator and get handle to it
     *
     * @param name        Short name of the task
     * @param description Longer description of the task
     * @param isCritical  If task is critical it will be displayed on top of
     *                    GUI, if not it will be just in list of backend tasks accessible by menu
     * @return handle to newly created task indicator
     */

    public String durableTaskStart(String name, String description, boolean isCritical) {
        return this.durableTaskStart(name, description, isCritical, 0.0);
    }

    public String durableTaskStart(String name, String description, boolean isCritical, Double percentComplete) {
        StringValidator.requireNonBlank(name, "task Name is empty");
        StringValidator.requireNonBlank(description, "task description is empty");
        Objects.requireNonNull(percentComplete, "percent Complete is NULL");

        String key = UUID.randomUUID().toString();
        DurableTaskInfo info = new DurableTaskInfo();
        info.setId(key);
        info.setName(name);
        info.setPercentComplete(percentComplete);
        info.setDecription(description);
        info.setStarted(new Date());
        info.setStateOfTask(DurableTaskInfo.TASK_STATES[0]);
        info.setIsCrititcal(isCritical);
        tasks.put(key, info);

        if (isCritical) {
            LOG.info("Task: '{}' started", name);
        } else {
            LOG.debug("Task: '{}' started", name);
        }
        return key;
    }

    /**
     * Update status of long running task
     *
     * @param taskId          task handle
     * @param percentComplete percent of task completion
     * @param message         optional message
     * @return handle to the task
     */
    public synchronized String durableTaskUpdate(String taskId, Double percentComplete, String message) {
        return durableTaskUpdate(taskId, percentComplete, message, -1);
    }

    /**
     * Update status of long running task
     *
     * @param taskId           task handle
     * @param percentComplete  percent of task completion
     * @param message          optional message
     * @param keepPrevMessages how many previous messages to keep
     * @return handle to the task
     */
    public synchronized String durableTaskUpdate(String taskId, Double percentComplete, String message, int keepPrevMessages) {

        DurableTaskInfo info = tasks.get(taskId);
        if (info == null) {
            taskId = durableTaskStart("Unnamed", "No Description", false);
            info = tasks.get(taskId);
        }
        info.setStateOfTask(DurableTaskInfo.TASK_STATES[1]);
        info.setPercentComplete(percentComplete);
        info.setDurationMS(System.currentTimeMillis() - info.getStarted().getTime());
        if (!StringUtils.isBlank(message)) {
            info.getMessages().add(message);
        }
        if (info.isCrititcal) {
            LOG.info("{}: {}%, message: {}", info.name, formatter.format(percentComplete), message);
        } else {
            LOG.debug("{}: {}%, message: {}, duration: {}", info.name, formatter.format(percentComplete), message, info.durationMS);
        }
        if (keepPrevMessages > 0) {
            int toDel = info.messages.size() - keepPrevMessages;
            if (toDel > 0) {
                for (int i = 0; i < toDel; i++) {
                    info.messages.remove(0);
                }
            }
        }
        return taskId;
    }

    public synchronized void durableTaskUpdate(String taskId, Double percentComplete, String message, int keepPrevMessages, Double offset) {
        if (offset != null && percentComplete != null) {
            throw new IllegalArgumentException("Only one parameter among [offset,percentComplete] should be supplied");
        }
        DurableTaskInfo info = tasks.get(taskId);
        if (info == null) {
            taskId = durableTaskStart("Unnamed", "No Description", false);
            info = tasks.get(taskId);
        }
        info.setStateOfTask(DurableTaskInfo.TASK_STATES[1]);
        if (offset != null) {
            info.setPercentComplete(info.getPercentComplete() + offset);

        } else {
            info.setPercentComplete(percentComplete);
        }
        info.setDurationMS(System.currentTimeMillis() - info.getStarted().getTime());
        if (!StringUtils.isBlank(message)) {
            info.getMessages().add(message);
        }
        if (info.isCrititcal) {
            LOG.info("{}: update,%: {}, message: {}", info.name, formatter.format(info.percentComplete), message);
        } else {
            LOG.debug("{}: update,%: {}, message: {}, duration: {}", info.name, formatter.format(info.percentComplete), message, info.durationMS);
        }
        if (keepPrevMessages > 0) {
            int toDel = info.messages.size() - keepPrevMessages;
            if (toDel > 0) {
                for (int i = 0; i < toDel; i++) {
                    info.messages.remove(0);
                }
            }
        }
    }

    public synchronized double durableTaskUpdate(String taskId, String message, double addPercents) {
        double res = 0.0;
        durableTaskUpdate(taskId, null, message, -1, addPercents);
        DurableTaskInfo ti = tasks.get(taskId);
        if (ti != null) {
            res = ti.getPercentComplete();
        }
        return res;
    }

    /**
     * Indicate that long running task is paused
     *
     * @param taskId  task handle
     * @param message optional message
     */
    public synchronized void durableTaskPaused(String taskId, String message) {
        DurableTaskInfo info = tasks.get(taskId);
        if (info == null) {
            return;
        }
        info.setStateOfTask(DurableTaskInfo.TASK_STATES[4]);
        LOG.debug("{}: paused, %: {}, message: {}", info.name, info.percentComplete, message);
        if (!StringUtils.isBlank(message)) {
            info.getMessages().add(message);
        }
    }

    /**
     * Indicate that previously paused long running task is running again
     *
     * @param taskId  task handle
     * @param message optional message
     */
    public synchronized void durableTaskContinue(String taskId, String message) {
        DurableTaskInfo info = tasks.get(taskId);
        if (info == null) {
            return;
        }
        info.setStateOfTask(DurableTaskInfo.TASK_STATES[1]);
        LOG.debug("{}: resumed, %: {}, message: {}", info.name, info.percentComplete, message);
        if (!StringUtils.isBlank(message)) {
            info.getMessages().add(message);
        }
    }

    /**
     * Indicate that long running task is finished
     *
     * @param taskId      task handle
     * @param isCancelled if false task is finished normally. If true, task is
     *                    cancelled
     * @param message     optional message
     */
    public synchronized void durableTaskFinished(String taskId, boolean isCancelled, String message) {
        DurableTaskInfo info = tasks.get(taskId);
        if (info == null) {
            return;
        }
        info.setFinished(new Date());
        info.setDurationMS(info.getFinished().getTime() - info.getStarted().getTime());
        if (!StringUtils.isBlank(message)) {
            info.getMessages().add(message);
        }
        if (info.isCrititcal) {
            LOG.debug("{}: finished with: {} duration, MS: {}", info.name, message, info.durationMS);
        } else {
            LOG.info("{}: finished with: {} duration, MS: {}", info.name, message, info.durationMS);
        }
        if (isCancelled) {
            info.setStateOfTask(DurableTaskInfo.TASK_STATES[3]);
        } else {
            info.setStateOfTask(DurableTaskInfo.TASK_STATES[2]);
        }
    }

    /**
     * Clear finished some time ago tasks
     *
     * @param secondsAgo how many seconds ago tasks are finished
     */
    public synchronized void clearFinished(Long secondsAgo) {
        if (secondsAgo == null || secondsAgo < 0) {
            secondsAgo = ONE_DAY;
        }
        List<String> ids = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (DurableTaskInfo ti : tasks.values()) {
            if (ti.getFinished() != null && ti.getFinished().getTime() + secondsAgo * 1000 <= now) {
                ids.add(ti.getId());
            }
        }
        ids.forEach(tasks::remove);
    }

    /**
     * get list of all long running tasks
     *
     * @return collection of long running tasks not cleaned yet
     */
    public Collection<DurableTaskInfo> getTasksList() {
        return tasks.values();
    }

    public synchronized Optional<DurableTaskInfo> findTaskByName(String taskName) {
        Objects.requireNonNull(taskName, "taskName is NULL");
        for (DurableTaskInfo taskInfo : tasks.values()) {
            if (taskInfo.getName() != null && !taskInfo.getName().isEmpty() && taskInfo.getName().contains(taskName)) {
                return Optional.of(taskInfo);
            }
        }
        return Optional.empty();
    }

    public double durableTaskUpdateAddPercents(String taskId, Double percentIncreaseValue) {
        Objects.requireNonNull(percentIncreaseValue, "percentComplete is NULL");
        DurableTaskInfo info = tasks.get(taskId);
        if (info != null) {
            info.percentComplete += percentIncreaseValue;
            LOG.trace("Task '{}' new percent value = '{}'", taskId, formatter.format(info.percentComplete));
            return info.percentComplete;
        }
        return Double.NaN;
    }

}
