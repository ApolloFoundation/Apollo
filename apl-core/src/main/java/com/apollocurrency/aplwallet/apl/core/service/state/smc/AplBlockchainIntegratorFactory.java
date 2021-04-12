/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.smc.SMCException;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.blockchain.SMCNotFoundException;
import com.apollocurrency.smc.blockchain.tx.SMCOperationReceipt;
import com.apollocurrency.smc.contract.vm.ContractAddress;
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

    public BlockchainIntegrator createIntegrator(final long originatorTransactionId, Account txSenderAccount, Account txRecipientAccount, final LedgerEvent ledgerEvent) {
        return new BlockchainIntegrator() {
            @Override
            public SMCOperationReceipt sendMessage(Address from, Address to, String data) {
                throw new UnsupportedOperationException("Not implemented yet.");
            }

            @Override
            public SMCOperationReceipt sendMoney(final Address fromAdr, Address toAdr, BigInteger value) {
                log.debug("--send money ---1: from={} to={} value={}", fromAdr, toAdr, value);
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
                log.debug("--send money ---2: from={} to={} amount={}", from, to, amount);
                try {
                    if (from.getLongId() == txSenderAccount.getId()) {//case 1
                        log.debug("--send money ---2.1: ");
                        if (to.getLongId() != txRecipientAccount.getId()) {
                            throw new SMCMessageSenderException("Wrong recipient address");
                        }
                        sender = txSenderAccount;
                        recipient = txRecipientAccount;
                    } else if (from.getLongId() == txRecipientAccount.getId()) {//case 2
                        log.debug("--send money ---2.2: ");
                        sender = txRecipientAccount; //contract address
                        recipient = accountService.getAccount(to.getLongId());
                        if (recipient == null) {
                            throw new SMCNotFoundException("Recipient not found, recipient=" + to.getLongId());
                        }
                    } else {
                        throw new SMCMessageSenderException("Wrong sender address");
                    }
                    log.debug("--send money ---3: sender={} recipient={}", sender, recipient);
                    txReceiptBuilder
                        .senderId(Long.toUnsignedString(sender.getId()))
                        .recipientId(Long.toUnsignedString(recipient.getId()));
                    log.debug("--send money ---4: before blockchain tx, receipt={}", txReceiptBuilder.build());

                    if (sender.getUnconfirmedBalanceATM() < amount) {
                        throw new SMCMessageSenderException("Insufficient balance.");
                    }
                    accountService.addToBalanceAndUnconfirmedBalanceATM(txSenderAccount, ledgerEvent, originatorTransactionId, -amount);
                    accountService.addToBalanceAndUnconfirmedBalanceATM(txRecipientAccount, ledgerEvent, originatorTransactionId, amount);
                    log.debug("--send money ---5: before blockchain tx, receipt={}", txReceiptBuilder.build());
                } catch (Exception e) {
                    //TODO adjust error code
                    txReceiptBuilder.errorCode(1L).errorDescription(e.getMessage()).errorDetails(ThreadUtils.last5Stacktrace());
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

            @Override
            public ContractAddress createAddressInstance(Address address, Address contractAddress) {
                return new SMCAddress(address, contractAddress, this, accountService);
            }

        };
    }

    public BlockchainIntegrator createMockIntegrator(final long originatorTransactionId) {
        SMCOperationReceipt rc = SMCOperationReceipt.OK_RECEIPT;
        rc.setTransactionId(Convert.toHexString(BigInteger.valueOf(originatorTransactionId).toByteArray()));
        return new BlockchainIntegrator() {
            @Override
            public BlockchainInfo getBlockchainInfo() {
                return BlockchainInfo.builder().build();
            }

            @Override
            public SMCOperationReceipt sendMessage(Address from, Address to, String s) {
                return rc;
            }

            @Override
            public SMCOperationReceipt sendMoney(Address from, Address to, BigInteger bigInteger) {
                return rc;
            }

            @Override
            public ContractAddress createAddressInstance(Address address, Address contractAddress) {
                return new ContractAddress() {
                    @Override
                    public BigInteger balance() {
                        return BigInteger.ZERO;
                    }

                    @Override
                    public void transfer(BigInteger value) throws SMCException {
                        //this is a mock object
                    }

                    @Override
                    public boolean send(BigInteger value) {
                        return true;
                    }

                    @Override
                    public byte[] get() {
                        return address.get();
                    }
                };
            }
        };
    }

    static class SMCAddress implements ContractAddress {
        private final Address contractAddress;
        private final AplAddress address;
        private final long addressId;
        private final BlockchainIntegrator integrator;
        private final AccountService accountService;

        public SMCAddress(Address address, Address contractAddress, BlockchainIntegrator integrator, AccountService accountService) {
            this.contractAddress = Objects.requireNonNull(contractAddress);
            this.address = new AplAddress(address);
            this.addressId = this.address.getLongId();
            this.integrator = integrator;
            this.accountService = accountService;
        }

        @Override
        public BigInteger balance() {
            Account account = accountService.getAccount(addressId);
            return BigInteger.valueOf(account.getBalanceATM());
        }

        @Override
        public void transfer(BigInteger value) throws SMCException {
            integrator.sendMoney(contractAddress, address, value);
        }

        @Override
        public boolean send(BigInteger value) {
            try {
                integrator.sendMoney(contractAddress, address, value);
                return true;
            } catch (Exception e) {
                log.error("Error: address.send(" + contractAddress.getHex() + "," + address.getHex() + "), cause: " + e.getClass().getSimpleName() + ":" + e.getMessage(), e);
            }
            return false;
        }

        @Override
        public byte[] get() {
            return address.get();
        }
    }
}
