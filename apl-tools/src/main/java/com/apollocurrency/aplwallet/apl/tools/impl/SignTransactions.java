/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class SignTransactions {

    public static int sign(String unsignedFN, String signedFN) {
        try {
            File unsigned = new File(unsignedFN);
            if (!unsigned.exists()) {
                System.out.println("File not found: " + unsigned.getAbsolutePath());
                return PosixExitCodes.EX_IOERR.exitCode();
            }
            File signed = new File(signedFN);
            if (signed.exists()) {
                System.out.println("File already exists: " + signed.getAbsolutePath());
                return PosixExitCodes.EX_IOERR.exitCode();
            }

            byte[] keySeed = readKeySeed();
            int n = 0;
            if (Files.exists(signed.toPath())) {
                Files.delete(signed.toPath());
            }
                Files.createFile(signed.toPath());
                List<String> unsignedTransactions = Files.readAllLines(unsigned.toPath());

                for (String unsignedTransaction : unsignedTransactions) {
                    Files.write(signed.toPath(), signTransaction(unsignedTransaction, keySeed).getBytes(), StandardOpenOption.APPEND);
                    Files.write(signed.toPath(), System.lineSeparator().getBytes(), StandardOpenOption.APPEND);
                    n += 1;
                }
                System.out.println("Signed " + n + " transactions");
        } catch (Exception e) {
            e.printStackTrace();
            return PosixExitCodes.EX_IOERR.exitCode();
        }
        return PosixExitCodes.OK.exitCode();
    }
    
    private static String signTransaction(String transactionBytesHexString, byte[] keySeed) throws AplException.NotValidException {
        byte[] transactionBytes = Convert.parseHexString(transactionBytesHexString);
        Transaction.Builder builder = Transaction.newTransactionBuilder(transactionBytes);
        Transaction transaction = builder.build(keySeed);
        return Convert.toHexString(transaction.getBytes());
    }
    
    public static int signJson(String unsignedFN, String signedFN) {
        try {
            File unsigned = new File(unsignedFN);
            if (!unsigned.exists()) {
                System.out.println("File not found: " + unsigned.getAbsolutePath());
                return PosixExitCodes.EX_IOERR.exitCode();
            }
            File signed = new File(signedFN);
            if (signed.exists()) {
                System.out.println("File already exists: " + signed.getAbsolutePath());
               return PosixExitCodes.EX_IOERR.exitCode();
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(unsigned))){
                JSONObject json = (JSONObject) JSONValue.parseWithException(reader);
                byte[] keySeed = readKeySeed();
                Files.write(signed.toPath(), signTransaction(json, keySeed).getBytes(), StandardOpenOption.CREATE);
                System.out.println("Signed transaction JSON saved as: " + signed.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return PosixExitCodes.EX_IOERR.exitCode();
        }
        return PosixExitCodes.OK.exitCode();
    }

    public static byte[] readKeySeed() {
        String secretPhraseString;
        byte[] secret = null;
        Console console = System.console();
        if (console == null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
                System.out.println("Enter secretPhrase, if you have secretKey press enter:");
                secretPhraseString = in.readLine();
                System.out.println("Enter secret key in hexadecimal format: ");
                if (secretPhraseString.isEmpty()) {
                    String s = in.readLine();
                    secret = Convert.parseHexString(s);
                } else {
                    secret = Convert.toBytes(secretPhraseString);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            secretPhraseString = new String(console.readPassword("Secret phrase, skip if you have secretKey : "));
            if (secretPhraseString.isEmpty()) {
                String s = new String(console.readPassword("Enter secretKey in hexadecimal format: "));
                secret = Convert.parseHexString(s);
            } else {
                secret = Convert.toBytes(secretPhraseString);
            }
        }
        return Crypto.getKeySeed(secret);
    }

    private static String signTransaction(JSONObject transactionJson, byte[] keySeed) throws AplException.NotValidException {
        Transaction.Builder builder = Transaction.newTransactionBuilder(transactionJson);
        Transaction transaction = builder.build(keySeed);
        return transaction.getJSONObject().toJSONString();
    }



}
