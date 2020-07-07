package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.InfoApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
public class InfoApiServiceImpl implements InfoApiService {
      public Response getHealthInfo(SecurityContext securityContext)
      throws NotFoundException {
      // do some magic!
      return Response.ok().build();
  }
}
