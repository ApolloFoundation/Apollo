/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author alukin@gmail.com
 */
@Schema(name = "ThreadsInfoDTO", description = "Information about backend's runnin thread")
public class ThreadInfoDTO {
    @Schema(name = "Name of thread", description = "Name of thread")
    public String name;
    @Schema(name = "State of thread", description = "State of thread")
    public String state;
    @Schema(name = "Priority of thread", description = "Priority of thread")
    public Integer priority;
    @Schema(name = " Is it daemon thread", description = "Is thread started as daemon thread")
    public Boolean isDaemon;
    @Schema(name = "CPU time of thread", description = "CPU time used by thread")
    public Long cpuTime;
}
