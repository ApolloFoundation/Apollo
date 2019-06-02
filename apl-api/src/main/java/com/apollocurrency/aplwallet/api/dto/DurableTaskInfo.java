/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of task that takes noticeable time
 * and should de displayed to user in UI
 * @author alukin@gmail.com
 */
@Schema(name="DurableTask", description="Information about running task")
@Getter @Setter @ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DurableTaskInfo {
    @Schema(name="ID of task", description="Identificator of task")
    String id;
    @Schema(name="Description of task", description="Description of task in one line")
    String decription;
    @Schema(name="Task state", description="Task state in one line in human readable form")
    String stateOfTask;
    @Schema(name="Task stafrt date", description="Task start date and time")
    Date started;
    @Schema(name="Task finish date", description="Task start date and time")
    Date finished;
    @Schema(name="Task run duration", description="Task run duration, milliseconds")
    Long durationMS;
    @Schema(name="Task completion percent", description="Task completion percent")    
    Double percentComplete;
    @Schema(name="Task messages", description="Task messages list")    
    List<String> messages=new ArrayList<>();
}
