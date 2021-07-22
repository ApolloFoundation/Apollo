/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.response;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response object for the p2p 'getTransactions' endpoint
 * @author Andrii Boiarskyi
 * @see com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest
 * @since 1.48.4
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTransactionsResponse extends BaseP2PResponse {
    private List<TransactionDTO> transactions;
}
