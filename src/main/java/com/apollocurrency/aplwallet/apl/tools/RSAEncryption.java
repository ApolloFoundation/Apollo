/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.RSAUtil;
import com.apollocurrency.aplwallet.apl.util.Convert;

import java.security.interfaces.RSAPrivateKey;
import java.util.Objects;
import java.util.Scanner;

/**
 * Usage:
 * You can encrypt your message only if you have private key and message to encrypt which can be passed in two ways: as hexadecimal representation of bytes or as simple utf-8 message
 * There are two different ways to encrypt message:
 * 1. Interactive - run this class without input parameters and follow the instructions
 * 2. With parameters - run class with parameters
 * For running with parameters you should pass 3 parameters in following order:
 * a) isHexadecimal - boolean flag that indicates that you want to pass to encryption not the ordinary string, but hexadecimal
 * b) hexadecimal string of message bytes or just message depending on option isHexadecimal
 * c) private key path (absolute path is better)
 * Example: java com.apollocurrency.aplwallet.apl.tools.RSAEncryption false SecretMessage C:/user/admin/private.key
 */

public class RSAEncryption {
    public static void main(String[] args) throws Exception {
        String pkPathString = "";
        String messageString = "";
        String isHexadecimalString = "";
        boolean isSecondEncryption = false;
        if (args == null || args.length == 0) {
            Scanner sc = new Scanner(System.in);

            System.out.println("Is your message in hexadecimal format ('true' or 'false')");
            isHexadecimalString = sc.nextLine();

            System.out.println("Enter message: ");
            messageString = sc.nextLine().trim();

            System.out.println("Enter private key path: ");
            pkPathString = sc.nextLine();


        } else if (args.length == 3) {
            isHexadecimalString = args[0];
            messageString = args[1];
            pkPathString = args[2];
            Objects.requireNonNull(pkPathString);
            Objects.requireNonNull(messageString);
        } else {
            System.out.println("Invalid number of parameters: " + args.length + ". Required 3 or no parameters at all");
            System.exit(1);
        }
        System.out.println("Got private key path " + pkPathString);
        System.out.println("Got message: \'" + messageString + "\'");
        System.out.println("Got isHexadecimal: " + isHexadecimalString);
        boolean isHexadecimal;
        try {
            isHexadecimal = Boolean.parseBoolean(isHexadecimalString);
        }
        catch (Exception e) {
            isHexadecimal = false;
        }

        byte[] messageBytes;
        if (isHexadecimal) {
            messageBytes = Convert.parseHexString(messageString);
        } else {
            messageBytes = messageString.getBytes();
        }
        RSAPrivateKey privateKey = (RSAPrivateKey) RSAUtil.getPrivateKey(pkPathString);
        if (messageBytes.length > RSAUtil.keyLength(privateKey)) {
            System.out.println("Cannot encrypt message with size: " + messageBytes.length);
            System.exit(1);
        }
        if (messageBytes.length == RSAUtil.keyLength(privateKey)) {
            isSecondEncryption = true;
            System.out.println("Second encryption will be performed");
        } else if (messageBytes.length > RSAUtil.maxEncryptionLength(privateKey)) {
            System.out.println("Message size is greater than " + RSAUtil.maxEncryptionLength(privateKey) + " bytes. Cannot encrypt.");
            return;
        }
        String result;
        if (isSecondEncryption) {
            DoubleByteArrayTuple splittedEncryptedBytes = RSAUtil.secondEncrypt(privateKey, messageBytes);
            result = splittedEncryptedBytes.toString();
        } else {
            byte[] encryptedBytes = RSAUtil.encrypt(privateKey, messageBytes);
            result = Convert.toHexString(encryptedBytes);
        }

        System.out.println("Your encrypted message in hexadecimal format:");
        System.out.print(result);
    }

}
