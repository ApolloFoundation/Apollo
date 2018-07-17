/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
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

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

public class GeneratePublicKey {

    public static void main(String[] args) {
        if (args.length > 0) {
            System.out.println("Usage: java apl.tools.GeneratePublicKey");
            System.exit(1);
        }
        Logger.setLevel(Logger.Level.ERROR);
        String secretPhrase;
        Console console = System.console();
        if (console == null) {
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in))) {
                while ((secretPhrase = inputReader.readLine()) != null) {
                    printPublicKey(secretPhrase);
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            char[] chars;
            while ((chars = console.readPassword("Enter secret phrase: ")) != null && chars.length > 0) {
                secretPhrase = new String(chars);
                printPublicKey(secretPhrase);
            }
        }
    }

    private static void printPublicKey(String secretPhrase) {
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        System.out.println(Convert.toHexString(publicKey));
    }
}
