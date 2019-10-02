/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.api.dto.NodeHealthInfo;
import com.apollocurrency.aplwallet.api.dto.NodeNetworkingInfo;
import com.apollocurrency.aplwallet.api.dto.NodeStatusInfo;
import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.dto.RunningThreadsInfo;
import com.apollocurrency.aplwallet.api.dto.ThreadInfoDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;
import lombok.Setter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alukin@gmail.com
 */
@ApplicationScoped
public class BackendControlService {
    
    @Inject @Setter
    AplAppStatus appStatus;

    @Inject
    @Setter
    AdminPasswordVerifier apv;

    @Inject
    @Setter
    Blockchain blockchain;

    @Inject
    @Setter
    private Converter<Peer, PeerDTO> peerConverter;

    @Inject
    @Setter
    private Converter<PeerInfo, PeerDTO> peerInfoConverter;

    @Inject
    @Setter
    private NetworkService networkService;

    @Inject
    @Setter
    private BlockDao blockDao;

    @Inject
    @Setter
    private DatabaseManager databaseManager;

    public NodeStatusInfo getNodeStatus() {
        NodeStatusInfo res = new NodeStatusInfo();
        OperatingSystemMXBean mxbean =  ManagementFactory.getOperatingSystemMXBean();
        res.threadsRunning=java.lang.Thread.activeCount();
        res.cpuCount = Runtime.getRuntime().availableProcessors();
        res.cpuLoad = mxbean.getSystemLoadAverage();
        res.operatingSystem=mxbean.getName()+" Version:"+mxbean.getVersion()+" Arch:"+mxbean.getArch();
        res.memoryTotal = Runtime.getRuntime().totalMemory();
        res.memoryFree  = Runtime.getRuntime().freeMemory();    
        return res;
    } 

    public List<DurableTaskInfo> getNodeTasks(String state) {
        ArrayList<DurableTaskInfo> res;
        if(state.equalsIgnoreCase(DurableTaskInfo.TASK_STATES[5])){ //"All
            res = new ArrayList(appStatus.getTasksList());
        }else{
            res=new ArrayList<>();
            for(DurableTaskInfo dti: appStatus.getTasksList()){
                if(dti.stateOfTask.equalsIgnoreCase(state)){
                    res.add(dti);
                }
            }
        }
        return res;
    }
    
    public RunningThreadsInfo getThreadsInfo(){
       RunningThreadsInfo res = new  RunningThreadsInfo();
       ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
       long[]  tids = tmxb.getAllThreadIds();
       for(long tid:tids){
           ThreadInfoDTO tdto = new ThreadInfoDTO();
           ThreadInfo ti = tmxb.getThreadInfo(tid);
           tdto.name = ti.getThreadName();
           tdto.state=ti.getThreadState().toString();
           tdto.priority= ti.getPriority();
           tdto.isDaemon=ti.isDaemon();
           tdto.cpuTime=tmxb.getThreadCpuTime(tid);
           res.threads.add(tdto);
       }
       return res;
    }
    
   //TODO: use AdminPasswordVerifier component 
   public boolean isAdminPasswordOK(HttpServletRequest request) {
       boolean res = apv.checkPassword(request);
       return res;
   }

    public NodeHealthInfo getNodeHealth() {
        NodeHealthInfo info = new NodeHealthInfo();
        info.dbOK = chekDataBaseOK();
        info.blockchainHeight = blockchain.getHeight();
        info.usedDbConnections = databaseManager.getDataSource().getJmxBean().getActiveConnections();
        return info;
    }

    public NodeNetworkingInfo getNetworkingInfo() {
        NodeNetworkingInfo info = new NodeNetworkingInfo();
        info.inboundPeers = networkService.getInboundPeers().size();
        info.outboundPeers = networkService.getOutboundPeers().size();
        info.myPeerInfo = peerInfoConverter.convert(networkService.getMyPeerInfo());
        return info;
    }

    private boolean chekDataBaseOK() {
        Block b = blockDao.findLastBlock();
        boolean res = b != null;
        return res;
    }
}
