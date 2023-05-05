/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.api.parameter;

import com.apollocurrency.aplwallet.apl.util.api.PositiveRange;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class FirstLastIndexBeanParam {
    @Parameter(description = "A zero-based index to the first record ID to retrieve (optional).")
    @QueryParam("firstIndex")
    @DefaultValue("0")
    @PositiveOrZero
    private int firstIndex = 0;
    @Parameter(description = "A zero-based index to the last record ID to retrieve (optional).")
    @QueryParam("lastIndex")
    @DefaultValue("-1")
    private int lastIndex = -1;

    public void adjustIndexes(int maxAPIrecords) {
        int tempFirstIndex = Math.min(firstIndex, Integer.MAX_VALUE - maxAPIrecords + 1);
        int tempLastIndex = Math.min(lastIndex, tempFirstIndex + maxAPIrecords - 1);
        if (tempLastIndex < tempFirstIndex) {
            tempLastIndex = tempFirstIndex + maxAPIrecords - 1;
        }
        this.firstIndex = tempFirstIndex;
        this.lastIndex = tempLastIndex;
    }

    public PositiveRange range() {
        return new PositiveRange(firstIndex, lastIndex);
    }
}
