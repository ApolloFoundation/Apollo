/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.dto;

import com.apollocurrency.smc.data.expr.Term;
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
public class SmcEventSubscriptionRequest {
    public enum Operation {
        SUBSCRIBE,
        UNSUBSCRIBE,
        SUBSCRIBE_TEST;
    }

    @JsonProperty
    private Operation operation;
    @JsonProperty
    private String requestId;
    @JsonProperty
    private List<Event> events;

    @JsonCreator
    @Builder
    public SmcEventSubscriptionRequest(@JsonProperty("operation") Operation operation
        , @JsonProperty("requestId") String requestId
        , @JsonProperty("events") List<Event> events) {
        this.operation = operation;
        this.requestId = requestId;
        this.events = events;
    }

    @Data
    @ToString
    @EqualsAndHashCode
    public static class Event {
        @JsonProperty
        private String subscriptionId;//the subscription Identifier
        @JsonProperty
        private String name;
        @JsonProperty
        private String signature;
        @JsonProperty
        private String fromBlock;
        @JsonProperty
        private Term filter;

        @JsonCreator
        @Builder
        public Event(@JsonProperty("subscriptionId") String subscriptionId
            , @JsonProperty("name") String name
            , @JsonProperty("signature") String signature
            , @JsonProperty("fromBlock") String fromBlock
            , @JsonProperty("filter") Term filter) {
            this.subscriptionId = subscriptionId;
            this.name = name;
            this.signature = signature;
            this.fromBlock = fromBlock;
            this.filter = filter;
        }
    }
}
