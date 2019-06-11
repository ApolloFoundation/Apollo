package com.apollocurrency.aplwallet.apl.core.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AplAppStatusTest {

    private AplAppStatus status;

    @BeforeEach
    void setUp() {
        status = new AplAppStatus();
    }

    @Test
    void durableTaskStart() {
        String taskId = status.durableTaskStart("name1", "descr1", true);
        assertNotNull(taskId);
        taskId = status.durableTaskStart("name1", "descr1", true, 100.0);
        assertNotNull(taskId);
    }

    @Test
    void durableTaskUpdate() {
        String taskId = status.durableTaskStart("name1", "descr1", true, 10.0);
        assertNotNull(taskId);
        String result = status.durableTaskUpdate(taskId, 50.0, "new msg");
        assertEquals(taskId, result);
    }

    @Test
    void getTasksList() {
        String taskId = status.durableTaskStart("name1", "descr1", true);
        assertNotNull(taskId);
        taskId = status.durableTaskStart("name1", "descr1", true);
        assertNotNull(taskId);
        taskId = status.durableTaskStart("name1", "descr1", false);
        assertNotNull(taskId);
        assertEquals(3, status.getTasksList().size());
    }

    @Test
    void findTaskByName() {
        String taskId = status.durableTaskStart("sharding", "descr1", true);
        assertNotNull(taskId);
        Optional<DurableTaskInfo> taskInfo = status.findTaskByName("sharding");
        assertNotNull(taskInfo);
        assertTrue(taskInfo.isPresent());
        assertEquals("sharding", taskInfo.get().getName());
    }
}