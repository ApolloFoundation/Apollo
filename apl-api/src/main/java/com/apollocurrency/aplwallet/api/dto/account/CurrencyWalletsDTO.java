/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.account;

import com.apollocurrency.aplwallet.api.dto.WalletDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrii Boiarskyi
 * @see CurrenciesWalletsDTO
 * @since 1.48.4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyWalletsDTO {
    private String currency;
    private List<WalletDTO> wallets = new ArrayList<>();
}
