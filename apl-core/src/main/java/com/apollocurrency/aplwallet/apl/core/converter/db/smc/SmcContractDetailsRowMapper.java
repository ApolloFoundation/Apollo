package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
@Slf4j
public class SmcContractDetailsRowMapper implements RowMapper<ContractDetails> {
    private final TransactionTypeFactory factory;

    @Inject
    public SmcContractDetailsRowMapper(TransactionTypeFactory factory) {
        this.factory = factory;
    }

    @Override
    public ContractDetails map(ResultSet rs, StatementContext ctx) throws SQLException {
        //contract
        long address = rs.getLong("address");
        long owner = rs.getLong("owner");
        long transactionId = rs.getLong("transaction_id");
        String data = rs.getString("data");
        String contractName = rs.getString("name");
        String languageName = rs.getString("language");
        String languageVersion = rs.getString("version");
        String args = rs.getString("args");

        //state
        String status = rs.getString("smc_status");

        //transaction
        byte type = rs.getByte("type");
        byte subtype = rs.getByte("subtype");
        long amount = rs.getLong("amount");
        long fee = rs.getLong("fee");
        byte[] signature = rs.getBytes("signature");
        int blockTimestamp = rs.getInt("block_timestamp");
        byte[] attachmentBytes = rs.getBytes("attachment_bytes");
        ByteBuffer buffer = ByteBuffer.wrap(attachmentBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        TransactionType transactionType = factory.findTransactionType(type, subtype);
        SmcPublishContractAttachment attachment;
        try {
            attachment = (SmcPublishContractAttachment) transactionType.parseAttachment(buffer);
        } catch (AplException.NotValidException e) {
            throw new AplCoreContractViolationException("Can't parse attachment.", e);
        }

        ContractDetails contract = new ContractDetails();
        contract.setAddress(Convert2.rsAccount(address));
        contract.setOwner(Convert2.rsAccount(owner));
        contract.setTransaction(Long.toUnsignedString(transactionId));
        contract.setAmount(Long.toUnsignedString(amount));
        contract.setFee(Long.toUnsignedString(fee));
        contract.setSignature(Convert.toHexString(signature));
        contract.setTimestamp(Convert2.fromEpochTime(blockTimestamp));
        contract.setName(contractName);
        contract.setParams(args);
        contract.setFuelLimit(attachment.getFuelLimit().toString());
        contract.setFuelPrice(attachment.getFuelPrice().toString());
        contract.setStatus(status);

        log.trace("Contract details={}", contract);

        return contract;
    }
}
