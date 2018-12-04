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
import com.apollocurrency.aplwallet.apl.util.Convert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class SignTransactions {

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("Usage: SignTransactions <unsigned transaction bytes file> <signed transaction bytes file>");
                System.exit(1);
            }
            File unsigned = new File(args[0]);
            if (!unsigned.exists()) {
                System.out.println("File not found: " + unsigned.getAbsolutePath());
                System.exit(1);
            }
            File signed = new File(args[1]);
            if (signed.exists()) {
                System.out.println("File already exists: " + signed.getAbsolutePath());
                System.exit(1);
            }

            byte[] keySeed = SignTransactionJSON.readKeySeed();
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
        }
    }
    private static String signTransaction(String transactionBytesHexString, byte[] keySeed) throws AplException.NotValidException {
        byte[] transactionBytes = Convert.parseHexString(transactionBytesHexString);
        Transaction.Builder builder = Apl.newTransactionBuilder(transactionBytes);
        Transaction transaction = builder.build(keySeed);
        return Convert.toHexString(transaction.getBytes());
    }



}
