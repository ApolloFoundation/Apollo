/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

public class CsvExportException extends CsvException {
    public CsvExportException() {
    }

    public CsvExportException(String message) {
        super(message);
    }

    public CsvExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
