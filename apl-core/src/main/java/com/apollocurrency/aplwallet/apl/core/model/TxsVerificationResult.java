/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.FailedTransactionVerificationServiceImpl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final Map<Long, VerifiedTransaction> verified;
    private final Map<Long, VerifiedTransaction> notVerified;
    private volatile int fromHeight;
    private volatile int toHeight;



    public TxsVerificationResult(Map<Long, VerifiedTransaction> failedTxs) {
        this.verified = failedTxs.entrySet()
            .stream()
            .filter(e -> e.getValue().isVerified())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.notVerified = failedTxs.entrySet()
            .stream()
            .filter(e -> !e.getValue().isVerified())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean isVerified(long id) {
        return this.verified.get(id) != null;
    }

    public Set<Long> allVerifiedIds() {
        return new HashSet<>(verified.keySet());
    }

    public Set<Long> allNotVerifiedIds() {
        return new HashSet<>(notVerified.keySet());
    }

    public List<VerifiedTransaction> all() {
        return Stream.concat(verified.values().stream(), notVerified.values().stream()).collect(Collectors.toList());
    }

    public Optional<VerifiedTransaction> get(long id) {
        if (verified.containsKey(id)) {
            return Optional.of(verified.get(id));
        }
        return Optional.ofNullable(notVerified.get(id));
    }

    public TxsVerificationResult() {
        this.notVerified = Map.of();
        this.verified = Map.of();
    }

    public boolean isEmpty() {
        return verified.isEmpty() && notVerified.isEmpty();
    }
}
