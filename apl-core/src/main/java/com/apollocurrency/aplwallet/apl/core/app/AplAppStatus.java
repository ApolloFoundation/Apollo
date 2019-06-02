/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 * @author alukin@gmail.com
 */
@Singleton
public class AplAppStatus {
    public static final String[] TASK_STATES={"Starded","In progress","Finished","Cancelled"};
    private static final long ONE_DAY=3600*24;
    private Map<String,DurableTaskInfo> tasks = new HashMap<>();

    @Inject
    public AplAppStatus() {
    }
    
    public String durableTaskStart(String name, String descritption){
        String key = UUID.randomUUID().toString();
        DurableTaskInfo info = new DurableTaskInfo();
        info.setId(key);
        info.setPercentComplete(0.0D);
        info.setDecription(descritption);
        info.setStarted(new Date());
        info.setStateOfTask(TASK_STATES[0]);
        tasks.put(key, info);
        return key;
    }
    
    public synchronized String durableTaskUpdate(String taskId, String state, Double percentComplete, String message){
       DurableTaskInfo info =  tasks.get(taskId);
       if(info==null){
           taskId=UUID.randomUUID().toString();
           info=new DurableTaskInfo();
           tasks.put(taskId, info);
       }
       info.setStateOfTask(state);
       info.setPercentComplete(percentComplete);
       if(!StringUtils.isBlank(message)){
           info.getMessages().add(message);
       }
       info.setDurationMS(info.getFinished().getTime()-System.currentTimeMillis());
       return taskId;
    }
    
    public synchronized void durableTaksFinished(String taskId, boolean isCancelled){
        DurableTaskInfo info =  tasks.get(taskId);
        if(info==null){
            return;
        }
        info.setFinished(new Date());
        info.setDurationMS(info.getFinished().getTime()-info.getStarted().getTime());
        if(isCancelled){
            info.setStateOfTask(TASK_STATES[3]);
        }else{
            info.setStateOfTask(TASK_STATES[2]);            
        }
    }
    
    public synchronized void clearFinished(Long secondsAgo){
        if(secondsAgo==null || secondsAgo<0){
            secondsAgo=ONE_DAY;
        }
        List<String> ids = new ArrayList<>();
        long now = System.currentTimeMillis();
        for(DurableTaskInfo ti: tasks.values()){
            if(ti.getFinished().getTime()+secondsAgo*1000<=now){
                ids.add(ti.getId());
            }
        }
        ids.forEach((id) -> {
            tasks.remove(id);
        });
    }

    public Collection<DurableTaskInfo> getTasksList() {
       return tasks.values();
    }
}
