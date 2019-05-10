/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.mapper;

import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSFeedback;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class DGSFeedbackMapper implements RowMapper<DGSFeedback> {
    @Override
    public DGSFeedback map(ResultSet rs, StatementContext ctx) throws SQLException {
        byte[] data = rs.getBytes("feedback_data");
        byte[] nonce = rs.getBytes("feedback_nonce");
        long id = rs.getLong("id");
        int height = rs.getInt("height");
        DGSFeedback dgsFeedback = new DGSFeedback(id, height, new EncryptedData(data, nonce));
        return dgsFeedback;
    }
}
