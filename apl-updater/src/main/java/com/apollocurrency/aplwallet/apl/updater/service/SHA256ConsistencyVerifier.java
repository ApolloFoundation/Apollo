/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.service;

import com.apollocurrency.aplwallet.apl.updater.ConsistencyVerifier;
import com.apollocurrency.aplwallet.apl.util.Convert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256ConsistencyVerifier implements ConsistencyVerifier {

    private byte[] calculateHash(Path file) throws IOException, NoSuchAlgorithmException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return digest.digest();
        }
    }

    @Override
    public boolean verify(Path file, byte[] hash) {
        try {
            byte[] actualHash = calculateHash(file);
            return Convert.toHexString(actualHash).equalsIgnoreCase(Convert.toHexString(hash));
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
