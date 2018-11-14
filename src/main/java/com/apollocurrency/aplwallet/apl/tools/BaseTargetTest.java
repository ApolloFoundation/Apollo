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

import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.slf4j.Logger;

public final class BaseTargetTest {
        private static final Logger LOG = getLogger(BaseTargetTest.class);

    private static final long MIN_BASE_TARGET = Constants.getInitialBaseTarget() * 9 / 10;
    private static final long MAX_BASE_TARGET = Constants.getInitialBaseTarget() * (Constants.isTestnet() ? Constants.getMaxBalanceAPL() : 50);

    private static final int MIN_BLOCKTIME_LIMIT = Constants.getBlockTime() - 7;
    private static final int MAX_BLOCKTIME_LIMIT = Constants.getBlockTime() + 7;

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
        int blockTime = Constants.getBlockTime();
        if (blocktimeEMA > blockTime) {
            baseTarget = (previousBaseTarget * Math.min(blocktimeEMA, MAX_BLOCKTIME_LIMIT)) / blockTime;
        } else {
            baseTarget =
                    previousBaseTarget - previousBaseTarget * GAMMA * (blockTime - Math.max(blocktimeEMA, MIN_BLOCKTIME_LIMIT)) / (100 * blockTime);
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

            String dbLocation = Constants.isTestnet() ? "apl_test_db" : "apl_db";

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

            LOG.info("Cumulative difficulty " + cumulativeDifficulty.toString());
            LOG.info("Test cumulative difficulty " + testCumulativeDifficulty.toString());
            LOG.info("Cumulative difficulty difference " + (testCumulativeDifficulty.subtract(cumulativeDifficulty))
                    .multiply(BigInteger.valueOf(100)).divide(cumulativeDifficulty).toString());
            LOG.info("Max blocktime " + maxBlocktime);
            LOG.info("Max test blocktime " + maxTestBlocktime);
            LOG.info("Min blocktime " + minBlocktime);
            LOG.info("Min test blocktime " + minTestBlocktime);
            LOG.info("Average blocktime " + ((double)totalBlocktime) / count);
            LOG.info("Average test blocktime " + ((double)totalTestBlocktime) / count);
            LOG.info("Standard deviation of blocktime " + Math.sqrt(S / count));
            LOG.info("Standard deviation of test blocktime " + Math.sqrt(testS / count));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkHeight(int currentHeight) {
        return currentHeight != HEIGHT;
    }

}
