/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.dgs;

import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class DGSFeedbackMapper extends VersionedDerivedEntityMapper<DGSFeedback> {

    public DGSFeedbackMapper(KeyFactory<DGSFeedback> keyFactory) {
        super(keyFactory);
    }

    @Override
    public DGSFeedback doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        byte[] data = rs.getBytes("feedback_data");
        byte[] nonce = rs.getBytes("feedback_nonce");
        long id = rs.getLong("id");
        return new DGSFeedback(null, null, id, new EncryptedData(data, nonce));
    }
}
