/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.account;

import lombok.Getter;

public enum AddressScope {
    EXTERNAL(0),//any address
    IN_FAMILY(1),//only accounts with the same parent
    CUSTOM(2);//there is an address list in a special table

    @Getter
    private final byte code;
    AddressScope(int code) {
        this.code = (byte) code;
    }

    public static AddressScope from(int code){
        for(AddressScope item: AddressScope.values()){
            if(item.code==code){
                return item;
            }
        }
        throw new IllegalArgumentException("There is no AddressScope constant with code "+code);
    }
}
