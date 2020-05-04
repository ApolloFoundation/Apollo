/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

/**
 * Error codes for communication channel
 *
 * @author alukin@gmail.com
 */
public class SvBusErrorCodes {

    public static final int OK = 0;
    public static final int NOT_CONNECTED = 1;
    public static final int NO_ROUTE = 2;
    public static final int NO_HANDLER = 3;
    public static final int PROCESSING_ERROR = 4;
    public static final int INVALID_HEADER = 5;
    public static final int INVALID_MESSAGE = 6;
    public static final int INVALID_BODY = 7;
    public static final int RESPONSE_TIMEOUT = 8;
}
