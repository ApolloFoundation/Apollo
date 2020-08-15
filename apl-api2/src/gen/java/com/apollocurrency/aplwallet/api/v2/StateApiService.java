package com.apollocurrency.aplwallet.api.v2;

import com.apollocurrency.aplwallet.api.v2.*;
import com.apollocurrency.aplwallet.api.v2.model.*;

import com.apollocurrency.aplwallet.api.v2.model.BlockInfo;
import com.apollocurrency.aplwallet.api.v2.model.BlockchainInfo;
import com.apollocurrency.aplwallet.api.v2.model.CountResponse;
import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionInfoArrayResp;
import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;

import java.util.List;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public interface StateApiService {
      Response getBlockByHeight(String height,SecurityContext securityContext)
      throws NotFoundException;
      Response getBlockById(String block,SecurityContext securityContext)
      throws NotFoundException;
      Response getBlockchainInfo(SecurityContext securityContext)
      throws NotFoundException;
      Response getTxReceiptById(String transaction,SecurityContext securityContext)
      throws NotFoundException;
      Response getTxReceiptList(java.util.List<String> body,SecurityContext securityContext)
      throws NotFoundException;
      Response getUnconfirmedTx(Integer page,Integer perPage,SecurityContext securityContext)
      throws NotFoundException;
      Response getUnconfirmedTxCount(SecurityContext securityContext)
      throws NotFoundException;
}
