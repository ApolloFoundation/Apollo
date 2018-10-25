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

package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
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
                byte[] publicKeyHash = Crypto.sha256().digest(Convert.parseHexString((String) json.get("senderPublicKey")));
                String senderRS = Convert.rsAccount(Convert.fullHashToId(publicKeyHash));
                byte[] keySeed = readKeySeed();
                Files.write(signed.toPath(), signTransaction(json, keySeed).getBytes(), StandardOpenOption.CREATE);
                System.out.println("Signed transaction JSON saved as: " + signed.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] readKeySeed() {
        String keySeedString;
        byte[] keySeed = null;
        Console console = System.console();
        if (console == null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
                System.out.println("Enter secretPhrase, if you have keySeed press enter:");
                keySeedString = in.readLine();
                System.out.println("Enter keySeed in hexadecimal fotmat: ");
                if (keySeedString.isEmpty()) {
                    keySeedString = in.readLine();
                    keySeed = Convert.parseHexString(keySeedString);
                } else {
                    keySeed = Crypto.getKeySeed(keySeedString);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            keySeedString = new String(console.readPassword("Secret phrase, skip if you have keySeed : "));
            if (keySeedString.isEmpty()) {
                keySeedString = new String(console.readPassword("Enter keySeed in hexadecimal fotmat: "));
                keySeed = Convert.parseHexString(keySeedString);
            } else {
                keySeed = Crypto.getKeySeed(keySeedString);
            }
        }
        return keySeed;
    }

    private static String signTransaction(JSONObject transactionJson, byte[] keySeed) throws AplException.NotValidException {
        Transaction.Builder builder = Apl.newTransactionBuilder(transactionJson);
        Transaction transaction = builder.build(keySeed);
        return transaction.getJSONObject().toJSONString();
    }

}
