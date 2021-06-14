/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.CurrenciesApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.model.CurrencyBurningTxCreationRequest;
import com.apollocurrency.aplwallet.api.v2.model.TransactionCreationResponse;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyBurningAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Objects;

@RequestScoped
public class CurrenciesApiServiceImpl implements CurrenciesApiService {
    private final TransactionCreator transactionCreator;
    private final AccountService accountService;
    private final AccountCurrencyService accountCurrencyService;
    private final Account2FAHelper account2FAHelper;


    @Inject
    public CurrenciesApiServiceImpl(AccountService accountService, TransactionCreator transactionCreator, AccountCurrencyService accountCurrencyService, Account2FAHelper account2FAHelper) {
        this.transactionCreator = Objects.requireNonNull(transactionCreator);
        this.accountService = accountService;
        this.accountCurrencyService = accountCurrencyService;
        this.account2FAHelper = account2FAHelper;
    }

    @Override
    public Response currencyBurningTx(CurrencyBurningTxCreationRequest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        long senderAccountId = Convert.parseAccountId(body.getSender());
        Account senderAccount = accountService.getAccount(senderAccountId);
        if (senderAccount == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_PARAM, "senderAccount", body.getSender() + " is not exist").build();
        }
        account2FAHelper.verify2FA(Convert2.rsAccount(senderAccountId), body.getPassphrase(), body.getSecretPhrase(), body.getPublicKey(), body.getCode2FA());

        String currencyId = body.getCurrencyId();
        if (currencyId == null) {
            return ResponseBuilderV2.apiError(ApiErrors.MISSING_PARAM, "currencyId").build();
        }
        if (body.getBurningAmount() == null) {
            return ResponseBuilderV2.apiError(ApiErrors.MISSING_PARAM, "burningAmount").build();
        }
        long currencyIdLong = Long.parseUnsignedLong(currencyId);
        AccountCurrency accountCurrency = accountCurrencyService.getAccountCurrency(senderAccountId, currencyIdLong);
        if (accountCurrency == null || accountCurrency.getUnconfirmedUnits() < body.getBurningAmount()) {
            return ResponseBuilderV2.apiError(ApiErrors.NOT_ENOUGH_FUNDS, "currency").build();
        }

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .version(1)
            .attachment(new MonetarySystemCurrencyBurningAttachment(currencyIdLong, body.getBurningAmount()))
            .senderAccount(senderAccount)
            .publicKey(Convert.parseHexString(body.getPublicKey()))
            .recipientId(0)
            .deadlineValue(String.valueOf(body.getDeadline()))
            .amountATM(0)
            .feeATM(body.getFee())
            .broadcast(body.isBroadcast() == null || body.isBroadcast())
            .validate(true)
            .passphrase(body.getPassphrase())
            .secretPhrase(body.getSecretPhrase())
            .build();

        TransactionCreationResponse apiV2TransactionResponse = transactionCreator.createApiV2Transaction(txRequest);

        return builder.bind(apiV2TransactionResponse).build();
    }
}
