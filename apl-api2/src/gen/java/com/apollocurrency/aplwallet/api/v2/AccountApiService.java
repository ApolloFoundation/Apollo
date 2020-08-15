package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.*;
import com.apollocurrency.aplwallet.api.v2.model.*;

import com.apollocurrency.aplwallet.api.v2.model.AccountInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.AccountReq;
import com.apollocurrency.aplwallet.api.v2.model.AccountReqSendMoney;
import com.apollocurrency.aplwallet.api.v2.model.AccountReqTest;
import com.apollocurrency.aplwallet.api.v2.model.CreateChildAccountResp;
import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;

import java.util.List;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public interface AccountApiService {
      Response createChildAccountTx(AccountReq body,SecurityContext securityContext)
      throws NotFoundException;
      Response createChildAccountTxSendMony(AccountReqSendMoney body,SecurityContext securityContext)
      throws NotFoundException;
      Response createChildAccountTxTest(AccountReqTest body,SecurityContext securityContext)
      throws NotFoundException;
      Response getAccountInfo(String account,SecurityContext securityContext)
      throws NotFoundException;
}
