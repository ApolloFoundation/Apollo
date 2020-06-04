/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
@Singleton
public class UtilComponentConfig {
    @Produces
    @Singleton
    public Zip zip() {
        return new ZipImpl();
    }

    @Produces
    @Singleton
    public UPnP uPnP() {
        return new UPnP();
    }

}
