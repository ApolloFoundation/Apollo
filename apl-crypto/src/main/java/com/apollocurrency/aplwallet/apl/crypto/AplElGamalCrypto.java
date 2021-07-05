/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.crypto;

import io.firstbridge.cryptolib.CryptoConfig;
import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.CryptoParams;
import io.firstbridge.cryptolib.ElGamalCrypto;
import io.firstbridge.cryptolib.ElGamalKeyPair;
import io.firstbridge.cryptolib.dataformat.ElGamalEncryptedMessage;
import io.firstbridge.cryptolib.impl.ecc.ElGamalCryptoImpl;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static com.apollocurrency.aplwallet.apl.crypto.Crypto.aesGCMDecrypt;
import static com.apollocurrency.aplwallet.apl.crypto.Crypto.aesGCMEncrypt;
import static com.apollocurrency.aplwallet.apl.crypto.Crypto.getSecureRandom;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * El Gamal routines for Apollo
 * @author alukin@gmail.com
 */
public class AplElGamalCrypto {
 private static final Logger LOG = getLogger(AplElGamalCrypto.class);

    private static String normalizeByLen(String in, int length) {
        String rx = "";
        int xlen = in.length();
        if (length == xlen) {
            return in;
        }
        if (length > xlen) {
            for (int i = 0; i < length - xlen; i++) {
                rx += "0";
            }
            rx += in;
            return rx;
        } else { // length < xlen // cut the vector
            return in.substring(xlen - length, length + 1);
        }
    }

    public static String decrypt(String cryptogramm, ElGamalKeyPair keyPair) {
        try {
            if (cryptogramm.length() < 450) {
                return cryptogramm;
            }
            int sha256length = 64;
            int elGamalCryptogrammLength = 393;
            String sha256hash = cryptogramm.substring(cryptogramm.length() - sha256length);
            int cryptogrammDivider = cryptogramm.length() - (sha256length + elGamalCryptogrammLength);
            String aesKey = cryptogramm.substring(cryptogrammDivider, (cryptogramm.length() - sha256length));
            String IVCiphered = cryptogramm.substring(0, cryptogrammDivider);

            CryptoParams params = CryptoConfig.createDefaultParams();
            ElGamalCrypto instanceOfAlice = new ElGamalCryptoImpl(params);

            ElGamalEncryptedMessage cryptogram1 = new ElGamalEncryptedMessage();
            String M2 = aesKey.substring(262);
            cryptogram1.setM2(new BigInteger(M2, 16));

            String M1_X = aesKey.substring(0, 131);
            String M1_Y = aesKey.substring(131, 262);

            //TODO:  this must be changed:  either put in interface of hide
            org.bouncycastle.math.ec.ECPoint _M1
                    = ((ElGamalCryptoImpl) instanceOfAlice).extrapolateECPoint(
                            new BigInteger(M1_X, 16),
                            new BigInteger(M1_Y, 16));

            // setting M1 to the instance of cryptogram
            cryptogram1.setM1(_M1);
            BigInteger pKey = keyPair.getPrivateKey();

            BigInteger restored = BigInteger.ZERO;

            restored = instanceOfAlice.decrypt(pKey, cryptogram1);
            // cut the vector restored.toString(16);
            String keyStr = normalizeByLen(restored.toString(16), 64);

            byte[] IVC = null;
            byte[] key = null;
            IVC = Convert.parseHexString(IVCiphered);
            key = Convert.parseHexString(keyStr);

            byte[] plain = aesGCMDecrypt(IVC, key);
            
            String cryptogramBody = cryptogram.substring(0, 393);
            
            
            return new String(plain);
        } catch (Exception e) {
            LOG.trace(e.getMessage());
            return cryptogramm;
        }
    }

    public static String encrypt(String plainText, ElGamalKeyPair keyPair) {

        CryptoParams params = CryptoConfig.createDefaultParams();
        ElGamalCrypto instanceOfAlice = new ElGamalCryptoImpl(params);
        org.bouncycastle.math.ec.ECPoint publicKey = keyPair.getPublicKey();
        // generating random 32-byte key

        SecureRandom random = getSecureRandom();
        byte[] randomAesKey = new byte[CryptoConstants.AES_KEY_BYTES];
        random.nextBytes(randomAesKey);

        ElGamalEncryptedMessage encryptedAesKey = null;
        try {
            encryptedAesKey = instanceOfAlice.encrypt(
                    publicKey.getAffineXCoord().toBigInteger(), publicKey.getAffineYCoord().toBigInteger(), new BigInteger(1, randomAesKey));
        } catch (CryptoNotValidException e) {
            LOG.trace(e.getMessage());
            return null;
        }

        // encrypt plaintext with one-time key
        byte[] plainTextData = plainText.getBytes();
        byte[] encryptedPassPhrase = aesGCMEncrypt(plainTextData, randomAesKey);

        BigInteger m1x = encryptedAesKey.getM1().getAffineXCoord().toBigInteger();
        BigInteger m1y = encryptedAesKey.getM1().getAffineYCoord().toBigInteger();

        // cryptogram comes first
        String cryptogram = Convert.toHexString(encryptedPassPhrase);
        // m1.x follows
        cryptogram += normalizeByLen(m1x.toString(CryptoConstants.HEX_RADIX), CryptoConstants.ELGAMAL_DISTANCE);
        cryptogram += normalizeByLen(m1y.toString(CryptoConstants.HEX_RADIX), CryptoConstants.ELGAMAL_DISTANCE);
        cryptogram += normalizeByLen(encryptedAesKey.getM2().toString(CryptoConstants.HEX_RADIX), CryptoConstants.ELGAMAL_DISTANCE);

        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOG.trace(e.getMessage());
            return null;
        }

        byte[] hash = digest.digest(plainText.getBytes());

        cryptogram += normalizeByLen(Convert.toHexString(hash), CryptoConstants.SHA256_DIGEST_CHARACTERS);
        return cryptogram;
    }

}
