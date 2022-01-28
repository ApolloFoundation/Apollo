/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.InMemoryAccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.event.SmcContractEventManagerClassFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog.SendMoneyRecord;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.crypto.AplIdGenerator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.service.mapping.SmcMappingRepositoryClassFactory;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.blockchain.BlockchainIntegratorFactory;
import com.apollocurrency.smc.blockchain.MockIntegrator;
import com.apollocurrency.smc.blockchain.OperationReceipt;
import com.apollocurrency.smc.blockchain.SMCOperationProcessor;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.blockchain.event.ContractEventManagerFactory;
import com.apollocurrency.smc.blockchain.storage.CachedMappingRepository;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepositoryFactory;
import com.apollocurrency.smc.contract.AddressNotFoundException;
import com.apollocurrency.smc.contract.ContractException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.vm.ContractEventManager;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ExternalContractVirtualMachine;
import com.apollocurrency.smc.contract.vm.LogDetailedReceipt;
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.contract.vm.global.BlockchainInfo;
import com.apollocurrency.smc.contract.vm.global.SMCBlock;
import com.apollocurrency.smc.contract.vm.global.SMCTransaction;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.ContractBlock;
import com.apollocurrency.smc.data.type.ContractBlockchainTransaction;
import com.apollocurrency.smc.txlog.ArrayTxLog;
import com.apollocurrency.smc.txlog.DevNullLog;
import com.apollocurrency.smc.txlog.TxLog;
import com.apollocurrency.smc.txlog.TxLogProcessor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Singleton
public class SmcBlockchainIntegratorFactoryCreator {

    private final AccountService accountService;
    private final Blockchain blockchain;
    private final ServerInfoService serverInfoService;
    private final BlockConverter blockConverter;
    private final SmcContractRepository contractRepository;
    private final SmcMappingRepositoryClassFactory smcMappingRepositoryClassFactory;
    private final SmcContractEventManagerClassFactory smcContractEventManagerClassFactory;
    private final TxLogProcessor txLogProcessor;
    private final HashSumProvider hashSumProvider;

    @Inject
    public SmcBlockchainIntegratorFactoryCreator(AccountService accountService,
                                                 Blockchain blockchain,
                                                 ServerInfoService serverInfoService,
                                                 SmcContractRepository contractRepository,
                                                 SmcMappingRepositoryClassFactory smcMappingRepositoryClassFactory,
                                                 SmcContractEventManagerClassFactory smcContractEventManagerClassFactory,
                                                 TxLogProcessor txLogProcessor,
                                                 HashSumProvider hashSumProvider) {
        this.accountService = Objects.requireNonNull(accountService);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.serverInfoService = Objects.requireNonNull(serverInfoService);
        this.blockConverter = new BlockConverter();
        this.contractRepository = Objects.requireNonNull(contractRepository);
        this.smcMappingRepositoryClassFactory = Objects.requireNonNull(smcMappingRepositoryClassFactory);
        this.smcContractEventManagerClassFactory = Objects.requireNonNull(smcContractEventManagerClassFactory);
        this.txLogProcessor = txLogProcessor;
        this.hashSumProvider = Objects.requireNonNull(hashSumProvider);
    }

    public BlockchainIntegratorFactory createProcessorFactory(final Transaction originator, AbstractSmcAttachment attachment, Account txSenderAccount, Account txRecipientAccount, final LedgerEvent ledgerEvent) {
        return machine -> {
            long originatorTransactionId = originator.getId();
            Address txAddress = new AplAddress(originatorTransactionId);
            var integrator = (BlockchainIntegrator) new FullIntegrator(originatorTransactionId, txSenderAccount, txRecipientAccount, ledgerEvent,
                blockConverter.convert(originator.getBlock()),
                new SMCTransaction(txAddress.get(), txAddress, attachment.getFuelPrice()),
                txLogProcessor,
                new SmcInMemoryAccountService(accountService),
                machine);

            return new SMCOperationProcessor(integrator, new ExecutionLog());
        };
    }

    public BlockchainIntegratorFactory createMockProcessorFactory(final long originatorTransactionId) {
        return machine -> new SMCOperationProcessor(new MockIntegrator(new AplAddress(originatorTransactionId), null, hashSumProvider), ExecutionLog.EMPTY_LOG);
    }

    public BlockchainIntegratorFactory createReadonlyProcessorFactory() {
        return machine -> new SMCOperationProcessor(new ReadonlyIntegrator(new SmcInMemoryAccountService(accountService)), new ExecutionLog());
    }

    private abstract class BaseIntegrator implements BlockchainIntegrator {

        final InMemoryAccountService inMemoryAccountService;

        public BaseIntegrator(InMemoryAccountService inMemoryAccountService) {
            this.inMemoryAccountService = inMemoryAccountService;
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
            Account account = inMemoryAccountService.getAccount(address);
            if (account == null) {
                throw new AddressNotFoundException(address);
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
        public HashSumProvider getHashSumProvider() {
            return hashSumProvider;
        }
    }

    private class ReadonlyIntegrator extends BaseIntegrator {
        private static final String READONLY_INTEGRATOR = "Readonly integrator.";

        public ReadonlyIntegrator(InMemoryAccountService inMemoryAccountService) {
            super(inMemoryAccountService);
        }

        @Override
        public TxLog txLogger() {
            return new DevNullLog();//it's suitable for ReadOnly integrator
        }

        @Override
        public void commit() {
            //nothing to do
        }

        @Override
        public Address createContractAddress() {
            throw new UnsupportedOperationException(READONLY_INTEGRATOR);
        }

        @Override
        public OperationReceipt createContract(SmartContract newContract, Address originator, Address caller, Fuel contractFuel) {
            throw new UnsupportedOperationException(READONLY_INTEGRATOR);
        }

        @Override
        public ResultValue sendMessage(Address contract, SmartMethod method, Address originator, Address caller, Fuel fuel) {
            throw new UnsupportedOperationException(READONLY_INTEGRATOR);
        }

        @Override
        public OperationReceipt sendMoney(Address contract, Address fromAdr, Address toAdr, BigInteger value) {
            throw new UnsupportedOperationException(READONLY_INTEGRATOR);
        }

        @Override
        public ContractBlockchainTransaction getBlockchainTransaction() {
            throw new UnsupportedOperationException(READONLY_INTEGRATOR);
        }

        @Override
        public ContractMappingRepositoryFactory createMappingFactory(Address contract) {
            return smcMappingRepositoryClassFactory.createReadonlyMappingFactory(contract);
        }

        @Override
        public ContractEventManager createEventManager(Address contract) {
            return smcContractEventManagerClassFactory.createMockEventManagerFactory().create(contract);
        }

    }

    private class FullIntegrator extends BaseIntegrator {

        final long originatorTransactionId;
        long contractCount;
        final Account txSenderAccount;
        final Account txRecipientAccount;
        final LedgerEvent ledgerEvent;
        final ContractBlockchainTransaction currentTransaction;
        final ContractBlock currentBlock;
        final TxLog txLog;
        final Set<CachedMappingRepository<?>> cachedMappingRepositories;
        final TxLogProcessor txLogProcessor;
        final ContractEventManagerFactory eventManagerFactory;
        final ExternalContractVirtualMachine machine;

        public FullIntegrator(long originatorTransactionId,
                              Account txSenderAccount, Account txRecipientAccount, LedgerEvent ledgerEvent,
                              ContractBlock currentBlock,
                              ContractBlockchainTransaction currentTransaction,
                              TxLogProcessor txLogProcessor,
                              InMemoryAccountService inMemoryAccountService,
                              ExternalContractVirtualMachine machine) {
            super(inMemoryAccountService);
            this.contractCount = 1;
            this.originatorTransactionId = originatorTransactionId;
            this.txSenderAccount = txSenderAccount;
            this.txRecipientAccount = txRecipientAccount;
            this.ledgerEvent = ledgerEvent;
            this.currentTransaction = currentTransaction;
            this.currentBlock = currentBlock;
            this.txLogProcessor = txLogProcessor;
            this.txLog = new ArrayTxLog(new AplAddress(originatorTransactionId));
            this.cachedMappingRepositories = new HashSet<>();
            this.eventManagerFactory = smcContractEventManagerClassFactory.createEventManagerFactory(currentTransaction, blockchain.getHeight(), txLog);
            this.machine = machine;
        }

        @Override
        public TxLog txLogger() {
            return txLog;
        }

        @Override
        public void commit() {
            //commit mapping changes
            cachedMappingRepositories.forEach(CachedMappingRepository::commit);
            cachedMappingRepositories.clear();

            //apply changes from action log
            txLogProcessor.process(txLog);
        }

        @Override
        public Address createContractAddress() {
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                var digest = Crypto.sha256();
                digest.update(Convert.toBytes(originatorTransactionId));
                digest.update(Convert.toBytes(contractCount++));
                var accountId = AplIdGenerator.ACCOUNT.getIdByHash(digest.digest()).longValue();
                var address = new AplAddress(accountId);
                if (!contractRepository.isContractExist(address)) {
                    return address;
                }
            }
            throw new ContractException(new AplAddress(originatorTransactionId), "Can't generate the uniq contract address, nonce=" + contractCount);
        }

        @Override
        public OperationReceipt createContract(SmartContract newContract, Address originator, Address caller, Fuel contractFuel) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }

        @Override
        public ResultValue sendMessage(Address contract, SmartMethod method, Address originator, Address caller, Fuel fuel) {
            SmartContract externalContract;
            log.debug("call external contract={} method={} caller={}", contract, method, caller);

            externalContract = contractRepository.loadContract(contract, originator, caller, fuel);

            log.trace("call external contract:  =2= enter");
            machine.getExecutionLog().add(new LogDetailedReceipt("enter", contract, null, OperationReceipt.NO_ERROR_RECEIPT, new Object[]{}));
            log.trace("call external contract:  =3= eval");
            var rc = machine.invokePayableMethod(externalContract, method);
            log.trace("call external contract:  =4= rc={}", rc);
            machine.getExecutionLog().add(new LogDetailedReceipt("leave", externalContract.address(), null, OperationReceipt.NO_ERROR_RECEIPT, new Object[]{}));
            log.trace("call external contract:  =5= leave");
            return rc;
        }

        @Override
        public OperationReceipt sendMoney(Address contract, final Address fromAdr, Address toAdr, BigInteger value) {
            log.trace("--send money ---1: from={} to={} value={}", fromAdr, toAdr, value);
            var txReceiptBuilder = OperationReceipt.builder()
                .transactionId(Long.toUnsignedString(originatorTransactionId));

            /* case 1) from transaction_SenderAccount to transaction_RecipientAccount
             * case 2) from transaction_RecipientAccount to arbitrary target account
             */
            AplAddress from = new AplAddress(fromAdr);
            AplAddress to = new AplAddress(toAdr);
            log.trace("--send money ---2: from={} to={} amount={}", from, to, value);
            try {
                if (from.getLongId() == txSenderAccount.getId()) {//case 1
                    log.trace("--send money ---2.1: ");
                    if (to.getLongId() != txRecipientAccount.getId()) {
                        throw new ContractException(contract, "Wrong recipient address");
                    }
                } else if (from.getLongId() == txRecipientAccount.getId()) {//case 2
                    log.trace("--send money ---2.2: ");
                    //from - is a contract address
                    //to - is an arbitrary address
                } else {
                    throw new ContractException(contract, "Wrong sender address");
                }
                log.trace("--send money ---3: sender={} recipient={}", from, to);
                txReceiptBuilder
                    .senderId(Long.toUnsignedString(from.getLongId()))
                    .recipientId(Long.toUnsignedString(to.getLongId()));
                log.trace("--send money ---4: before blockchain tx, receipt={}", txReceiptBuilder.build());

                inMemoryAccountService.addToBalanceAndUnconfirmedBalanceATM(from, value.negate());
                inMemoryAccountService.addToBalanceAndUnconfirmedBalanceATM(to, value);

                var rec = SendMoneyRecord.builder()
                    .contract(contract)
                    .sender(from.getLongId())
                    .recipient(to.getLongId())
                    .event(ledgerEvent)
                    .value(value.longValueExact())
                    .transaction(originatorTransactionId)
                    .build();
                txLog.append(rec);

            } catch (Exception e) {
                log.error("--send money error:", e);
                //TODO adjust error code
                txReceiptBuilder.errorCode(1L)
                    .errorDescription(e.getMessage())
                    .errorDetails(ThreadUtils.last5Stacktrace());
            }
            var rc = txReceiptBuilder.build();
            log.trace("--send money ---5: receipt={}", rc);
            return rc;
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
            return smcMappingRepositoryClassFactory.createCachedMappingFactory(contract, cachedMappingRepositories);
        }

        @Override
        public ContractEventManager createEventManager(Address contract) {
            return eventManagerFactory.create(contract);
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
                BigInteger.ZERO,//TODO: block fuel limit - is not implemented yet
                block.getHeight(),

                (int) (Convert2.fromEpochTime(block.getTimestamp()) / 1000)
            );
        }
    }
}
