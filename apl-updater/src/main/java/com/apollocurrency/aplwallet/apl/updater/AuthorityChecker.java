/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.Certificate;

public interface AuthorityChecker {

    boolean verifyCertificates(String certificateDirectory);

    void verifyJarSignature(Certificate certificate, Path jarFilePath) throws IOException;

    boolean verifyJarSignature(String certificateDirectory, Path jarFilePath);
}
