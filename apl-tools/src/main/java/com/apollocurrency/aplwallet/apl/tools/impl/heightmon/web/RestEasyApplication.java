/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.web;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/rest")
public class RestEasyApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {

        HashSet<Class<?>> set = new HashSet<>();
        set.add(NetStatController.class);
        return set;
    }
}
