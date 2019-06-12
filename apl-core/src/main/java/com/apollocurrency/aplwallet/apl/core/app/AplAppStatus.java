/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

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

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import org.slf4j.Logger;

/**
 *
 * @author alukin@gmail.com
 */
@Singleton
public class AplAppStatus {
    private static final Logger LOG = getLogger(AplAppStatus.class);

    private static final long ONE_DAY=3600*24;
    private final Map<String,DurableTaskInfo> tasks = new HashMap<>();
    private NumberFormat formatter = new DecimalFormat("#000.000");
    
    @Inject
    public AplAppStatus() {
    }
    
    public String durableTaskStart(String name, String description, boolean isCritical) {
        return this.durableTaskStart(name, description, isCritical, 0.0);
    }

    public String durableTaskStart(String name, String description, boolean isCritical, Double percentComplete) {
        Objects.requireNonNull(name, "task Name is NULL");
        StringValidator.requireNonBlank(name, "task Name is empty");
        Objects.requireNonNull(description, "task description is NULL");
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
        if(isCritical){
            LOG.info("Task: '{}' started",name);
        }else{
            LOG.debug("Task: '{}' started",name);
        }
        return key;
    }

    public String durableTaskUpdate(String taskId, Double percentComplete) {
        return durableTaskUpdate(taskId, percentComplete, null,-1);
    }
    
    public String durableTaskUpdate(String taskId, Double percentComplete, String message){
        return durableTaskUpdate(taskId,percentComplete,message,-1);
    }

    public synchronized String durableTaskUpdate(String taskId, Double percentComplete, String message, int keepPrevMessages){
        Objects.requireNonNull(percentComplete, "percentComplete is NULL");

       DurableTaskInfo info =  tasks.get(taskId);
       if(info==null){
           taskId=durableTaskStart("Unnamed", "No Description", false);
           info=tasks.get(taskId);
       }
       info.setStateOfTask(DurableTaskInfo.TASK_STATES[1]);
       info.setPercentComplete(percentComplete);
       info.setDurationMS(System.currentTimeMillis()-info.getStarted().getTime());
       if(!StringUtils.isBlank(message)){
           info.getMessages().add(message);
       }
       if(info.isCrititcal){
          LOG.info("{}: {}%, message: {}",info.name, formatter.format(percentComplete), message);           
       }else{
          LOG.debug("{}: {}%, message: {}, duration: {}",info.name, formatter.format(percentComplete), message,info.durationMS);
       }
       if(keepPrevMessages>0){
           int toDel = info.messages.size()-keepPrevMessages;
           if(toDel>0){
             for(int i=0;i<toDel;i++){  
               info.messages.remove(0);
             }
           }
       }
       return taskId;
    }

    public synchronized Double increaseTaskCompletenessByPercent(String taskId, Double percentComplete) {
        Objects.requireNonNull(percentComplete, "percentComplete is NULL");
        DurableTaskInfo info =  tasks.get(taskId);
        if(info != null) {
            info.percentComplete += percentComplete;
            LOG.trace("Task '{}' new percent value = '{}'", taskId, formatter.format(info.percentComplete));
            return info.percentComplete;
        }
        return Double.NaN;
    }

    public synchronized void durableTaskPaused(String taskId, String message){
        DurableTaskInfo info =  tasks.get(taskId);
        if(info==null){
            return;
        }
        info.setStateOfTask(DurableTaskInfo.TASK_STATES[4]);
        LOG.debug("'{}': paused, %: {}, message: {}",info.name,  message);
        if(!StringUtils.isBlank(message)){
           info.getMessages().add(message);
        }        
    }

    public synchronized void durableTaskContinue(String taskId, String message){
        DurableTaskInfo info =  tasks.get(taskId);
        if(info==null){
            return;
        }     
        info.setStateOfTask(DurableTaskInfo.TASK_STATES[1]);
        LOG.debug("'{}': resumed, %: {}, message: {}",info.name,  message);
        if(!StringUtils.isBlank(message)){
           info.getMessages().add(message);
        }        
    }

    public synchronized void durableTaskFinished(String taskId, boolean isCancelled, String message){
        DurableTaskInfo info =  tasks.get(taskId);
        if(info==null){
            return;
        }
        info.setFinished(new Date());
        info.setDurationMS(info.getFinished().getTime()-info.getStarted().getTime());
        if(!StringUtils.isBlank(message)){
           info.getMessages().add(message);
        }
        if(info.isCrititcal){
           LOG.debug("'{}': finished with: {} duration, MS: {}",info.name,message,info.durationMS);
        }else{        
           LOG.info("'{}': finished with: {} duration, MS: {}",info.name,message,info.durationMS);
        }
        if(isCancelled){
            info.setStateOfTask(DurableTaskInfo.TASK_STATES[3]);
        }else{
            info.setStateOfTask(DurableTaskInfo.TASK_STATES[2]);            
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

    public synchronized Optional<DurableTaskInfo> findTaskByName(String taskName) {
        Objects.requireNonNull(taskName, "taskName is NULL");
        for(DurableTaskInfo taskInfo: tasks.values()){
            if (taskInfo.getName() != null && !taskInfo.getName().isEmpty() && taskInfo.getName().contains(taskName)) {
                return Optional.of(taskInfo);
            }
        }
        return Optional.empty();
    }

}
