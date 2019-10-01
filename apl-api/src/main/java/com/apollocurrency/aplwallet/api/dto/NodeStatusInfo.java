/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author alukin@gmail.com
 */

@Schema(name = "NodeStatueInfo", description = "Information about backend state")
@Getter @Setter @ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeStatusInfo {
    @Schema(name="Number of CPU", description="Number of active CPUs")
    public Integer cpuCount;
    @Schema(name="Average CPU load", description="Current average CPU load for all cores")
    public Double cpuLoad;
    @Schema(name="Active threads", description="Threads currently running in aplicaion")    
    public Integer threadsRunning;
    @Schema(name = "DB connections", description = "DB connections currently running in aplicaion")
    public Integer dbConnections;
    @Schema(name="Total memory", description="Tottal memory in bytes")
    public Long memoryTotal;
    @Schema(name="Free memory", description="Free memory available for this application")
    public Long memoryFree;
    @Schema(name="Free disk space", description="Free disk space available to applicaion")
    public Long diskFree; 
    @Schema(name="Node OS", description="Operating system of node")
    public String operatingSystem; 
}
