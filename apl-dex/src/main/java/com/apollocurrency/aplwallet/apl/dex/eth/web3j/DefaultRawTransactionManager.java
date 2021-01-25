package com.apollocurrency.aplwallet.apl.dex.eth.web3j;

import com.apollocurrency.aplwallet.apl.dex.core.dao.DexTransactionDao;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexTransaction;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

public class DefaultRawTransactionManager extends RawTransactionManager {

    private DexTransaction transaction;
    private DexTransactionDao dexTransactionDao;


    public DefaultRawTransactionManager(Web3j web3j, Credentials credentials, byte chainId, DexTransaction dexTransaction, DexTransactionDao dexTransactionDao) {
        super(web3j, credentials, chainId);
        this.dexTransactionDao = dexTransactionDao;
        this.transaction = dexTransaction;
    }

    @Override
    public String sign(RawTransaction rawTransaction) {
        String signedTx = super.sign(rawTransaction);
        String txHash = Hash.sha3(signedTx);
        transaction.setHash(Numeric.hexStringToByteArray(txHash));
        transaction.setRawTransactionBytes(Numeric.hexStringToByteArray(signedTx));
        transaction.setTimestamp(System.currentTimeMillis());
        dexTransactionDao.add(transaction);
        return signedTx;
    }
}
