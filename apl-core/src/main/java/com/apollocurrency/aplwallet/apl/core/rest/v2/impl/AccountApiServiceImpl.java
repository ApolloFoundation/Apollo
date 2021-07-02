package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.AccountApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.model.AccountInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.AccountReq;
import com.apollocurrency.aplwallet.api.v2.model.AccountReqSendMoney;
import com.apollocurrency.aplwallet.api.v2.model.AccountReqTest;
import com.apollocurrency.aplwallet.api.v2.model.CreateChildAccountResp;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.AccountInfoMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.MultiSigCredential;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Objects;
import java.util.stream.Collectors;

@RequestScoped
public class AccountApiServiceImpl implements AccountApiService {

    private final BlockchainConfig blockchainConfig;
    private final AccountService accountService;
    private final AccountInfoMapper accountInfoMapper;
    private final TransactionCreator transactionCreator;
    private final TxBContext txBContext;

    @Inject
    public AccountApiServiceImpl(BlockchainConfig blockchainConfig, AccountService accountService, AccountInfoMapper accountInfoMapper, TransactionCreator transactionCreator) {
        this.accountService = Objects.requireNonNull(accountService);
        this.accountInfoMapper = Objects.requireNonNull(accountInfoMapper);
        this.transactionCreator = Objects.requireNonNull(transactionCreator);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    @Override
    public Response createChildAccountTxSendMony(AccountReqSendMoney body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        long parentAccountId = Convert.parseAccountId(body.getParent());
        Account parentAccount = accountService.getAccount(parentAccountId);
        if (parentAccount == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "parent_account", body.getParent()).build();
        }
        long senderAccountId = Convert.parseAccountId(body.getSender());
        Account senderAccount = accountService.getAccount(senderAccountId);
        if (senderAccount == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "sender_account", body.getSender()).build();
        }
        long recipientAccountId = Convert.parseAccountId(body.getRecipient());
        Account recipientAccount = accountService.getAccount(recipientAccountId);
        if (recipientAccount == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "recipient_account", body.getRecipient()).build();
        }

        CreateChildAccountResp response = new CreateChildAccountResp();
        response.setParent(body.getParent());

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .version(2)
            .senderAccount(senderAccount)
            .publicKey(accountService.getPublicKeyByteArray(senderAccountId))
            .recipientId(recipientAccountId)
            .deadlineValue("1440")
            .amountATM(body.getAmount())
            .feeATM(0)
            .attachment(Attachment.ORDINARY_PAYMENT)
            .broadcast(false)
            .validate(false)
            .message(new MessageAppendix("simple text message"))
            .credential(
                new MultiSigCredential(2,
                    Crypto.getKeySeed(body.getCsecret()),
                    Crypto.getKeySeed(body.getPsecret())
                )
            )
            .build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);
        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response createChildAccountTxTest(AccountReqTest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        int childCount = body.getChildSecretList().size();
        if (childCount == 0) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_PARAM_VALUE, "child_count").build();
        }
        long parentAccountId = Convert.parseAccountId(body.getParent());
        Account parentAccount = accountService.getAccount(parentAccountId);
        if (parentAccount == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "parent_account", body.getParent()).build();
        }
        CreateChildAccountResp response = new CreateChildAccountResp();
        response.setParent(body.getParent());

        ChildAccountAttachment attachment = new ChildAccountAttachment(
            AddressScope.IN_FAMILY,
            childCount,
            body.getChildSecretList().stream()
                .map(Crypto::getPublicKey).collect(Collectors.toUnmodifiableList()));

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .senderAccount(parentAccount)
            .recipientId(parentAccountId)
            .secretPhrase(body.getSecret())
            .deadlineValue("1440")
            .amountATM(0)
            .feeATM(0)
            .attachment(attachment)
            .broadcast(false)
            .validate(false)
            .build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);
        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);

        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response createChildAccountTx(AccountReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        int childCount = body.getChildPublicKeyList().size();
        if (childCount == 0) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_PARAM_VALUE, "child_count").build();
        }
        long parentAccountId = Convert.parseAccountId(body.getParent());
        Account parentAccount = accountService.getAccount(parentAccountId);
        if (parentAccount == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "parent_account", body.getParent()).build();
        }
        CreateChildAccountResp response = new CreateChildAccountResp();
        response.setParent(body.getParent());

        ChildAccountAttachment attachment = new ChildAccountAttachment(
            AddressScope.IN_FAMILY,
            childCount,
            body.getChildPublicKeyList().stream()
                .map(Convert::parseHexString).collect(Collectors.toUnmodifiableList()));

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .senderAccount(parentAccount)
            .recipientId(parentAccountId)
            .publicKey(parentAccount.getPublicKey().getPublicKey())
            .deadlineValue("1440")
            .amountATM(0)
            .feeATM(0)
            .attachment(attachment)
            .broadcast(false)
            .validate(false)
            .build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);
        Result unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), unsignedTxBytes);

        response.setTx(Convert.toHexString(unsignedTxBytes.array()));

        return builder.bind(response).build();
    }

    public Response getAccountInfo(String accountStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        long accountId = Convert.parseAccountId(accountStr);
        Account account = accountService.getAccount(accountId);
        if (account == null) {
            throw new NotFoundException("Account not found, id=" + accountStr);
        }

        AccountInfoResp response = accountInfoMapper.convert(account);

        return builder.bind(response).build();
    }
}
