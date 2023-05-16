/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P2P request to get confirmed transactions by given ids
 * @author Andrii Boiarskyi
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class GetTransactionsRequest extends BaseP2PRequest {
    @Size(min = 1, max = 100)
    @NotNull (message = "transactionIds object should not be null for GetTransactions request")
    private Set<@NotNull(message = "Null transaction ids are not allowed for GetTransactions request") Long> transactionIds;

    public GetTransactionsRequest(Set<Long> transactionIds, UUID chainId) {
        super("getTransactions", chainId);
        this.transactionIds = transactionIds;
    }

    public void setTransactionIds(Set<Long> transactionIds) {
        this.transactionIds = transactionIds;
    }

    public Set<Long> getTransactionIds() {
        return transactionIds;
    }

    @Override
    public GetTransactionsRequest clone() {
        try {
            return (GetTransactionsRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cloning operation should be supported for GetTransactionsRequest");
        }
    }

    @JsonSetter
    void setTransactionIds(List<String> transactionIds) {
        this.transactionIds = transactionIds.stream().map(Long::parseUnsignedLong).collect(Collectors.toSet());
    }

    @JsonGetter(value = "transactionIds")
    List<String> getStringTransactionIds() {
        return transactionIds.stream().map(Long::toUnsignedString).collect(Collectors.toList());
    }
}
