package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.exchange.model.UserErrorMessage;

public class UserErrorMessageTestData {
    public final UserErrorMessage ERROR_1 = new UserErrorMessage(100L,            "0x0398E119419E0D7792c53913d3f370f9202Ae137", "Invalid transaction", 1000);
    public final UserErrorMessage ERROR_2 = new UserErrorMessage(200L,            "0x8e96e98b32c56115614B64704bA35feFE9e8f7bC", "Out of gas"         , 1100);
    public final UserErrorMessage ERROR_3 = new UserErrorMessage(300L,            "0x0398E119419E0D7792c53913d3f370f9202Ae137", "Double spending"    , 1200);

    public final UserErrorMessage NEW_ERROR = new UserErrorMessage(301L,            "0x0398E119419E0D7792c53913d3f370f9202Ae137", "No enought funds"    , 1300);
}
