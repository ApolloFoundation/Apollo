package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.updater.RSAUtil;
import com.apollocurrency.aplwallet.apl.util.Convert;

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
 * a) private key path (absolute path is better)
 * b) hexadecimal string of message bytes or just message depending on option isHexadecimal
 * Example: java com.apollocurrency.aplwallet.apl.tools.RSAEncryption false C:/user/admin/private.key SecretMessage
 */

public class RSAEncryption {
    public static void main(String [] args) throws Exception {
        String pkPathString = "";
        String messageString = "";
        String isHexadecimalString = "";
        if (args == null || args.length == 0) {
            Scanner sc = new Scanner(System.in);

            System.out.println("Is your message in hexadecimal format ('true' or 'false')");
            isHexadecimalString = sc.nextLine();

            System.out.println("Enter message: ");
            messageString = sc.nextLine();

            System.out.println("Enter private key path: ");
            pkPathString = sc.nextLine();


        } else if (args.length == 3) {
            isHexadecimalString = args[0];
            pkPathString = args[1];
            messageString = args[2];
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
        }  else {
            messageBytes = messageString.getBytes();
        }
        byte[] encryptedBytes = RSAUtil.encrypt(pkPathString, messageBytes);

        System.out.println("Your encrypted message in hexadecimal format: ");
        System.out.println(Convert.toHexString(encryptedBytes));

    }

}
