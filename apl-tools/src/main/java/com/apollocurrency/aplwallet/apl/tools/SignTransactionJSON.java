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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
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

public final class SignTransactionJSON {
    public static void main(String[] args) {
        try {
            if (args.length == 0 || args.length > 2) {
                System.out.println("Usage: SignTransactionJSON <unsigned transaction json file> <signed transaction json file>");
                System.exit(1);
            }
            File unsigned = new File(args[0]);
            if (!unsigned.exists()) {
                System.out.println("File not found: " + unsigned.getAbsolutePath());
                System.exit(1);
            }
            File signed;
            if (args.length == 2) {
                signed = new File(args[1]);
            } else if (unsigned.getName().startsWith("unsigned.")) {
                signed = new File(unsigned.getParentFile(), unsigned.getName().substring(2));
            } else {
                signed = new File(unsigned.getParentFile(), "signed." + unsigned.getName());
            }
            if (signed.exists()) {
                System.out.println("File already exists: " + signed.getAbsolutePath());
                System.exit(1);
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(unsigned))){
                JSONObject json = (JSONObject) JSONValue.parseWithException(reader);
                byte[] keySeed = readKeySeed();
                Files.write(signed.toPath(), signTransaction(json, keySeed).getBytes(), StandardOpenOption.CREATE);
                System.out.println("Signed transaction JSON saved as: " + signed.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
