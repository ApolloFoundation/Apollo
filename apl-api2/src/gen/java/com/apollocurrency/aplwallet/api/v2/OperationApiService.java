package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.*;
import com.apollocurrency.aplwallet.api.v2.model.*;

import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.QueryCountResult;
import com.apollocurrency.aplwallet.api.v2.model.QueryObject;
import com.apollocurrency.aplwallet.api.v2.model.QueryResult;

import java.util.List;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public interface OperationApiService {
      Response getOperations(QueryObject body,SecurityContext securityContext)
      throws NotFoundException;
      Response getOperationsCount(QueryObject body,SecurityContext securityContext)
      throws NotFoundException;
}
