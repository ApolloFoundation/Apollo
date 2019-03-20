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

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.slf4j.LoggerFactory.getLogger;

public final class PassphraseRecovery {
    private static final Logger LOG = getLogger(PassphraseRecovery.class);

    final static Solution NO_SOLUTION = new Solution();
    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private static DatabaseManager databaseManager;

    public static void main(String[] args) {
        new PassphraseRecovery().recover();
    }

    private void recover() {
        try {
            Map<Long, byte[]> publicKeys = getPublicKeys();
            String wildcard = propertiesHolder.getStringProperty("recoveryWildcard", "", false, "UTF-8"); // support UTF8 chars
            if ("".equals(wildcard)) {
                LOG.info("Specify in the recoveryWildcard setting, an approximate passphrase as close as possible to the real passphrase");
                return;
            }
            int[] passphraseChars = wildcard.chars().toArray();
            LOG.info("wildcard=" + wildcard + ", wildcard chars=" + Arrays.toString(passphraseChars));
            String positionsStr = propertiesHolder.getStringProperty("recoveryPositions", "");
            int[] positions;
            try {
                if (positionsStr.length() == 0) {
                    positions = new int[0];
                } else {
                    positions = Arrays.stream(positionsStr.split(",")).map(String::trim).mapToInt(Integer::parseInt).map(i -> i - 1).toArray();
                }
                List<Integer> list = IntStream.of(positions).boxed().collect(Collectors.toList());
                String s = list.stream().map(p -> Character.toString(wildcard.charAt(p))).collect(Collectors.joining(" "));
                LOG.info("Recovering chars: " + s);
            } catch (NumberFormatException e) {
                LOG.info("Specify in the recoveryPositions setting, a comma separated list of numeric positions pointing to the recoveryWildcard unknown characters (first position is 1)");
                return;
            }
            String dictionaryStr = propertiesHolder.getStringProperty("recoveryDictionary", "");
            char[] dictionary;
            switch(dictionaryStr.toLowerCase()) {
                case "":
                case "ascii":
                    dictionary = getDictionary(32, 127);
                    break;
                case "asciiall":
                    dictionary = getDictionary(0, (int)(Math.pow(2, 8) - 1));
                    break;
                case "unicode":
                    dictionary = getDictionary(0, (int)(Math.pow(2, 16) - 1));
                    break;
                default:
                    dictionary = dictionaryStr.toCharArray();
            }
            LOG.info(String.format("Wildcard %s positions %s dictionary %s", wildcard, Arrays.toString(positions), Arrays.toString(dictionary)));
            Scanner scanner = new Scanner(publicKeys, positions, wildcard.toCharArray(), dictionary);
            Solution solution = scanner.scan();
            LOG.debug("" + solution);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static char[] getDefaultDictionary() {
        return getDictionary(27, 132);
    }

    static char[] getDictionary(int from, int to) {
        return IntStream.rangeClosed(from, to).mapToObj(c -> "" + (char) c).collect(Collectors.joining()).toCharArray();
    }

    static Map<Long, byte[]> getPublicKeys() {
        DbProperties dbProperties = CDI.current().select(DbProperties.class).get(); // it should be present and initialized
        databaseManager = new DatabaseManagerImpl(dbProperties, propertiesHolder);
        Map<Long, byte[]> publicKeys = new HashMap<>();
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement selectBlocks = con.prepareStatement("SELECT * FROM public_key WHERE latest=TRUE");
             ResultSet rs = selectBlocks.executeQuery()) {
            while (rs.next()) {
                long accountId = rs.getLong("account_id");
                byte[] publicKey = rs.getBytes("public_key");
                publicKeys.put(accountId, publicKey);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }

        LOG.info(String.format("Loaded %d public keys", publicKeys.size()));
        return publicKeys;
    }

    static class Scanner implements Callable<Solution> {
        private Map<Long, byte[]> publicKeys;
        private int[] positions;
        private char[] wildcard;
        private char[] dictionary;
        private Solution realSolution = NO_SOLUTION;

        Scanner(Map<Long, byte[]> publicKeys, int[] positions, char[] wildcard, char[] dictionary) {
            this.publicKeys = publicKeys;
            this.positions = positions;
            this.wildcard = wildcard;
            this.dictionary = dictionary;
        }

        Solution scan() {
            if (positions.length == 0) {
                LOG.info("Position not specified scanning for a single typo");
                char[] copy = new char[wildcard.length];
                for (int i=0; i<wildcard.length; i++) {
                    positions = new int[1];
                    positions[0] = i;
                    System.arraycopy(wildcard, 0, copy, 0, wildcard.length);
                    Solution solution = scan(0, copy);
                    if (solution != NO_SOLUTION) {
                        return solution;
                    }
                }
                return NO_SOLUTION;
            }
            LOG.info("Scanning " + Math.pow(dictionary.length, positions.length) + " permutations");
            if (positions.length == 1) {
                return scan(0, wildcard);
            }
            final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
            final ExecutorCompletionService<Solution> completionService = new ExecutorCompletionService<>(executorService);
            executorService.submit(() -> {
                int counter = 0;
                while (!executorService.isShutdown()) {
                    final Solution solution;
                    try {
                        solution = completionService.take().get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new IllegalStateException(e);
                    }
                    counter++;
                    LOG.info(String.format("task %d / %d is done", counter, dictionary.length));
                    if (solution != NO_SOLUTION) {
                        realSolution = solution;
                        break;
                    }
                }
                executorService.shutdown();
            });
            for (char c : dictionary) {
                char[] subWildcard = new char[wildcard.length];
                System.arraycopy(wildcard, 0, subWildcard, 0, wildcard.length);
                subWildcard[positions[0]] = c;
                Scanner scanner = new Scanner(publicKeys, positions, subWildcard, dictionary);
                completionService.submit(scanner);
            }
            try {
                executorService.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return realSolution;
        }

        private Solution scan(int pos, char[] wildcard) {
            for (char c : dictionary) {
                wildcard[positions[pos]] = c;
                if (pos < positions.length - 1) {
                    Solution solution = scan(pos + 1, wildcard);
                    if (solution != NO_SOLUTION) {
                        return solution;
                    }
                } else {
                    String secretPhrase = new String(wildcard);
                    byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                    long id = Account.getId(publicKey);
                    if (publicKeys.keySet().contains(id)) {
                        return new Solution(secretPhrase, publicKeys.get(id), id, Convert2.rsAccount(id));
                    }
                }
            }
            return NO_SOLUTION;
        }

        @Override
        public Solution call() throws Exception {
            return scan(1, wildcard);
        }
    }

    static class Solution {
        private String passphrase;
        private byte[] publicKey;
        private long accountId;
        private String rsAccount;

        Solution() {
        }

        Solution(String passphrase, byte[] publicKey, long accountId, String rsAccount) {
            this.passphrase = passphrase;
            this.publicKey = publicKey;
            this.accountId = accountId;
            this.rsAccount = rsAccount;
        }

        public Solution(String passphrase, byte[] publicKey, long accountId) {
            this.passphrase = passphrase;
            this.publicKey = publicKey;
            this.accountId = accountId;
            this.rsAccount = Convert2.rsAccount(accountId);
        }

        @Override
        public String toString() {
            if (this == NO_SOLUTION) {
                return "Not Found";
            }
            int[] passphraseChars = passphrase.chars().toArray();
            return "Solution{" +
                    "passphrase=" + passphrase +
                    ", passphraseChars=" + Arrays.toString(passphraseChars) +
                    ", publicKey=" + (publicKey != null ? Convert.toHexString(publicKey) : "not registered on blockchain") +
                    ", accountId=" + accountId +
                    ", rsAccount=" + rsAccount +
                    '}';
        }

        public String getRsAccount() {
            return rsAccount;
        }
    }
}
