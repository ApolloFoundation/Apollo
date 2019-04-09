/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.mapper;

import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class DGSPublicFeedbackMapper implements RowMapper<DGSPublicFeedback> {
    @Override
    public DGSPublicFeedback map(ResultSet rs, StatementContext ctx) throws SQLException {
        String feedback = rs.getString("public_feedback");
        int height = rs.getInt("height");
        long purchaseId = rs.getLong("id");
        return new DGSPublicFeedback(feedback, purchaseId, height);
    }
}
