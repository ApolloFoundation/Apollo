/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Representation of task that takes noticeable time
 * and should de displayed to user in UI
 *
 * @author alukin@gmail.com
 */
@Schema(name = "DurableTask", description = "Information about running task")
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DurableTaskInfo {
    @JsonIgnore
    public static final String[] TASK_STATES = {"Started", "In progress", "Finished", "Cancelled", "Paused", "All"};
    @Schema(name = "ID of task", description = "Identification of task")
    public String id;
    @Schema(name = "Name of task", description = "Short but descriptive name of task")
    public String name;
    @Schema(name = "Description of task", description = "Description of task in one line")
    public String decription;
    @Schema(name = "Is task critical", description = "Taks is critical and should be displayed on top of UI window")
    public Boolean isCrititcal = false;
    @Schema(name = "Task state", description = "Task state in one line in human readable form",
        allowableValues = {"Started", "In progress", "Finished", "Cancelled", "Paused"})
    public String stateOfTask;
    @Schema(name = "Task start date", description = "Task start date and time")
    public Date started;
    @Schema(name = "Task finish date", description = "Task finish date and time")
    public Date finished;
    @Schema(name = "Task run duration", description = "Task run duration, milliseconds")
    public Long durationMS;
    @Schema(name = "Task completion percent", description = "Task completion percent")
    public Double percentComplete;
    @Schema(name = "Task messages", description = "Task messages list")
    public List<String> messages = new ArrayList<>();
}
