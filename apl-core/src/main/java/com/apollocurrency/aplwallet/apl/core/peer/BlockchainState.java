/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

/**
 * @author alukin@gmail.com
 */
public enum BlockchainState {
    UP_TO_DATE,
    DOWNLOADING,
    LIGHT_CLIENT,
    FORK
}
