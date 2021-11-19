/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeParams;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import static com.apollocurrency.aplwallet.apl.util.Constants.HEALTH_CHECK_INTERVAL;
import static com.apollocurrency.aplwallet.apl.util.Constants.MEMPOOL_CHECK_INTERVAL;

/**
 * Simple periodical health checks
 *
 * @author Oleksiy Lukin alukin@gmail.com
 */
@Slf4j
@Singleton
public class AplHealthLogger {

    private final DatabaseManager databaseManager;
    private final MemPool memPool;
    private  PeersService peers;

    @Inject
    public AplHealthLogger(TaskDispatchManager taskDispatchManager, DatabaseManager databaseManager, AplAppStatus aplAppStatus, PeersService peers, MemPool memPool) {
        this.databaseManager = databaseManager;
        this.memPool = memPool;
        this.peers = peers;

        TaskDispatcher taskDispatcher = taskDispatchManager.newScheduledDispatcher("AplCoreRuntime-periodics");

       // checkInjects();
        taskDispatcher.schedule(Task.builder()
                .name("Core-health")
                .initialDelay(HEALTH_CHECK_INTERVAL * 2)
                .delay(HEALTH_CHECK_INTERVAL)
                .task(() -> {
                    log.info(getNodeHealth());
                    aplAppStatus.clearFinished(1 * 60L); //10 min
                })
                .build());

        taskDispatcher.schedule(Task.builder()
            .name("Core-MemPool")
            .initialDelay(MEMPOOL_CHECK_INTERVAL * 2)
            .delay(MEMPOOL_CHECK_INTERVAL)
            .task(this::printMemPoolStat)
            .build());
    }

    public  void logSystemProperties() {
        String[] loggedProperties = new String[]{
            "java.version",
            "java.vm.version",
            "java.vm.name",
            "java.vendor",
            "java.vm.vendor",
            "java.home",
            "java.library.path",
            "java.class.path",
            "os.arch",
            "sun.arch.data.model",
            "os.name",
            "file.encoding",
            "java.security.policy",
            "java.security.manager",
            RuntimeEnvironment.RUNTIME_MODE_ARG,};
        for (String property : loggedProperties) {
            log.debug("{} = {}", property, System.getProperty(property));
        }
        log.debug("availableProcessors = {}", Runtime.getRuntime().availableProcessors());
        log.debug("maxMemory = {}", Runtime.getRuntime().maxMemory());
        log.debug("processId = {}", RuntimeParams.getProcessId());
    }

    private void findDeadLocks(StringBuilder sb) {
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        long[] ids = tmx.findDeadlockedThreads();
        if (ids != null) {
            // threads that are in deadlock waiting to acquire object monitors or ownable synchronizers
            sb.append("DeadLocked threads found:\n");
            printDeadLockedThreadInfo(sb, tmx, ids);
        } else if (tmx.findMonitorDeadlockedThreads() != null) {
            //threads that are blocked waiting to enter a synchronization block or waiting to reenter a synchronization block
            sb.append("Monitor DeadLocked threads found:\n");
            printDeadLockedThreadInfo(sb, tmx, tmx.findMonitorDeadlockedThreads());
        } else {
            sb.append("\nNo dead-locked threads found.\n");
        }
    }

    private void printDeadLockedThreadInfo(StringBuilder sb, ThreadMXBean tmx, long[] ids) {
        ThreadInfo[] infos = tmx.getThreadInfo(ids, true, true);
        sb.append("Following Threads are deadlocked:\n");
        for (ThreadInfo info : infos) {
            sb.append(info.toString()).append("\n");
        }
    }

    private String getNodeHealth() {

        StringBuilder sb = new StringBuilder("Node health info\n");
        HikariPoolMXBean jmxBean = databaseManager.getDataSource().getJmxBean();
        String usedConnections = null;
        if (jmxBean != null) {
            int totalConnections = jmxBean.getTotalConnections();
            int activeConnections = jmxBean.getActiveConnections();
            int idleConnections = jmxBean.getIdleConnections();
            int threadAwaitingConnections = jmxBean.getThreadsAwaitingConnection();
            usedConnections = String.format("Total/Active/Idle connections in Pool '%d'/'%d'/'%d', threadsAwaitPool=[%d], 'main-db'",
                    totalConnections,
                    activeConnections,
                    idleConnections,
                    threadAwaitingConnections);
        }
        sb.append("Used DB connections: ").append(usedConnections);
        Runtime runtime = Runtime.getRuntime();
        sb.append("\nRuntime total memory :").append(String.format(" %,d KB", (runtime.totalMemory() / 1024)));
        sb.append("\nRuntime free  memory :").append(String.format(" %,d KB", (runtime.freeMemory() / 1024)));
        sb.append("\nRuntime max   memory :").append(String.format(" %,d KB", (runtime.maxMemory() / 1024)));
        sb.append("\nActive threads count :").append(Thread.currentThread().getThreadGroup().getParent().activeCount());
        sb.append("\nInbound peers count: ").append(peers.getInboundPeers().size());
        sb.append(", Active peers count: ").append(peers.getActivePeers().size());
        sb.append(", Known peers count: ").append(peers.getAllPeers().size());
        sb.append(", Connectable peers count: ").append(peers.getAllConnectablePeers().size());
        findDeadLocks(sb);
        return sb.toString();
    }

    private void printMemPoolStat() {
        StringBuilder sb = new StringBuilder();
        int memPoolSize = memPool.getSavedCount();
        int cacheSize = memPool.getCachedCount();

        if(memPoolSize > 0 ) {
            sb.append("MemPool Info:  ");
            sb.append("Txs: ").append(memPoolSize).append(", ");
            sb.append("Cache size: ").append(cacheSize).append(", ");
            sb.append("Pending processing: ").append(memPool.processingQueueSize()).append(", ");
            sb.append("Removed txs: ").append(memPool.getRemovedSize()).append(", ");
            sb.append("Process Later Queue: ").append(memPool.getProcessLaterCount()).append(", ");
            sb.append("Referenced: ").append(memPool.getReferencedCount());

            log.info(sb.toString());
        }
    }

}
