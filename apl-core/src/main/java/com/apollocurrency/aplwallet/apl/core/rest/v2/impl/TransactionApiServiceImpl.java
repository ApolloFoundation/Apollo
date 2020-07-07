package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.TransactionApiService;
import com.apollocurrency.aplwallet.api.v2.model.TxRequest;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@RequestScoped
public class TransactionApiServiceImpl implements TransactionApiService {
      public Response broadcastTx(TxRequest body,SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
      public Response broadcastTxBatch(List<TxRequest> body, SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
      public Response getTxById(String transaction,SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
}
