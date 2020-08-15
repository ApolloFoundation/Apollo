package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.*;
import com.apollocurrency.aplwallet.api.v2.model.*;

import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.ListResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.api.v2.model.TxRequest;

import java.util.List;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public interface TransactionApiService {
      Response broadcastTx(TxRequest body,SecurityContext securityContext)
      throws NotFoundException;
      Response broadcastTxBatch(java.util.List<TxRequest> body,SecurityContext securityContext)
      throws NotFoundException;
      Response getTxById(String transaction,SecurityContext securityContext)
      throws NotFoundException;
}
