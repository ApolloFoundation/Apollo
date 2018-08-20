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

import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class BaseTargetTest {

    private static final long MIN_BASE_TARGET = Constants.INITIAL_BASE_TARGET * 9 / 10;
    private static final long MAX_BASE_TARGET = Constants.INITIAL_BASE_TARGET * (Constants.isTestnet ? Constants.MAX_BALANCE_APL : 50);

    private static final int MIN_BLOCKTIME_LIMIT = Constants.BLOCK_TIME - 7;
    private static final int MAX_BLOCKTIME_LIMIT = Constants.BLOCK_TIME + 7;

    private static final int GAMMA = 64;

    private static final int START_HEIGHT = 20;
    private static final int MAX_HEIGHT = 1_000;

    private static final boolean USE_EWMA = false;
    private static final int EWMA_N = 8;
    private static final int SMA_N = 3;
    private static final int FREQUENCY = 2;
    private static final int HEIGHT = 47;


    private static long calculateBaseTarget(long previousBaseTarget, long blocktimeEMA) {
        long baseTarget;
        if (blocktimeEMA > Constants.BLOCK_TIME) {
            baseTarget = (previousBaseTarget * Math.min(blocktimeEMA, MAX_BLOCKTIME_LIMIT)) / Constants.BLOCK_TIME;
        } else {
            baseTarget = previousBaseTarget - previousBaseTarget * GAMMA * (Constants.BLOCK_TIME - Math.max(blocktimeEMA, MIN_BLOCKTIME_LIMIT)) / (100 * Constants.BLOCK_TIME);
        }
        if (baseTarget < 0 || baseTarget > MAX_BASE_TARGET) {
            baseTarget = MAX_BASE_TARGET;
        }
        if (baseTarget < MIN_BASE_TARGET) {
            baseTarget = MIN_BASE_TARGET;
        }
        return baseTarget;
    }

    public static void main(String[] args) {

        try {

            BigInteger testCumulativeDifficulty = BigInteger.ZERO;
            long testBaseTarget;
            int testTimestamp;

            BigInteger cumulativeDifficulty = BigInteger.ZERO;
            long baseTarget;
            int timestamp;

            BigInteger previousCumulativeDifficulty = null;
            long previousBaseTarget = 0;
            int previousTimestamp = 0;

            BigInteger previousTestCumulativeDifficulty = null;
            long previousTestBaseTarget = 0;
            int previousTestTimestamp = 0;

            int height = START_HEIGHT;
            if (args.length == 1) {
                height = Integer.parseInt(args[0]);
            }

            long totalBlocktime = 0;
            long totalTestBlocktime = 0;
            long maxBlocktime = 0;
            long minBlocktime = Integer.MAX_VALUE;
            long maxTestBlocktime = 0;
            long minTestBlocktime = Integer.MAX_VALUE;
            double M = 0.0;
            double S = 0.0;
            double testM = 0.0;
            double testS = 0.0;
            long testBlocktimeEMA = 0;

            List<Integer> testBlocktimes = new ArrayList<>();

            int count = 0;

            String dbLocation = Constants.isTestnet ? "apl_test_db" : "apl_db";

            try (Connection con = DriverManager.getConnection("jdbc:h2:./" + dbLocation + "/apl;DB_CLOSE_ON_EXIT=FALSE;MVCC=TRUE", "sa", "sa");
                 PreparedStatement selectBlocks = con.prepareStatement("SELECT * FROM block WHERE height > " + height + " ORDER BY db_id ASC");
                 ResultSet rs = selectBlocks.executeQuery()) {

                while (rs.next()) {

                    cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
                    baseTarget = rs.getLong("base_target");
                    timestamp = rs.getInt("timestamp");
                    height = rs.getInt("height");

                    if (previousCumulativeDifficulty == null) {

                        previousCumulativeDifficulty = cumulativeDifficulty;
                        previousBaseTarget = baseTarget;
                        previousTimestamp = timestamp;

                        previousTestCumulativeDifficulty = previousCumulativeDifficulty;
                        previousTestBaseTarget = previousBaseTarget;
                        previousTestTimestamp = previousTimestamp;

                        continue;
                    }

                    int testBlocktime = (int)((previousBaseTarget * (timestamp - previousTimestamp - 1)) / previousTestBaseTarget) + 1;
                    if (testBlocktimeEMA == 0) {
                        testBlocktimeEMA = testBlocktime;
                    } else {
                        testBlocktimeEMA = (testBlocktime + testBlocktimeEMA * (EWMA_N - 1)) / EWMA_N;
                    }
                    testTimestamp = previousTestTimestamp + testBlocktime;

                    testBlocktimes.add(testBlocktime);
                    if (testBlocktimes.size() > SMA_N) {
                        testBlocktimes.remove(0);
                    }
                    int testBlocktimeSMA = 0;
                    for (int t : testBlocktimes) {
                        testBlocktimeSMA += t;
                    }
                    testBlocktimeSMA = testBlocktimeSMA / testBlocktimes.size();

                    if (testBlocktimes.size() < SMA_N) {
                        testBaseTarget = baseTarget;
                    } else if ((height - 1) % FREQUENCY == 0) {
                        testBaseTarget = calculateBaseTarget(previousTestBaseTarget, USE_EWMA ? testBlocktimeEMA : testBlocktimeSMA);
                    } else {
                        testBaseTarget = previousTestBaseTarget;
                    }
                    testCumulativeDifficulty = previousTestCumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(testBaseTarget)));

                    int blocktime = timestamp - previousTimestamp;
                    if (blocktime > maxBlocktime) {
                        maxBlocktime = blocktime;
                    }
                    if (blocktime < minBlocktime) {
                        minBlocktime = blocktime;
                    }
                    if (testBlocktime > maxTestBlocktime) {
                        maxTestBlocktime = testBlocktime;
                    }
                    if (testBlocktime < minTestBlocktime) {
                        minTestBlocktime = testBlocktime;
                    }
                    totalBlocktime += blocktime;
                    totalTestBlocktime += testBlocktime;
                    count += 1;

                    checkHeight(height);

                    double tmp = M;
                    M += (blocktime - tmp) / count;
                    S += (blocktime - tmp) * (blocktime - M);

                    tmp = testM;
                    testM += (testBlocktime - tmp) / count;
                    testS += (testBlocktime - tmp) * (testBlocktime - testM);

                    previousTestTimestamp = testTimestamp;
                    previousTestBaseTarget = testBaseTarget;
                    previousTestCumulativeDifficulty = testCumulativeDifficulty;

                    previousTimestamp = timestamp;
                    previousBaseTarget = baseTarget;
                    previousCumulativeDifficulty = cumulativeDifficulty;

                }

            }

            Logger.logMessage("Cumulative difficulty " + cumulativeDifficulty.toString());
            Logger.logMessage("Test cumulative difficulty " + testCumulativeDifficulty.toString());
            Logger.logMessage("Cumulative difficulty difference " + (testCumulativeDifficulty.subtract(cumulativeDifficulty))
                    .multiply(BigInteger.valueOf(100)).divide(cumulativeDifficulty).toString());
            Logger.logMessage("Max blocktime " + maxBlocktime);
            Logger.logMessage("Max test blocktime " + maxTestBlocktime);
            Logger.logMessage("Min blocktime " + minBlocktime);
            Logger.logMessage("Min test blocktime " + minTestBlocktime);
            Logger.logMessage("Average blocktime " + ((double)totalBlocktime) / count);
            Logger.logMessage("Average test blocktime " + ((double)totalTestBlocktime) / count);
            Logger.logMessage("Standard deviation of blocktime " + Math.sqrt(S / count));
            Logger.logMessage("Standard deviation of test blocktime " + Math.sqrt(testS / count));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkHeight(int currentHeight) {
        return currentHeight != HEIGHT;
    }

}
