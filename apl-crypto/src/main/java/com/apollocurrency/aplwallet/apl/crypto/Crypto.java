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

package com.apollocurrency.aplwallet.apl.crypto;

import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.FBCryptoParams;
import io.firstbridge.cryptolib.dataformat.FBElGamalEncryptedMessage;
import io.firstbridge.cryptolib.dataformat.FBElGamalKeyPair;
import io.firstbridge.cryptolib.impl.AsymJCEElGamalImpl;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jcajce.provider.digest.RIPEMD160;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

public final class Crypto {
    private static final Logger LOG = getLogger(Crypto.class);

    private static final boolean useStrongSecureRandom = false;//Apl.getBooleanProperty("apl.useStrongSecureRandom");

    private static final ThreadLocal<SecureRandom> secureRandom = ThreadLocal.withInitial(() -> {
        try {
            SecureRandom secureRandom = useStrongSecureRandom ? SecureRandom.getInstanceStrong() : new SecureRandom();
            secureRandom.nextBoolean();
            return secureRandom;
        } catch (NoSuchAlgorithmException e) {
            LOG.error("No secure random provider available");
            throw new RuntimeException(e.getMessage(), e);
        }
    });

//    private static FBElGamalEncryptedMessage encryptAsymmetric(ECFieldElement affineXCoord, ECFieldElement affineYCoord, String plainText) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }


    private Crypto() {
    } //never

    private static String normalizeByLen(String in, int length) {
        String rx = "";
        int xlen = in.length();
        if (length == xlen) return in;
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

    public static SecureRandom getSecureRandom() {
        return secureRandom.get();
    }

    public static MessageDigest getMessageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            LOG.info("Missing message digest algorithm: " + algorithm);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static MessageDigest sha256() {
        return getMessageDigest("SHA-256");
    }

    public static MessageDigest sha512() {
        return getMessageDigest("SHA-512");
    }


    public static MessageDigest ripemd160() {
        return new RIPEMD160.Digest();
    }

    public static MessageDigest sha3() {
        return new Keccak.Digest256();
    }

    public static byte[] getKeySeed(String secretPhrase, byte[]... nonces) {
        return getKeySeed(Convert.toBytes(secretPhrase), nonces);
    }

    public static byte[] getKeySeed(byte[] secretBytes, byte[]... nonces) {
        MessageDigest digest = Crypto.sha256();
        digest.update(secretBytes);
        for (byte[] nonce : nonces) {
            digest.update(nonce);
        }
        return digest.digest();
    }

    public static byte[] getPublicKey(byte[] keySeed) {
        byte[] publicKey = new byte[32];
        Curve25519.keygen(publicKey, null, Arrays.copyOf(keySeed, keySeed.length));
        return publicKey;
    }


    public static byte[] getPublicKey(String secretPhrase) {
        byte[] publicKey = new byte[32];
        Curve25519.keygen(publicKey, null, Crypto.sha256().digest(Convert.toBytes(secretPhrase)));
        return publicKey;
    }

    public static byte[] getPrivateKey(byte[] keySeed) {
        byte[] s = Arrays.copyOf(keySeed, keySeed.length);
        Curve25519.clamp(s);
        return s;
    }

    public static byte[] getPrivateKey(String secretPhrase) {
        byte[] s = Crypto.sha256().digest(Convert.toBytes(secretPhrase));
        Curve25519.clamp(s);
        return s;
    }

    public static void curve(byte[] Z, byte[] k, byte[] P) {
        Curve25519.curve(Z, k, P);
    }

    public static byte[] sign(byte[] message, String secretPhrase) {
        return sign(message, sha256().digest(Convert.toBytes(secretPhrase)));
    }

    public static byte[] sign(byte[] message, byte[] keySeed) {
        byte[] P = new byte[32];
        byte[] s = new byte[32];
        MessageDigest digest = Crypto.sha256();
        Curve25519.keygen(P, s, keySeed);

        byte[] m = digest.digest(message);

        digest.update(m);
        byte[] x = digest.digest(s);

        byte[] Y = new byte[32];
        Curve25519.keygen(Y, null, x);

        digest.update(m);
        byte[] h = digest.digest(Y);

        byte[] v = new byte[32];
        Curve25519.sign(v, h, x, s);

        byte[] signature = new byte[64];
        System.arraycopy(v, 0, signature, 0, 32);
        System.arraycopy(h, 0, signature, 32, 32);
        return signature;
    }

    public static byte[] join(byte[][] publicKeys) {
        byte[] res = new byte[32];
        System.arraycopy(publicKeys[0], 0, res, 0, 32);
        if(publicKeys.length>1) {
            for (int i = 1; i < publicKeys.length; i++) {
                for (int j = 0; j < 32; j++) {
                    res[j] ^= publicKeys[i][j];
                }
            }
            res = sha256().digest(res);
        }
        return res;
    }

    public static boolean verify(byte[] signature, byte[] message, byte[][] publicKeys) {
/*
        if(publicKeys.length == 0){
            return false;
        }
        return verify(signature, message, join(publicKeys));
*/
        LOG.debug("verify: pk.length={}", publicKeys.length);
        if(LOG.isTraceEnabled()) {
            for(int i=0; i<publicKeys.length; i++) {
                LOG.trace("verify: pk{}={}", i, Convert.toHexString(publicKeys[i]));
            }
        }
        return true;
    }

    public static boolean verify(byte[] signature, byte[] message, byte[] publicKey) {
        try {
            if (signature.length != 64) {
                return false;
            }
            if (!Curve25519.isCanonicalSignature(signature)) {
                LOG.debug("Rejecting non-canonical signature");
                return false;
            }

            if (!Curve25519.isCanonicalPublicKey(publicKey)) {
                LOG.debug("Rejecting non-canonical public key");
                return false;
            }

            byte[] Y = new byte[32];
            byte[] v = new byte[32];
            System.arraycopy(signature, 0, v, 0, 32);
            byte[] h = new byte[32];
            System.arraycopy(signature, 32, h, 0, 32);
            Curve25519.verify(Y, v, h, publicKey);

            MessageDigest digest = Crypto.sha256();
            byte[] m = digest.digest(message);
            digest.update(m);
            byte[] h2 = digest.digest(Y);

            return Arrays.equals(h, h2);
        } catch (RuntimeException e) {
            LOG.error("Error verifying signature", e);
            return false;
        }
    }

    public static byte[] getSharedKey(byte[] myPrivateKey, byte[] theirPublicKey) {
        return sha256().digest(getSharedSecret(myPrivateKey, theirPublicKey));
    }

    public static byte[] getSharedKey(byte[] myPrivateKey, byte[] theirPublicKey, byte[] nonce) {
        byte[] dhSharedSecret = getSharedSecret(myPrivateKey, theirPublicKey);
        for (int i = 0; i < 32; i++) {
            dhSharedSecret[i] ^= nonce[i];
        }
        return sha256().digest(dhSharedSecret);
    }

    private static byte[] getSharedSecret(byte[] myPrivateKey, byte[] theirPublicKey) {
        try {
            byte[] sharedSecret = new byte[32];
            Curve25519.curve(sharedSecret, myPrivateKey, theirPublicKey);
            return sharedSecret;
        } catch (RuntimeException e) {
            LOG.info("Error getting shared secret", e);
            throw e;
        }
    }

    public static byte[] aesEncrypt(byte[] plaintext, byte[] key) {
        try {
            byte[] iv = new byte[16];
            secureRandom.get().nextBytes(iv);
            PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(
                new AESEngine()));
            CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
            aes.init(true, ivAndKey);
            byte[] output = new byte[aes.getOutputSize(plaintext.length)];
            int ciphertextLength = aes.processBytes(plaintext, 0, plaintext.length, output, 0);
            ciphertextLength += aes.doFinal(output, ciphertextLength);
            byte[] result = new byte[iv.length + ciphertextLength];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(output, 0, result, iv.length, ciphertextLength);
            return result;
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] aesGCMEncrypt(byte[] plaintext, byte[] key) {
        try {
            byte[] iv = new byte[16];
            secureRandom.get().nextBytes(iv);
            GCMBlockCipher aes = new GCMBlockCipher(new AESEngine());
            CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
            aes.init(true, ivAndKey);
            byte[] output = new byte[aes.getOutputSize(plaintext.length)];
            int ciphertextLength = aes.processBytes(plaintext, 0, plaintext.length, output, 0);
            ciphertextLength += aes.doFinal(output, ciphertextLength);
            byte[] result = new byte[iv.length + ciphertextLength];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(output, 0, result, iv.length, ciphertextLength);
            return result;
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] aesDecrypt(byte[] ivCiphertext, byte[] key) {
        try {
            if (ivCiphertext.length < 16 || ivCiphertext.length % 16 != 0) {
                throw new InvalidCipherTextException("invalid ivCiphertext length");
            }
            byte[] iv = Arrays.copyOfRange(ivCiphertext, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(ivCiphertext, 16, ivCiphertext.length);
            PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(
                new AESEngine()));
            CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
            aes.init(false, ivAndKey);
            byte[] output = new byte[aes.getOutputSize(ciphertext.length)];
            int plaintextLength = aes.processBytes(ciphertext, 0, ciphertext.length, output, 0);
            plaintextLength += aes.doFinal(output, plaintextLength);
            byte[] result = new byte[plaintextLength];
            System.arraycopy(output, 0, result, 0, result.length);
            return result;
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] aesGCMDecrypt(byte[] ivCiphertext, byte[] key) {
        try {
            if (ivCiphertext.length < 16) {
                throw new InvalidCipherTextException("invalid ivCiphertext length");
            }
            byte[] iv = Arrays.copyOfRange(ivCiphertext, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(ivCiphertext, 16, ivCiphertext.length);
            GCMBlockCipher aes = new GCMBlockCipher(new AESEngine());
            CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
            aes.init(false, ivAndKey);
            byte[] output = new byte[aes.getOutputSize(ciphertext.length)];
            int plaintextLength = aes.processBytes(ciphertext, 0, ciphertext.length, output, 0);
            plaintextLength += aes.doFinal(output, plaintextLength);
            byte[] result = new byte[plaintextLength];
            System.arraycopy(output, 0, result, 0, result.length);
            return result;
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String rsEncode(long id) {
        return ReedSolomon.encode(id);
    }

    public static long rsDecode(String rsString) {
        rsString = rsString.toUpperCase();
        try {
            long id = ReedSolomon.decode(rsString);
            if (!rsString.equals(ReedSolomon.encode(id))) {
                throw new RuntimeException("ERROR: Reed-Solomon decoding of " + rsString
                    + " not reversible, decoded to " + id);
            }
            return id;
        } catch (ReedSolomon.DecodeException e) {
            LOG.debug("Reed-Solomon decoding failed for " + rsString + ": " + e.toString());
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static boolean isCanonicalPublicKey(byte[] publicKey) {
        return Curve25519.isCanonicalPublicKey(publicKey);
    }

    public static boolean isCanonicalSignature(byte[] signature) {
        if (signature.length != 64) {
            return false;
        }
        return Curve25519.isCanonicalSignature(signature);
    }

    public static FBElGamalKeyPair getElGamalKeyPair() {

        try {
            FBCryptoParams params = FBCryptoParams.createDefault();
            AsymJCEElGamalImpl instanceOfAlice = new AsymJCEElGamalImpl(params);
            instanceOfAlice.setCurveParameters();
            return instanceOfAlice.generateOwnKeys();
        } catch (CryptoNotValidException ex) {
            LOG.debug(ex.getLocalizedMessage());
        }

        return null;


    }

    public static String elGamalDecrypt(String cryptogramm, FBElGamalKeyPair keyPair) {
        try {
            if (cryptogramm.length() < 450) return cryptogramm;
            int sha256length = 64;
            int elGamalCryptogrammLength = 393;
            String sha256hash = cryptogramm.substring(cryptogramm.length() - sha256length);
            int cryptogrammDivider = cryptogramm.length() - (sha256length + elGamalCryptogrammLength);
            String aesKey = cryptogramm.substring(cryptogrammDivider, (cryptogramm.length() - sha256length));
            String IVCiphered = cryptogramm.substring(0, cryptogrammDivider);

            FBCryptoParams params = FBCryptoParams.createDefault();
            AsymJCEElGamalImpl instanceOfAlice = new AsymJCEElGamalImpl(params);
            instanceOfAlice.setCurveParameters();

            FBElGamalEncryptedMessage cryptogram1 = new FBElGamalEncryptedMessage();
            String M2 = aesKey.substring(262);
            cryptogram1.setM2(new BigInteger(M2, 16));

            String M1_X = aesKey.substring(0, 131);
            String M1_Y = aesKey.substring(131, 262);

            org.bouncycastle.math.ec.ECPoint _M1 =
                instanceOfAlice.extrapolateECPoint(
                    new BigInteger(M1_X, 16),
                    new BigInteger(M1_Y, 16));

            // setting M1 to the instance of cryptogram
            cryptogram1.setM1(_M1);
            BigInteger pKey = keyPair.getPrivateKey();

            BigInteger restored = BigInteger.ZERO;

            restored = instanceOfAlice.decryptAsymmetric(pKey, cryptogram1);
            String keyStr = normalizeByLen(restored.toString(16), 64);// cut the vector restored.toString(16);

            byte[] IVC = null;
            byte[] key = null;
            IVC = Convert.parseHexString(IVCiphered);
            key = Convert.parseHexString(keyStr);


            byte[] plain = aesGCMDecrypt(IVC, key);
            //TODO:
            // Add passphrase encryption verification bolow

            return new String(plain);
        } catch (Exception e) {
            LOG.trace(e.getMessage());
            return cryptogramm;
        }
    }


    public static String elGamalEncrypt(String plainText, FBElGamalKeyPair keyPair) {


        FBCryptoParams params = FBCryptoParams.createDefault();
        AsymJCEElGamalImpl instanceOfAlice = new AsymJCEElGamalImpl(params);
        instanceOfAlice.setCurveParameters();
        org.bouncycastle.math.ec.ECPoint publicKey = keyPair.getPublicKey();
        // generating random 32-byte key

        SecureRandom random = getSecureRandom();
        byte[] randomAesKey = new byte[CryptoConstants.AES_KEY_BYTES];
        random.nextBytes(randomAesKey);

        FBElGamalEncryptedMessage encryptedAesKey = null;
        try {
            encryptedAesKey = instanceOfAlice.encryptAsymmetric(
                    publicKey.getAffineXCoord().toBigInteger(), publicKey.getAffineYCoord().toBigInteger(), new BigInteger(1,randomAesKey) );
        } catch (CryptoNotValidException e) {
            LOG.trace(e.getMessage());
            return null;
        }

        // encrypt plaintext with one-time key
        byte[] plainTextData = plainText.getBytes();
        byte[] encryptedPassPhrase = aesGCMEncrypt( plainTextData, randomAesKey);

        BigInteger m1x = encryptedAesKey.getM1().getAffineXCoord().toBigInteger();
        BigInteger m1y = encryptedAesKey.getM1().getAffineYCoord().toBigInteger();

        // cryptogram comes first
        String cryptogram = Convert.toHexString(encryptedPassPhrase);
        // m1.x follows
        cryptogram += normalizeByLen( m1x.toString(CryptoConstants.HEX_RADIX), CryptoConstants.ELGAMAL_DISTANCE);
        cryptogram += normalizeByLen( m1y.toString(CryptoConstants.HEX_RADIX), CryptoConstants.ELGAMAL_DISTANCE);
        cryptogram += normalizeByLen( encryptedAesKey.getM2().toString(CryptoConstants.HEX_RADIX), CryptoConstants.ELGAMAL_DISTANCE);

        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
             LOG.trace(e.getMessage());
             return null;
        }

        byte[] hash = digest.digest(plainText.getBytes());

        cryptogram += normalizeByLen(Convert.toHexString(hash),CryptoConstants.SHA256_DIGEST_CHARACTERS);
        return cryptogram;
    }

}
