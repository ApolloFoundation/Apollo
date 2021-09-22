/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class SmcEventRequest {
    public enum Operation {
        SUBSCRIBE("subscribe"),
        UNSUBSCRIBE("unsubscribe");

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        public static Operation from(String name) {
            return SUBSCRIBE;
        }
    }

    @JsonCreator
    @Builder
    public SmcEventRequest(@JsonProperty("operation") Operation operation, @JsonProperty("events") List<Event> events) {
        this.operation = operation;
        this.events = events;
    }

    @Data
    @ToString
    @EqualsAndHashCode
    public static class Event {
        private String name;
        private String signature;
        private String fromBlock;
        private List<Filter> filter;

        @JsonCreator
        @Builder
        public Event(@JsonProperty("name") String name, @JsonProperty("signature") String signature, @JsonProperty("fromBlock") String fromBlock, @JsonProperty("filter") List<Filter> filter) {
            this.name = name;
            this.signature = signature;
            this.fromBlock = fromBlock;
            this.filter = filter;
        }
    }

    @Data
    @ToString
    @EqualsAndHashCode
    public static class Filter {
        private String parameter;
        private String value;

        @JsonCreator
        @Builder
        public Filter(@JsonProperty("parameter") String parameter, @JsonProperty("value") String value) {
            this.parameter = parameter;
            this.value = value;
        }
    }

    private Operation operation;
    private List<Event> events;

}
