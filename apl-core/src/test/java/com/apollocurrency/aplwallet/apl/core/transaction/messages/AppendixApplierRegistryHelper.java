/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class AppendixApplierRegistryHelper {

    public static void addApplier(AppendixApplier applier, AppendixApplierRegistry registry) {
        registry.addApplier(applier);
    }

}
