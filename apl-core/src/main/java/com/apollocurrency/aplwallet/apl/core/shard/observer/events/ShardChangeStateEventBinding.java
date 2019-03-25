/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer.events;

import javax.enterprise.util.AnnotationLiteral;

public abstract class ShardChangeStateEventBinding
        extends AnnotationLiteral<ShardChangeStateEvent>
        implements ShardChangeStateEvent {
}
