/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import com.apollocurrency.aplwallet.apl.testutil.ResourceFileLoader;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@EnableWeld
class CsvReaderTest {

    private CsvEscaper translator = new CsvEscaperImpl();

    @Test
    void readShardCsv() throws Exception {
        int readRowsByCsvReader = 0;
        String itemName = "shard.csv";

        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        try (CsvReader csvReader = new CsvReaderImpl(resourceFileLoader.getResourcePath(), translator);
             ResultSet rs = csvReader.read(itemName, null, null)) {
            csvReader.setOptions("fieldDelimiter="); // do not put ""
            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            int columnsCount = meta.getColumnCount(); // columns count is main
            StringBuilder columnNames = new StringBuilder(200);

            for (int i = 0; i < columnsCount; i++) {
                columnNames.append(meta.getColumnLabel(i + 1)).append(",");
            }
            log.debug("'{}' column HEADERS = {}", itemName, columnNames.toString()); // read headers
            assertTrue(columnNames.toString().length() > 0, "headers row is empty for '" + itemName + "'");

            while (rs.next()) {
                for (int j = 0; j < columnsCount; j++) {
                    Object object = rs.getObject(j + 1); // can be NULL sometimes
                    log.trace("Row column [{}] value is {}", meta.getColumnLabel(j + 1), object);
                }
                readRowsByCsvReader++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> stringsInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(itemName));
        int numberOfRows = stringsInCsv.size();
        assertEquals(numberOfRows - 1, readRowsByCsvReader, "incorrect lines imported from '" + itemName + "'");
    }

    @Test
    void readSeveralCsvFiles() throws Exception {
        List<String> csvFileList = List.of("account_control_phasing.csv", "goods.csv", "goods2.csv",
            "phasing_poll.csv", "public_key.csv", "purchase.csv", "shard.csv", "shuffling_data.csv");

        int processedTables = 0;
        for (String itemName : csvFileList) {
            int readRowsByCsvReader = 0;

            ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
            try (CsvReader csvReader = new CsvReaderImpl(resourceFileLoader.getResourcePath(), translator);
                 ResultSet rs = csvReader.read(itemName, null, null)) {
                csvReader.setOptions("fieldDelimiter="); // do not put ""
                // get CSV meta data info
                ResultSetMetaData meta = rs.getMetaData();
                int columnsCount = meta.getColumnCount(); // columns count is main
                StringBuilder columnNames = new StringBuilder(200);

                for (int i = 0; i < columnsCount; i++) {
                    columnNames.append(meta.getColumnLabel(i + 1)).append(",");
                }
                log.debug("'{}' column HEADERS = {}", itemName, columnNames.toString()); // read headers
                assertTrue(columnNames.toString().length() > 0, "headers row is empty for '" + itemName + "'");

                while (rs.next()) {
                    for (int j = 0; j < columnsCount; j++) {
                        Object object = rs.getObject(j + 1); // can be NULL sometimes
                        log.trace("Row column [{}] value is {}", meta.getColumnLabel(j + 1), object);
                    }
                    readRowsByCsvReader++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            List<String> stringsInCsv = Files.readAllLines(resourceFileLoader.getResourcePath().resolve(itemName));
            int numberOfRows = stringsInCsv.size();
            assertEquals(numberOfRows - 1, readRowsByCsvReader, "incorrect lines imported from '" + itemName + "'");
            processedTables++;
        }
        assertEquals(processedTables, csvFileList.size());
    }

}