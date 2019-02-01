/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.tools.impl;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.tools.ApolloTools;
import com.apollocurrency.aplwallet.apl.updater.decryption.RSAUtil;
import com.apollocurrency.aplwallet.apl.util.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Objects;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilites for updater URL encryption/decryption
 *
 * @author al
 */
public class UpdaterUrlUtils {

    private static final Logger log = LoggerFactory.getLogger(ApolloTools.class);

    /**
     * Usage: You can encrypt your message only if you have private key and
     * message to encrypt which can be passed in two ways: as hexadecimal
     * representation of bytes or as simple utf-8 message There are two
     * different ways to encrypt message: 1. Interactive - run this class
     * without input parameters and follow the instructions 2. With parameters -
     * run class with parameters For running with parameters you should pass 3
     * parameters in following order: a) isHexadecimal - boolean flag that
     * indicates that you want to pass to encryption not the ordinary string,
     * but hexadecimal b) hexadecimal string of message bytes or just message
     * depending on option isHexadecimal c) private key path (absolute path is
     * better) Example: java
     * com.apollocurrency.aplwallet.apl.tools.RSAEncryption false SecretMessage
     * C:/user/admin/private.key
     */
    public static int encrypt(String pkPathString, String messageString, boolean isHexadecimal) {
        boolean isSecondEncryption = false;

//            System.out.println("Is your message in hexadecimal format ('true' or 'false')");
//            isHexadecimalString = sc.nextLine();
//            System.out.println("Enter message: ");
//            messageString = sc.nextLine().trim();
//            System.out.println("Enter private key path: ");
//            pkPathString = sc.nextLine();
        Objects.requireNonNull(pkPathString);
        Objects.requireNonNull(messageString);

        System.out.println("Got private key path " + pkPathString);
        System.out.println("Got message: \'" + messageString + "\'");
        System.out.println("Got isHexadecimal: " + isHexadecimal);

        byte[] messageBytes;
        if (isHexadecimal) {
            messageBytes = Convert.parseHexString(messageString);
        } else {
            messageBytes = messageString.getBytes();
        }
        RSAPrivateKey privateKey;
        try {
            privateKey = (RSAPrivateKey) RSAUtil.getPrivateKey(pkPathString);
        } catch (IOException | GeneralSecurityException | URISyntaxException ex) {
            log.error("Error reading private key", ex);
            return PosixExitCodes.EX_IOERR.exitCode();
        }
        if (messageBytes.length > RSAUtil.keyLength(privateKey)) {
            System.out.println("Cannot encrypt message with size: " + messageBytes.length);
            System.exit(1);
        }
        if (messageBytes.length == RSAUtil.keyLength(privateKey)) {
            isSecondEncryption = true;
            System.out.println("Second encryption will be performed");
        } else if (messageBytes.length > RSAUtil.maxEncryptionLength(privateKey)) {
            System.out.println("Message size is greater than " + RSAUtil.maxEncryptionLength(privateKey) + " bytes. Cannot encrypt.");
            return PosixExitCodes.EX_DATAERR.exitCode();
        }
        String result;
        if (isSecondEncryption) {
            DoubleByteArrayTuple splittedEncryptedBytes;
            try {
                splittedEncryptedBytes = RSAUtil.secondEncrypt(privateKey, messageBytes);
            } catch (GeneralSecurityException ex) {
                log.error("Security exception.", ex);
                return PosixExitCodes.EX_PROTOCOL.exitCode();
            }
            result = splittedEncryptedBytes.toString();
        } else {
            byte[] encryptedBytes;
            try {
                encryptedBytes = RSAUtil.encrypt(privateKey, messageBytes);
            } catch (GeneralSecurityException ex) {
                log.error("Security exception.", ex);
                return PosixExitCodes.EX_PROTOCOL.exitCode();
            }
            result = Convert.toHexString(encryptedBytes);
        }

        System.out.println("Your encrypted message in hexadecimal format:");
        System.out.print(result);
        return PosixExitCodes.OK.exitCode();
    }

    /**
     * Usage: You can decrypt your message only if you have x509 certificate
     * with public key and hexadecimal string that represents bytes of encrypted
     * message There are two different ways to decrypt message: 1.Interactive -
     * run this class without input parameters and follow the instructions 2.
     * With parameters - run class with parameters For running with parameters
     * you should pass 3 parameters in following order: a) certificate path
     * (absolute path is better) b) hexadecimal string of encrypted message
     * bytes c) boolean flag that indicates that you want to make from decrypted
     * bytes real message string Example: decrypt( [
     * C:/user/admin/certificate.pem f4ac4942(another 128, 256, 512 hexadecimal
     * signs) true] )
     */
    public static int decrypt(String certificatePathString, String encryptedMessageString, boolean isConvertToString) {

//            System.out.println("Enter encrypted message in hexadecimal format. If you want to decrypt double encrypted message separate 512 byte messages with coma or semicolon");
//            encryptedMessageString = sc.nextLine();
//            System.out.println("Enter certificate path. If you want to decrypt double encrypted message completely - separate certificate paths with coma or semicolon");
//            certificatePathString = sc.nextLine();
//            System.out.println("Do you want to convert bytes to UTF-8 string ('true' or 'false')");
//            convertToString = sc.nextLine();
        Objects.requireNonNull(certificatePathString);
        Objects.requireNonNull(encryptedMessageString);

        System.out.println("Got public key path " + certificatePathString);
        System.out.println("Got encrypted message: \'" + encryptedMessageString + "\'");
        System.out.println("Convert to string: " + isConvertToString);
        String[] split = encryptedMessageString.split("([;,]+)|(\\s+)");
        boolean isSplittedMessage = split.length == 2;
        if (split.length > 2 || split.length == 0) {
            System.out.println("Invalid message string.");
            return PosixExitCodes.EX_DATAERR.exitCode();
        }
        String[] split1 = certificatePathString.split("([;,]+)|(\\s+)");
        if (split1.length > 2 || split1.length == 0) {
            System.out.println("Invalid certificate string");
            return PosixExitCodes.EX_DATAERR.exitCode();
        }

        boolean isDoubleDecryptionRequired = split1.length == 2;

        byte[] result;
        PublicKey publicKey1;
        try {
            publicKey1 = RSAUtil.getPublicKeyFromCertificate(split1[0]);
        } catch (CertificateException | IOException | URISyntaxException ex) {
            log.error("Security exception.", ex);
            return PosixExitCodes.EX_PROTOCOL.exitCode();
        }

        PublicKey publicKey2;
        try {
            if (isSplittedMessage) {
                byte[] firstMessagePart = Convert.parseHexString(split[0]);
                byte[] secondMessagePart = Convert.parseHexString(split[1]);
                DoubleByteArrayTuple encryptedBytes = new DoubleByteArrayTuple(firstMessagePart, secondMessagePart);
                if (isDoubleDecryptionRequired) {
                    publicKey2 = RSAUtil.getPublicKeyFromCertificate(split1[1]);
                    result = RSAUtil.doubleDecrypt(publicKey1, publicKey2, encryptedBytes);
                } else {
                    result = RSAUtil.firstDecrypt(publicKey1, encryptedBytes);
                }
            } else {
                result = RSAUtil.decrypt(publicKey1, Convert.parseHexString(split[0]));
            }
        } catch (CertificateException ex) {
            java.util.logging.Logger.getLogger(UpdaterUrlUtils.class.getName()).log(Level.SEVERE, null, ex);
            return PosixExitCodes.EX_DATAERR.exitCode();
        } catch (IOException | URISyntaxException | GeneralSecurityException ex) {
            java.util.logging.Logger.getLogger(UpdaterUrlUtils.class.getName()).log(Level.SEVERE, null, ex);
            return PosixExitCodes.EX_DATAERR.exitCode();
        }

        System.out.println("Your decrypted message in hexadecimal format:");
        System.out.println(Convert.toHexString(result));

        if (isConvertToString) {
            System.out.println("Result message is:");
            try {
                System.out.println(new String(result, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                log.error("Unsupported Encosing", ex);
            }
        }
        return PosixExitCodes.OK.exitCode();
    }

}
