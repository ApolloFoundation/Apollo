package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import javax.inject.Inject;
import javax.inject.Singleton;
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
        String data = rs.getString("data");
        String contractName = rs.getString("name");
        String baseContract = rs.getString("base_contract");
        String args = rs.getString("args");

        //state
        String status = rs.getString("smc_status");

        //transaction
        long transactionId = rs.getLong("transaction_id");
        int blockTimestamp = rs.getInt("block_timestamp");
        byte[] transactionFullHash = rs.getBytes("transaction_full_hash");
        long fuelPrice = rs.getLong("fuel_price");
        long fuelLimit = rs.getLong("fuel_limit");
        long fuelCharged = rs.getLong("fuel_charged");

        ContractDetails contract = new ContractDetails();
        contract.setAddress(Convert2.rsAccount(address));
        contract.setOwner(Convert2.rsAccount(owner));
        contract.setName(contractName);
        contract.setBaseContract(baseContract);
        contract.setSrc(data);
        contract.setParams(args);

        contract.setTransaction(Long.toUnsignedString(transactionId));
        contract.setFullHash(Convert.toHexString(transactionFullHash));
        contract.setSignature(contract.getFullHash());
        contract.setTimestamp(Convert2.fromEpochTime(blockTimestamp));
        contract.setFuelPrice(Long.toUnsignedString(fuelPrice));
        contract.setFuelLimit(Long.toUnsignedString(fuelLimit));
        contract.setFuelCharged(Long.toUnsignedString(fuelCharged));
        contract.setStatus(status);

        log.trace("Contract details={}", contract);

        return contract;
    }
}
