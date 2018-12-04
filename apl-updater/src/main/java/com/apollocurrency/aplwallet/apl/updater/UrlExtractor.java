/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.util.regex.Pattern;

public interface UrlExtractor {
    String extract(byte[] encryptedUrlBytes, Pattern urlPattern);
}
