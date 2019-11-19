package com.apollocurrency.aplwallet.apl.eth.web3j;

import com.apollocurrency.aplwallet.apl.exchange.dao.DexTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTransaction;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;
import org.web3j.utils.TxHashVerifier;

public class DefaultRawTransactionManager extends RawTransactionManager {

    private DexTransaction transaction;
    private DexTransactionDao dexTransactionDao;

    protected TxHashVerifier txHashVerifier = new TxHashVerifier();

    public DefaultRawTransactionManager(Web3j web3j, Credentials credentials, byte chainId, DexTransaction dexTransaction, DexTransactionDao dexTransactionDao) {
        super(web3j, credentials, chainId);
        this.dexTransactionDao = dexTransactionDao;
        this.transaction = dexTransaction;
    }

    public TxHashVerifier getTxHashVerifier() {
        return txHashVerifier;
    }

    public void setTxHashVerifier(TxHashVerifier txHashVerifier) {
        this.txHashVerifier = txHashVerifier;
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
