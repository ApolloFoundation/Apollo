/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

/**
 * MemoryAppender maintains a ring buffer of log messages.  The GetLog API is used
 * to retrieve these log messages.
 *
 * The following logging.properties entries are used:
 * <ul>
 * <li>com.apollocurrency.aplwallet.apl.util.MemoryAppender.size (default 100, minimum 10)</li>
 * </ul>
 */
public class MemoryAppender extends AppenderBase<ILoggingEvent> {

    /** Default ring buffer size */
    private static final int DEFAULT_SIZE = 100;

    private static final int MAX_SIZE = 10_000;

    private static final int MIN_SIZE = 10;

    /** Ring buffer */
    private ILoggingEvent[] buffer;

    private int size = DEFAULT_SIZE;

    /** Buffer start */
    private int start = 0;

    /** Number of buffer entries */
    private int count = 0;

    public MemoryAppender() {
    }

    public void setSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            this.size = DEFAULT_SIZE;
        } else {
            this.size = size;
        }
            buffer = new ILoggingEvent[this.size];
    }

    /**
     * Store a ILoggingEvent in the ring buffer
     *
     * @param   iLoggingEvent              Description of the log event.
     */
    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
            int ix = (start+count)%buffer.length;
            buffer[ix] = iLoggingEvent;
            if (count < buffer.length) {
                count++;
            } else {
                start++;
                start %= buffer.length;
            }
    }


    /**
     * Return the log messages from the ring buffer
     *
     * @param   msgCount            Number of messages to return
     * @return                      List of log messages
     */
    public synchronized List<String> getMessages(int msgCount) {
        List<String> rtnList = new ArrayList<>(buffer.length);
            int rtnSize = Math.min(msgCount, count);
            int pos = (start + (count-rtnSize))%buffer.length;
            for (int i=0; i<rtnSize; i++) {
                rtnList.add(buffer[pos++].getFormattedMessage());
                if (pos == buffer.length)
                    pos = 0;
            }
        return rtnList;
    }
}
