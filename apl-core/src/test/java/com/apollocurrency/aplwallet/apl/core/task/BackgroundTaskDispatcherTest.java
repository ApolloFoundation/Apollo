package com.apollocurrency.aplwallet.apl.core.task;

import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
class BackgroundTaskDispatcherTest {

    @Mock
    private PropertiesHolder propertiesHolder;

    private TaskDispatchManager manager;
    private TaskDispatcher taskDispatcher;

    private int[] count=new int[3];

    @BeforeEach
    void setUp() {
        //doReturn(10).when(propertiesHolder).getIntProperty(anyString(), anyInt());
        doReturn(false).when(propertiesHolder).getBooleanProperty(anyString(), anyBoolean());

        manager = new TaskDispatchManager(propertiesHolder);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void scheduleAtFixedRate() {
        taskDispatcher = manager.newScheduledDispatcher("TestHealthCore");
        count[0] = 10;
        Task task = Task.builder()
                .name("healthMonitor")
                .recurring(true)
                .delay(100)
                .initialDelay(100)
                .daemon(true)
                .task(() -> {
                    decrementCount();
                    log.info(getNodeHealth());
                })
                .build();

        taskDispatcher.schedule(task);

        taskDispatcher.dispatch();
        log.info("Thread dispatch");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        log.info("Count = {}", count[0]);

        assertTrue(count[0]<3);
    }

    private void decrementCount(){
        count[0]--;
    }

    private void decrementCount(int idx){
        count[idx]--;
    }

    private int getCount(){
        return count[0];
    }

    private int getCount(int idx){
        return count[idx];
    }
    @Test
    void scheduleBeforeAndAfterScheduleTasks() {
        taskDispatcher = manager.newBackgroundDispatcher("TestTransactionService");
        count[0] = 10;
        count[1] = 10;
        Task task0 = Task.builder()
                .name("task-INIT1")
                .task(()-> {decrementCount(); log.info("task-body: INIT task running");})
                .initialDelay(0)
                .build();

        Task task1 = Task.builder()
                .name("task-BEFORE1")
                .task(()-> {decrementCount(); log.info("task-body: BEFORE task 1 running");})
                .initialDelay(10)
                .build();
        Task task12 = Task.builder()
                .name("task-BEFORE12")
                .task(()-> {decrementCount(); log.info("task-body: BEFORE task 12 running");})
                .initialDelay(20)
                .build();
        Task task2 = Task.builder()
                .name("task-AFTER1")
                .task(()-> {decrementCount();log.info("task-body: AFTER task 1 running");})
                .delay(10)
                .build();
        Task task22 = Task.builder()
                .name("task-AFTER12")
                .task(()-> {decrementCount();log.info("task-body: AFTER task 2 running");})
                .delay(20)
                .build();
        Task taskMain = Task.builder()
                .name("MainTask-sleep-main")
                .task(()->{
                    for (;;){
                        assertTrue(getCount()<=7);
                        log.info("task-body: MAIN task running, count= {}", getCount());
                        decrementCount(1);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                })
                .delay(20)
                .build();

        taskDispatcher.schedule(taskMain, TaskOrder.TASK);
        taskDispatcher.schedule(task0, TaskOrder.INIT);
        taskDispatcher.schedule(task1, TaskOrder.BEFORE);
        taskDispatcher.schedule(task12, TaskOrder.BEFORE);
        taskDispatcher.schedule(task2, TaskOrder.AFTER);
        taskDispatcher.schedule(task22, TaskOrder.AFTER);

        taskDispatcher.dispatch();
        log.info("Thread dispatch");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        assertTrue(getCount(1)<2, "Exception was occurred in the Main task.");
        assertTrue(getCount()<6);

    }

    private static String getNodeHealth() {
        StringBuilder sb = new StringBuilder();
        sb.append("ThreadGroup Name: ").append(Thread.currentThread().getThreadGroup().getName());
        sb.append(" Active threads count :").append(Thread.activeCount());
        return sb.toString();
    }
}