package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.config.WalletClientProducer;
import com.apollocurrency.aplwallet.apl.eth.utils.Web3jUtils;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.testutil.FileLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.slf4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

@EnableWeld
class EthereumWalletServiceTest {

    private static final Logger log = getLogger(EthereumWalletServiceTest.class);

    @Inject
    private EthereumWalletService ethereumWalletService;
    @Inject
    private PropertiesHolder propertiesHolder;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(WalletClientProducer.class, EthereumWalletService.class)
            .addBeans(MockBean.of(initProperties(), PropertiesHolder.class))
            .build();

    private static String fromAddress = "0x1a3B4eAdb0de9Ba2A32F11144f8C5Ce66F721E0F";
    private static String toAddress = "0xBd411c8498A75ce459b5eC18d976A6CDE49CC9a5";
    private static String password = "candy maple cake sugar pudding cream honey rich smooth crumble sweet treat";

//    @Test
    void getBalance() {
        BigDecimal balance1 = ethereumWalletService.getBalanceEther(fromAddress);
        BigDecimal balance2 = ethereumWalletService.getBalanceEther(toAddress);

        log.info(balance1.toString());
        log.info(balance2.toString());

        assertNotNull(balance1);
        assertNotNull(balance2);
        assertTrue(balance1.compareTo(BigDecimal.ZERO) == 1);
        assertTrue(balance2.compareTo(BigDecimal.ZERO) == 1);
    }

//    @Test
    public void testTransfer() throws Exception {
        BigInteger transferAmount = Web3jUtils.etherToWei(new BigDecimal("1000"));
        FileLoader fileLoader = new FileLoader();

        BigInteger balanceBefore = ethereumWalletService.getBalanceWei(toAddress);

        Credentials credentials = WalletUtils.loadCredentials(
                    password,
                    fileLoader.getFile("keystore/UTC--2019-02-06T16-56-34.524090000Z--1a3b4eadb0de9ba2a32f11144f8c5ce66f721e0f"));

        String privateKeyGenerated = credentials.getEcKeyPair().getPrivateKey().toString(16);

        log.info("Private key: " + privateKeyGenerated);
        credentials = Credentials.create(privateKeyGenerated);

        String transactionHash = ethereumWalletService.transfer(fromAddress, credentials, toAddress, transferAmount);

        // step 4: wait for the confirmation of the network
        TransactionReceipt receipt = ethereumWalletService.waitForReceipt(transactionHash);

        BigInteger gasUsed = receipt.getCumulativeGasUsed();
        BigInteger balanceAfter = ethereumWalletService.getBalanceWei(toAddress);

        log.info("Tx cost: " + gasUsed + " Gas (" + Web3jUtils.weiToEther(gasUsed.multiply(Web3jUtils.GAS_PRICE)) +" Ether)\n");
        log.info("Balance after Tx: " + balanceAfter);


        assertTrue(balanceAfter.subtract(balanceBefore).compareTo(transferAmount) >= 0);

    }


    private PropertiesHolder initProperties() {
        PropertiesHolder propertiesHolder = new PropertiesHolder();
        Properties properties = new Properties();
        properties.put("eth.node.url", "http://localhost");
        properties.put("eth.node.port", "8080");
        propertiesHolder.init(properties);
        return propertiesHolder;
    }
}