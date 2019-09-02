package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExchangeContractMapper  implements RowMapper<ExchangeContract> {
    @Override
    public ExchangeContract map(ResultSet rs, StatementContext ctx) throws SQLException {
        return ExchangeContract.builder()
                .id(rs.getLong("db_id"))
                .orderId(rs.getLong("offer_id"))
                .recipient(rs.getLong("recipient"))
                .sender(rs.getLong("sender"))
                .contractStatus(ExchangeContractStatus.getType(rs.getByte("status")))
                .counterOrderId(rs.getLong("counter_offer_id"))
                .secretHash(rs.getBytes("secret_hash"))
                .encryptedSecret(rs.getBytes("encrypted_secret"))
                .transferTxId(rs.getString("transfer_tx_id"))
                .counterTransferTxId(rs.getString("counter_transfer_tx_id"))
//                .finishTime(rs.getInt("finish_time"))
                .build();
    }
}