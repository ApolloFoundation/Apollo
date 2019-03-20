package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.OrderDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetOpenOrderResponse extends ResponseBase{
    public OrderDTO[] openOrderDTOS;
    public OrderDTO[] askOrders;
    public OrderDTO[] openOrders;
}
