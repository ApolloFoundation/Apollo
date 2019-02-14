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

import io.firstbridge.cryptolib.impl.AsymJCEElGamalImpl;
import io.firstbridge.cryptolib.FBCryptoParams;
import io.firstbridge.cryptolib.dataformat.FBElGamalEncryptedMessage;
import io.firstbridge.cryptolib.dataformat.FBElGamalKeyPair;
import io.firstbridge.cryptolib.exception.CryptoNotValidException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.slf4j.LoggerFactory.getLogger;

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Level;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECFieldElement;

public final class Crypto {
        private static final Logger LOG = getLogger(Crypto.class);

    private static final boolean useStrongSecureRandom = false;//Apl.getBooleanProperty("apl.useStrongSecureRandom");

    private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            try {
                SecureRandom secureRandom = useStrongSecureRandom ? SecureRandom.getInstanceStrong() : new SecureRandom();
                secureRandom.nextBoolean();
                return secureRandom;
            } catch (NoSuchAlgorithmException e) {
                LOG.error("No secure random provider available");
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    };

    private Crypto() {} //never

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
            if (! rsString.equals(ReedSolomon.encode(id))) {
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
    
    public static String elGamalDecrypt(String cryptogramm, FBElGamalKeyPair keyPair)
    {
        
        /*int sha256length = 64;
        int elGamalCryptogrammLength = 132;
        int cryptogrammDivider = cryptogramm.length() - (sha256length + elGamalCryptogrammLength);
        String aesKey = cryptogramm.substring(cryptogrammDivider);
        byte[] aesKeyArray = new BigInteger(aesKey, 16).toByteArray();
        String aesCryptogramm = cryptogramm.substring(0, cryptogrammDivider);
        byte[] aesCryptogrammArray = new BigInteger(aesCryptogramm, 16).toByteArray();
        */
        LOG.debug("Reach1");

        FBCryptoParams params = FBCryptoParams.createDefault();
        LOG.debug("Reach2");
        AsymJCEElGamalImpl instanceOfAlice = new AsymJCEElGamalImpl(params);
        LOG.debug("Reach3");
        FBElGamalEncryptedMessage cryptogram1 = new FBElGamalEncryptedMessage();    
        LOG.debug("Reach4");
        cryptogram1.setM2( new BigInteger("1e4c9037d2ab8687d6d3312f5b5b10a73d83a3ada7e59e744310292b7c1dd5955eda4a8555dcc6e67dd179733bde55b32dec0561f258629045ab02a477f09580aa6", 16)); 
        LOG.debug("Reach5");
        org.bouncycastle.math.ec.ECPoint.Fp _M1 = 
            instanceOfAlice.extrapolateECPoint(
                    new BigInteger("3323515dee1b436e222a542d9982f1c5681c6529c106a034d5b8dff59728a265316443d6965464f47355b92387ad4c4deb63b190a9d3e75f9c0529bc3d027f87e4", 16),
                    new BigInteger("182a8ff4d18d7924a6f0773cf52632277d1263ab0b26c00b90e7d656b1f209cc07c522d1e511eab2bec4ee335cd79247e22637bb13a1902a056fc4bd076df42b9b8", 16));
        LOG.debug("Reach6");
        // setting M1 to the instance of cryptogram
        cryptogram1.setM1(_M1);
        LOG.debug("Reach7");
        BigInteger pKey = new BigInteger("f7778e563581602bfaa4c03a32c4efcd925e952c0256365d02c6eb27f0452432481b1a1cadc65eed531b0391828366bb583edd563f3ce450cf4a470a49ce150fae", 16);
        LOG.debug("Reach8");
        BigInteger restored = BigInteger.ZERO;
        LOG.debug("Reach9");
        try
        {
            LOG.debug("Reach10");
            restored = instanceOfAlice.decryptAsymmetric(pKey, cryptogram1);
            LOG.debug("Reach11");
            LOG.debug(restored.toString(16));
        }
        catch (CryptoNotValidException e)
        {
            LOG.debug(e.getMessage());
        }
        LOG.debug("Reach12");
        LOG.debug(restored.toString(16));
        LOG.debug("Reach13");
        return restored.toString(16);
    }

}
