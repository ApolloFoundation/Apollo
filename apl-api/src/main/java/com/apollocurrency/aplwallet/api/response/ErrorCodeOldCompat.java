package com.apollocurrency.aplwallet.api.response;

/**
 * Integer error codes, compatible wij old implementation.
 * Those error codes used khaotically with quite different descriptions
 * so there's no sense to keep them.
 * Should be gone after transition to new API.
 * @author alukin@gmail.com
 */
public class ErrorCodeOldCompat {
    public static final int PARAMETER_MISSIG = 3;
    public static final int PARAMETER_INCORRECT = 4;
    public static final int PARAMETER_UNKNOWN = 5;
    public static final int PARAMETER_NOT_FOUND = 10;
    
    
}
