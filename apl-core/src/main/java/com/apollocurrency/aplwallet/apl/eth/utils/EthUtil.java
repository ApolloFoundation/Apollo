package com.apollocurrency.aplwallet.apl.eth.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.ethereum.util.blockchain.EtherUtil;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Convert;

public class EthUtil {

    /**
     * Generate new account with random key.
     * @return EthWallet
     */
    public static EthWalletKey generateNewAccount(){
        byte[] secretBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(secretBytes);
        byte[] keySeed = Crypto.getKeySeed(secretBytes);
        byte[] privateKey = Crypto.getPrivateKey(keySeed);

        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        Credentials cs = Credentials.create(ecKeyPair);

        return new EthWalletKey(cs);
    }

    public static BigDecimal weiToEther(BigInteger wei) {
        return Convert.fromWei(wei.toString(), Convert.Unit.ETHER);
    }

    public static BigInteger etherToWei(BigDecimal ether) {
        return Convert.toWei(ether, Convert.Unit.ETHER).toBigInteger();
    }
    public static BigInteger gweiToWei(Long gwei) {
        return EtherUtil.convert(gwei, EtherUtil.Unit.GWEI);
    }

    public static BigDecimal gweiToEth(Long gwei) {
        return weiToEther(gweiToWei(gwei));
    }


    public static boolean isAddressValid(String address){
        String regex = "^0x[0-9a-f]{40}$";

        if(StringUtils.isBlank(address)){
            return false;
        }

        return address.toLowerCase().matches(regex);
    }

}
