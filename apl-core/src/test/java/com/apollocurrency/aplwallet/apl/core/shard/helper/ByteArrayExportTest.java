package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ByteArrayExportTest {

    @Test
    void testExportBytes() throws SQLException {
        byte[] hexString = Convert.parseHexString("c7571205be4b9a79d48d1906756050b2230391ffa70fe09801ee86edd3a330b");
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("hexBytes", Types.BINARY, 32, 0);
        resultSet.addRow(new Object[]{hexString});
        resultSet.next();
        byte[] hex = resultSet.getBytes(1);
        String encoded = Base64.getEncoder().encodeToString(hex);
        byte[] decode = Base64.getDecoder().decode(encoded);
        assertArrayEquals(hexString, decode);
    }

}
