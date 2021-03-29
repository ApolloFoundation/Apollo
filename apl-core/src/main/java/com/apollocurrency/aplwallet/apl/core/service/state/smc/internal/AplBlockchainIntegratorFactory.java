/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.blockchain.SMCNotFoundException;
import com.apollocurrency.smc.blockchain.tx.SMCOperationReceipt;
import com.apollocurrency.smc.contract.vm.SMCMessageSenderException;
import com.apollocurrency.smc.contract.vm.internal.BlockchainInfo;
import com.apollocurrency.smc.data.type.Address;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.Objects;

@Slf4j
@Singleton
public class AplBlockchainIntegratorFactory {

    private final AccountService accountService;
    private final ServerInfoService serverInfoService;

    @Inject
    public AplBlockchainIntegratorFactory(AccountService accountService, ServerInfoService serverInfoService) {
        this.accountService = Objects.requireNonNull(accountService);
        this.serverInfoService = Objects.requireNonNull(serverInfoService);
    }

    public BlockchainIntegrator createInstance(final long originatorTransactionId, Account txSenderAccount, Account txRecipientAccount, final LedgerEvent ledgerEvent) {
        return new BlockchainIntegrator() {
            @Override
            public SMCOperationReceipt sendMessage(Address from, Address to, String data) {
                throw new UnsupportedOperationException("Not implemented.");
            }

            @Override
            public SMCOperationReceipt sendMoney(final Address fromAdr, Address toAdr, BigInteger value) {
                SMCOperationReceipt.SMCOperationReceiptBuilder txReceiptBuilder = SMCOperationReceipt.builder()
                    .transactionId(Long.toUnsignedString(originatorTransactionId));
                long amount = value.longValueExact();
                Account sender;
                Account recipient;
                /* case 1) from txSenderAccount to txRecipientAccount
                 * case 2) from txRecipientAccount to arbitrary target account
                 */
                AplAddress from = new AplAddress(fromAdr);
                AplAddress to = new AplAddress(toAdr);
                try {
                    if (from.getLongId() == txSenderAccount.getId()) {//case 1
                        if (to.getLongId() != txRecipientAccount.getId()) {
                            throw new SMCMessageSenderException("Wrong recipient address");
                        }
                        sender = txSenderAccount;
                        recipient = txRecipientAccount;
                    } else if (from.getLongId() == txRecipientAccount.getId()) {//case 2
                        sender = txRecipientAccount; //contract address
                        recipient = accountService.getAccount(to.getLongId());
                        if (recipient == null) {
                            throw new SMCNotFoundException("Recipient not found, recipient=" + to.getLongId());
                        }
                    } else {
                        throw new SMCMessageSenderException("Wrong sender address");
                    }
                    txReceiptBuilder
                        .senderId(Long.toUnsignedString(sender.getId()))
                        .recipientId(Long.toUnsignedString(recipient.getId()));

                    if (sender.getUnconfirmedBalanceATM() < amount) {
                        throw new SMCMessageSenderException("Insufficient balance.");
                    }
                    accountService.addToBalanceAndUnconfirmedBalanceATM(txSenderAccount, ledgerEvent, originatorTransactionId, -amount);
                    accountService.addToBalanceAndUnconfirmedBalanceATM(txRecipientAccount, ledgerEvent, originatorTransactionId, amount);
                } catch (Exception e) {
                    //TODO adjust error code
                    txReceiptBuilder.errorCode(1L).errorDescription(e.getMessage());
                }
                return txReceiptBuilder.build();
            }

            @Override
            public BlockchainInfo getBlockchainInfo() {
                BlockchainStatusDto blockchainStatus = serverInfoService.getBlockchainStatus();
                return BlockchainInfo.builder()
                    .chainId(blockchainStatus.getChainId().toString())
                    .height(blockchainStatus.getNumberOfBlocks())
                    .blockId(blockchainStatus.getLastBlock())
                    .timestamp(blockchainStatus.getTime())
                    .build();
            }
        };
    }
}
