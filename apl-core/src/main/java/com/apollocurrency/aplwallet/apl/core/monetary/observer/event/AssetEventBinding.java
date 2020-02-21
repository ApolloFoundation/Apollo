/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.observer.event;

import com.apollocurrency.aplwallet.apl.core.monetary.observer.AssetEventType;

import javax.enterprise.util.AnnotationLiteral;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class AssetEventBinding extends AnnotationLiteral<AssetEvent> implements AssetEvent{

    public static AnnotationLiteral<AssetEvent> literal(final AssetEventType assetEventType) {
        return new AssetEventBinding() {
            @Override
            public AssetEventType value() {
                return assetEventType;
            }
        };
    }

}
