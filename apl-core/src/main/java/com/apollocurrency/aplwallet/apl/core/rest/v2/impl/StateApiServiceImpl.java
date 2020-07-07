package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.StateApiService;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@RequestScoped
public class StateApiServiceImpl implements StateApiService {
      public Response getBlockByHeight(String height,SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
      public Response getBlockById(String block,SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
      public Response getBlockchainInfo(SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
      public Response getTxReceiptById(String transaction,SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
      public Response getTxReceiptList(List<String> body, SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
      public Response getUnconfirmedTx(Integer page,Integer perPage,SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
      public Response getUnconfirmedTxCount(SecurityContext securityContext)
      throws NotFoundException {
      return Response.ok().build();
  }
}
