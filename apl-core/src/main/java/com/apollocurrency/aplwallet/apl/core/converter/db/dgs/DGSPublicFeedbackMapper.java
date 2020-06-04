/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.dgs;

import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class DGSPublicFeedbackMapper extends VersionedDerivedEntityMapper<DGSPublicFeedback> {

    public DGSPublicFeedbackMapper(KeyFactory<DGSPublicFeedback> keyFactory) {
        super(keyFactory);
    }

    @Override
    public DGSPublicFeedback doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        String feedback = rs.getString("public_feedback");
        long purchaseId = rs.getLong("id");
        return new DGSPublicFeedback(null, null, feedback, purchaseId);
    }
}
