/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Builder
public class Task implements TaskAttributes {

    private Runnable task;
    private String name;
    private String group;
    private boolean daemon;
    private boolean recurring;

}
