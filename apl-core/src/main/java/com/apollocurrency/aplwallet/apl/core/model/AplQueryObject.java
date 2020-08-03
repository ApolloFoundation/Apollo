/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AplQueryObject {
    private byte type = -1;
    private List<Long> accounts = new ArrayList<>();
    private int firstHeight;
    private int lastHeight;
    private int startTime; //timestamp in seconds
    private int endTime; //timestamp in seconds
    private int page = -1;
    private int perPage = 25;
    private OrderByEnum order = OrderByEnum.ASC;

    /**
     * Gets or Sets orderBy
     */
    public enum OrderByEnum {
        ASC("asc"),
        DESC("desc");
        @Getter
        private final String value;

        OrderByEnum(String value) {
            this.value = value;
        }

        /**
         * Return the enum constant given name or ASC constant if the given name doesn't match any enum constant.
         *
         * @param value name of constant
         * @return the enum constant
         */
        public static OrderByEnum from(String value) {
            for (OrderByEnum v : OrderByEnum.values()) {
                if (v.getValue().equalsIgnoreCase(value)) {
                    return v;
                }
            }
            return ASC;
        }

    }

    public AplQueryObject(QueryObject query) {
        this.setType(query.getType() != null ? query.getType().byteValue() : (byte) -1);
        if (query.getAccounts() != null) {
            this.setAccounts(query.getAccounts().stream().map(Convert::parseAccountId).collect(Collectors.toUnmodifiableList()));
        } else {
            this.setAccounts(new ArrayList<>());
        }

        this.setStartTime(query.getStartTime() != null ? Convert2.toEpochTime(query.getStartTime()) : -1);
        this.setEndTime(query.getEndTime() != null ? Convert2.toEpochTime(query.getEndTime()) : -1); //timestamp
        this.setFirstHeight(query.getFirst() != null ? query.getFirst().intValue() : -1);
        this.setLastHeight(query.getLast() != null ? query.getLast().intValue() : -1); //timestamp

        this.setPage(query.getPage() != null ? query.getPage() : -1);
        this.setPerPage(query.getPerPage() != null ? query.getPerPage() : 25);

        if (query.getOrderBy() != null) {
            this.setOrder(OrderByEnum.from(query.getOrderBy().name()));
        } else {
            this.setOrder(OrderByEnum.ASC);
        }
    }

    public int getFrom() {
        if (getPage() > 0) {
            return (getPage() - 1) * getPerPage();
        } else {
            return 0;
        }
    }

    public int getTo() {
        return getFrom() + getPerPage() - 1;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AplQueryObject.class.getSimpleName() + "[", "]")
            .add("type=" + type)
            .add("accounts=[" + accounts.stream().map(Long::toUnsignedString).collect(Collectors.joining(",")) + "]")
            .add("firstHeight=" + firstHeight)
            .add("lastHeight=" + lastHeight)
            .add("startTime=" + startTime)
            .add("endTime=" + endTime)
            .add("startTimeUnix=" + Convert2.fromEpochTime(startTime))
            .add("endTimeUnix=" + Convert2.fromEpochTime(endTime))
            .add("page=" + page)
            .add("perPage=" + perPage)
            .add("order=" + order)
            .toString();
    }
}
