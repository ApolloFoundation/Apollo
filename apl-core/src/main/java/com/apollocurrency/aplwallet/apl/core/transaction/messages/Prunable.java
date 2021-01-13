/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

public interface Prunable {

    byte[] getHash();

    boolean hasPrunableData();

}
