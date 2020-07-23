package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.AccountApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.model.AccountInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.AccountReq;
import com.apollocurrency.aplwallet.api.v2.model.AccountReqTest;
import com.apollocurrency.aplwallet.api.v2.model.CreateChildAccountResp;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.AccountInfoMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Objects;
import java.util.stream.Collectors;

@RequestScoped
public class AccountApiServiceImpl implements AccountApiService {

    private final AccountService accountService;
    private final AccountInfoMapper accountInfoMapper;
    private final TransactionCreator transactionCreator;

    @Inject
    public AccountApiServiceImpl(AccountService accountService, AccountInfoMapper accountInfoMapper, TransactionCreator transactionCreator) {
        this.accountService = Objects.requireNonNull(accountService);
        this.accountInfoMapper = Objects.requireNonNull(accountInfoMapper);
        this.transactionCreator = Objects.requireNonNull(transactionCreator);
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
        response.setTx(Convert.toHexString(transaction.getCopyTxBytes()));

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
        response.setTx(Convert.toHexString(transaction.getUnsignedBytes()));

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
