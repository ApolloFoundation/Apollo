/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.annotation;

/**
 * @author silaev-firstbridge on 11/22/2019
 */
public enum DmlMarker {
    SET_ARRAY,
    MERGE,
    NAMED_SUB_SELECT,
    FULL_TEXT_SEARCH,
    /**
     * with no `FOR UPDATE NOWAIT`
     */
    UPDATE_WITH_LIMIT,
    /**
     * with no `FOR UPDATE NOWAIT`
     */
    DELETE_WITH_LIMIT,
    FROM_ABSENCE,
    CHECK_IF_TABLE_EXISTS,
    RESERVED_KEYWORD_USE,
    /**
     * COALESCE
     */
    IFNULL_USE,
    /**
     * DUAL is a way to select a constant
     */
    DUAL_TABLE_USE,
    TEMP_TABLE_USE
}
