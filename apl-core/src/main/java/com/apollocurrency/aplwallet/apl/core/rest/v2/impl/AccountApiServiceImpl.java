package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.AccountApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.model.AccountInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.AccountReq;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
public class AccountApiServiceImpl implements AccountApiService {

    @Override
    public Response createChildAccountTx(AccountReq body, SecurityContext securityContext) throws NotFoundException {
        //ResponseBuilder response = ResponseBuilder.startTiming();
        String pk = body.getChildPublicKeyList().get(0);
        byte[] publicKey = Convert.parseHexString(pk);
        long accountId = Convert.getId(publicKey);

        AccountInfoResp basicAccount = new AccountInfoResp();
        basicAccount.setPublicKey(pk);
        basicAccount.setAccount(Convert.defaultRsAccount(accountId));

        return Response.ok().entity(basicAccount).build();
    }

    public Response getAccountInfo(String account, SecurityContext securityContext)
      throws NotFoundException {
        AccountInfoResp basicAccount = new AccountInfoResp();
          basicAccount.setAccount(account);
      return Response.ok().entity(basicAccount).build();
  }
}
