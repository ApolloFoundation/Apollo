package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.*;
import com.apollocurrency.aplwallet.api.v2.model.*;

import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.HealthResponse;

import java.util.List;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public interface InfoApiService {
      Response getHealthInfo(SecurityContext securityContext)
      throws NotFoundException;
}
