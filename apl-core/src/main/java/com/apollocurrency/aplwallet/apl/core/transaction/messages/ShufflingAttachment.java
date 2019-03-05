/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

/**
 *
 * @author al
 */
public interface ShufflingAttachment extends Attachment {

    long getShufflingId();

    byte[] getShufflingStateHash();
    
}
