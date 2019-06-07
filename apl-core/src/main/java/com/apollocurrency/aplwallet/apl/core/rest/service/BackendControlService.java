/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.api.dto.NodeHWStatusInfo;
import com.apollocurrency.aplwallet.api.dto.RunningThreadsInfo;
import com.apollocurrency.aplwallet.api.dto.ThreadInfoDTO;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import lombok.Setter;

/**
 *
 * @author alukin@gmail.com
 */
@ApplicationScoped
public class BackendControlService {
    
    @Inject @Setter
    AplAppStatus appStatus;
    
    @Inject @Setter    
    PropertiesHolder ph;
    
    public NodeHWStatusInfo getHWStatus(){
        NodeHWStatusInfo res = new NodeHWStatusInfo();
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
    public boolean isAdminPasswordOK(String password){
        String pw = ph.getStringProperty("apl.adminPassword");
        boolean res = (!StringUtils.isBlank(pw)&& password!=null && pw.compareTo(password)==0);
        return res;
    }
}
