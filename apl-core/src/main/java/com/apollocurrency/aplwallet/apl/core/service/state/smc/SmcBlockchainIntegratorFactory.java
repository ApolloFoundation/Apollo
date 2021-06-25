/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.storage.PersistentMappingRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.storage.ReadonlyMappingRepository;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.blockchain.ContractNotFoundException;
import com.apollocurrency.smc.blockchain.MockIntegrator;
import com.apollocurrency.smc.blockchain.storage.AddressJsonConverter;
import com.apollocurrency.smc.blockchain.storage.BigIntegerJsonConverter;
import com.apollocurrency.smc.blockchain.storage.BigNumJsonConverter;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepository;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepositoryFactory;
import com.apollocurrency.smc.blockchain.storage.StringJsonConverter;
import com.apollocurrency.smc.blockchain.tx.SMCOperationReceipt;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.vm.ContractBlock;
import com.apollocurrency.smc.contract.vm.ContractBlockchainTransaction;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SendMsgException;
import com.apollocurrency.smc.contract.vm.global.BlockchainInfo;
import com.apollocurrency.smc.contract.vm.global.SMCBlock;
import com.apollocurrency.smc.contract.vm.global.SMCTransaction;
import com.apollocurrency.smc.contract.vm.operation.SMCOperationProcessor;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.BigNum;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.Objects;

@Slf4j
@Singleton
public class SmcBlockchainIntegratorFactory {

    private final AccountService accountService;
    private final Blockchain blockchain;
    private final ServerInfoService serverInfoService;
    private final BlockConverter blockConverter;
    private final SmcContractStorageService smcContractStorageService;
    private final SmcMappingRepositoryClassFactory smcMappingRepositoryClassFactory;

    @Inject
    public SmcBlockchainIntegratorFactory(AccountService accountService, Blockchain blockchain, ServerInfoService serverInfoService, SmcContractStorageService smcContractStorageService) {
        this.accountService = Objects.requireNonNull(accountService);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.serverInfoService = Objects.requireNonNull(serverInfoService);
        this.blockConverter = new BlockConverter();
        this.smcContractStorageService = Objects.requireNonNull(smcContractStorageService);
        this.smcMappingRepositoryClassFactory = new SmcMappingRepositoryClassFactory(smcContractStorageService);
    }

    public BlockchainIntegrator createProcessor(final Transaction originator, AbstractSmcAttachment attachment, Account txSenderAccount, Account txRecipientAccount, final LedgerEvent ledgerEvent) {
        BlockchainIntegrator integrator = createIntegrator(originator, attachment, txSenderAccount, txRecipientAccount, ledgerEvent);
        return SMCOperationProcessor.createProcessor(integrator, new ExecutionLog());
    }

    public BlockchainIntegrator createMockProcessor(final long originatorTransactionId) {
        var transaction = new AplAddress(originatorTransactionId);
        return SMCOperationProcessor.createProcessor(
            new MockIntegrator(transaction)
            , ExecutionLog.EMPTY_LOG);
    }

    public BlockchainIntegrator createReadonlyProcessor() {
        return SMCOperationProcessor.createProcessor(new ReadonlyIntegrator(), new ExecutionLog());
    }

    private BlockchainIntegrator createIntegrator(final Transaction transaction, AbstractSmcAttachment attachment, Account txSenderAccount, Account txRecipientAccount, final LedgerEvent ledgerEvent) {
        final long originatorTransactionId = transaction.getId();
        final ContractBlock currentBlock = blockConverter.apply(transaction.getBlock());
        Address trAddr = new AplAddress(transaction.getId());
        final ContractBlockchainTransaction currentTransaction = new SMCTransaction(trAddr.get(), trAddr, attachment.getFuelPrice());

        return new FullIntegrator(originatorTransactionId, txSenderAccount, txRecipientAccount, ledgerEvent, currentBlock, currentTransaction);
    }

    private class ReadonlyIntegrator implements BlockchainIntegrator {

        private static final String READONLY_INTEGRATOR = "Readonly integrator.";

        @Override
        public SMCOperationReceipt sendMessage(Address from, Address to, SmartMethod data) {
            throw new UnsupportedOperationException(READONLY_INTEGRATOR);
        }

        @Override
        public SMCOperationReceipt sendMoney(final Address fromAdr, Address toAdr, BigInteger value) {
            throw new UnsupportedOperationException(READONLY_INTEGRATOR);
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
        public BigInteger getBalance(Address address) {
            Account account = accountService.getAccount(new AplAddress(address).getLongId());
            if (account == null) {
                throw new ContractNotFoundException("Address not found, address=" + address.getHex());
            }
            return BigInteger.valueOf(account.getBalanceATM());
        }

        @Override
        public ContractBlock getBlock(int height) {
            return blockConverter.convert(blockchain.getBlockAtHeight(height));
        }

        @Override
        public ContractBlock getBlock(Address address) {
            return blockConverter.convert(blockchain.getBlock(new AplAddress(address).getLongId()));
        }

        @Override
        public ContractBlock getCurrentBlock() {
            return blockConverter.convert(blockchain.getLastBlock());
        }

        @Override
        public ContractBlockchainTransaction getBlockchainTransaction() {
            throw new UnsupportedOperationException(READONLY_INTEGRATOR);
        }

        @Override
        public ContractMappingRepositoryFactory createMappingFactory(Address contract) {
            return smcMappingRepositoryClassFactory.createReadonlyMappingFactory(contract);
        }
    }

    private class FullIntegrator extends ReadonlyIntegrator {

        final long originatorTransactionId;
        final Account txSenderAccount;
        final Account txRecipientAccount;
        final LedgerEvent ledgerEvent;
        final ContractBlockchainTransaction currentTransaction;
        final ContractBlock currentBlock;

        public FullIntegrator(long originatorTransactionId, Account txSenderAccount, Account txRecipientAccount, LedgerEvent ledgerEvent, ContractBlock currentBlock, ContractBlockchainTransaction currentTransaction) {
            this.originatorTransactionId = originatorTransactionId;
            this.txSenderAccount = txSenderAccount;
            this.txRecipientAccount = txRecipientAccount;
            this.ledgerEvent = ledgerEvent;
            this.currentTransaction = currentTransaction;
            this.currentBlock = currentBlock;
        }

        @Override
        public SMCOperationReceipt sendMessage(Address from, Address to, SmartMethod data) {
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
            /* case 1) from transaction_SenderAccount to transaction_RecipientAccount
             * case 2) from transaction_RecipientAccount to arbitrary target account
             */
            AplAddress from = new AplAddress(fromAdr);
            AplAddress to = new AplAddress(toAdr);
            log.debug("--send money ---2: from={} to={} amount={}", from, to, amount);
            try {
                if (from.getLongId() == txSenderAccount.getId()) {//case 1
                    log.debug("--send money ---2.1: ");
                    if (to.getLongId() != txRecipientAccount.getId()) {
                        throw new SendMsgException("Wrong recipient address");
                    }
                    sender = txSenderAccount;
                    recipient = txRecipientAccount;
                } else if (from.getLongId() == txRecipientAccount.getId()) {//case 2
                    log.debug("--send money ---2.2: ");
                    sender = txRecipientAccount; //contract address
                    recipient = accountService.getAccount(to.getLongId());
                    if (recipient == null) {
                        throw new ContractNotFoundException("Recipient not found, recipient=" + to.getLongId());
                    }
                } else {
                    throw new SendMsgException("Wrong sender address");
                }
                log.debug("--send money ---3: sender={} recipient={}", sender, recipient);
                txReceiptBuilder
                    .senderId(Long.toUnsignedString(sender.getId()))
                    .recipientId(Long.toUnsignedString(recipient.getId()));
                log.debug("--send money ---4: before blockchain tx, receipt={}", txReceiptBuilder.build());

                if (sender.getUnconfirmedBalanceATM() < amount) {
                    throw new SendMsgException("Insufficient balance.");
                }
                accountService.addToBalanceAndUnconfirmedBalanceATM(sender, ledgerEvent, originatorTransactionId, -amount);
                accountService.addToBalanceAndUnconfirmedBalanceATM(recipient, ledgerEvent, originatorTransactionId, amount);
                log.debug("--send money ---5: before blockchain tx, receipt={}", txReceiptBuilder.build());
            } catch (Exception e) {
                //TODO adjust error code
                txReceiptBuilder.errorCode(1L).errorDescription(e.getMessage()).errorDetails(ThreadUtils.last5Stacktrace());
            }
            return txReceiptBuilder.build();
        }

        @Override
        public ContractBlock getCurrentBlock() {
            return currentBlock;
        }

        @Override
        public ContractBlockchainTransaction getBlockchainTransaction() {
            return currentTransaction;
        }

        @Override
        public ContractMappingRepositoryFactory createMappingFactory(Address contract) {
            return smcMappingRepositoryClassFactory.createMappingFactory(contract);
        }

    }

    private static class SmcMappingRepositoryClassFactory {
        private final SmcContractStorageService smcContractStorageService;

        @Inject
        public SmcMappingRepositoryClassFactory(SmcContractStorageService smcContractStorageService) {
            this.smcContractStorageService = smcContractStorageService;
        }

        public ContractMappingRepositoryFactory createMappingFactory(final Address contract) {
            return new ContractMappingRepositoryFactory() {
                @Override
                public boolean hasMapping(String mappingName) {
                    return smcContractStorageService.isMappingExist(contract, mappingName);
                }

                @Override
                public ContractMappingRepository<Address> addressRepository(String mappingName) {
                    return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new AddressJsonConverter());
                }

                @Override
                public ContractMappingRepository<BigNum> bigNumRepository(String mappingName) {
                    return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new BigNumJsonConverter());
                }

                @Override
                public ContractMappingRepository<BigInteger> bigIntegerRepository(String mappingName) {
                    return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new BigIntegerJsonConverter());
                }

                @Override
                public ContractMappingRepository<String> stringRepository(String mappingName) {
                    return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new StringJsonConverter());
                }
            };
        }

        public ContractMappingRepositoryFactory createReadonlyMappingFactory(final Address contract) {
            return new ContractMappingRepositoryFactory() {
                @Override
                public boolean hasMapping(String mappingName) {
                    return smcContractStorageService.isMappingExist(contract, mappingName);
                }

                @Override
                public ContractMappingRepository<Address> addressRepository(String mappingName) {
                    return new ReadonlyMappingRepository<>(smcContractStorageService, contract, mappingName, new AddressJsonConverter());
                }

                @Override
                public ContractMappingRepository<BigNum> bigNumRepository(String mappingName) {
                    return new ReadonlyMappingRepository<>(smcContractStorageService, contract, mappingName, new BigNumJsonConverter());
                }

                @Override
                public ContractMappingRepository<BigInteger> bigIntegerRepository(String mappingName) {
                    return new ReadonlyMappingRepository<>(smcContractStorageService, contract, mappingName, new BigIntegerJsonConverter());
                }

                @Override
                public ContractMappingRepository<String> stringRepository(String mappingName) {
                    return new ReadonlyMappingRepository<>(smcContractStorageService, contract, mappingName, new StringJsonConverter());
                }
            };
        }
    }

    private static class BlockConverter implements Converter<Block, ContractBlock> {

        @Override
        public ContractBlock apply(Block block) {
            Objects.requireNonNull(block);
            return new SMCBlock(
                new AplAddress(block.getId()).get(),
                new AplAddress(block.getGeneratorId()),
                block.getCumulativeDifficulty(),
                BigInteger.ZERO,//TODO not implemented yet
                block.getHeight(),
                block.getTimestamp()
            );
        }
    }
}
