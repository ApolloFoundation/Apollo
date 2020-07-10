package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.AccountApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.model.AccountInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.AccountReq;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.AccountInfoMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Objects;

@RequestScoped
public class AccountApiServiceImpl implements AccountApiService {

    private final AccountService accountService;
    private final AccountInfoMapper accountInfoMapper;

    @Inject
    public AccountApiServiceImpl(AccountService accountService, AccountInfoMapper accountInfoMapper) {
        this.accountService = Objects.requireNonNull(accountService);
        this.accountInfoMapper = Objects.requireNonNull(accountInfoMapper);
    }

    @Override
    public Response createChildAccountTx(AccountReq body, SecurityContext securityContext) throws NotFoundException {
        return ResponseBuilderV2.apiError(ApiErrors.CUSTOM_ERROR_MESSAGE, "Not implemented yet, work in progress.").build();
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
