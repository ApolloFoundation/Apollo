/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Andrii Boiarskyi
 * @since 1.48.4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TxErrorHashDTO {
    private String id;
    private String errorHash;
    private String error;
}
