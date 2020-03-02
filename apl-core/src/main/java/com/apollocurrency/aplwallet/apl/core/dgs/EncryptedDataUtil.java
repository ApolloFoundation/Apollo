/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class EncryptedDataUtil {

    public static EncryptedData loadEncryptedData(ResultSet rs, String dataColumn, String nonceColumn) throws SQLException {
        byte[] data = rs.getBytes(dataColumn);
        if (data == null) {
            return null;
        }
        return new EncryptedData(data, rs.getBytes(nonceColumn));
    }

    public static int setEncryptedData(PreparedStatement pstmt, EncryptedData encryptedData, int i) throws SQLException {
        if (encryptedData == null) {
            pstmt.setNull(i++, Types.VARBINARY);
            pstmt.setNull(i++, Types.VARBINARY);
        } else {
            pstmt.setBytes(i++, encryptedData.getData());
            pstmt.setBytes(i++, encryptedData.getNonce());
        }
        return i;
    }

    public static EncryptedData encryptTo(byte[] publicKey, byte[] data, byte[] keySeed, boolean compress) {
        if (compress && data.length > 0) {
            data = Convert.compress(data);
        }
        return EncryptedData.encrypt(data, keySeed, publicKey);
    }

    public static byte[] decryptFrom(byte[] publicKey, EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
        byte[] decrypted = encryptedData.decrypt(recipientKeySeed, publicKey);
        if (uncompress && decrypted.length > 0) {
            decrypted = Convert.uncompress(decrypted);
        }
        return decrypted;
    }

}
