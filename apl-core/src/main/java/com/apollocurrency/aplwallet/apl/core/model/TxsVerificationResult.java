/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.FailedTransactionVerificationServiceImpl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Failed transaction's verification container, consisting of fully verified and not verified transactions
 * @author Andrii Boiarskyi
 * @see VerifiedTransaction
 * @see FailedTransactionVerificationServiceImpl
 * @since 1.48.4
 */
@EqualsAndHashCode
@Getter
@Setter
public class TxsVerificationResult {
    private final Map<Long, VerifiedTransaction> txs;
    private volatile int fromHeight;
    private volatile int toHeight;



    public TxsVerificationResult(Map<Long, VerifiedTransaction> failedTxs) {
        this.txs = failedTxs;
    }

    public boolean isVerified(long id) {
        VerifiedTransaction tx = this.txs.get(id);
        return tx != null && tx.isVerified();
    }

    public Set<Long> allVerifiedIds() {
        return txs.entrySet()
            .stream()
            .filter(e -> e.getValue().isVerified())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public Set<Long> allNotVerifiedIds() {
        return txs.entrySet()
            .stream()
            .filter(e -> !e.getValue().isVerified())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public List<VerifiedTransaction> all() {
        return new ArrayList<>(txs.values());
    }

    public Optional<VerifiedTransaction> get(long id) {
        return Optional.ofNullable(txs.get(id));
    }

    public TxsVerificationResult() {
        this.txs = new HashMap<>();
    }

    public boolean isEmpty() {
        return txs.isEmpty();
    }
}
