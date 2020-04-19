package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrency.aplwallet.api.response.GetPeersIpResponse;
import com.apollocurrrency.aplwallet.inttest.helper.RestHelper;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.addParameters;
import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.getInstanse;
import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccount;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountId;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getBalance;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getForging;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.startForging;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public abstract class TestBase implements ITest {
    public static final Logger log = LoggerFactory.getLogger(TestBase.class);
    public static TestInfo testInfo;
    protected static RetryPolicy retryPolicy;
    protected static RestHelper restHelper;
    protected static ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void initAll() {
        log.info("Preconditions started");
        TestConfiguration.getTestConfiguration();
        retryPolicy = new RetryPolicy()
            .retryWhen(false)
            .withMaxRetries(30)
            .withDelay(5, TimeUnit.SECONDS);
        restHelper = new RestHelper();
        ClassLoader classLoader = TestBase.class.getClassLoader();
        String secretFilePath = Objects.requireNonNull(classLoader.getResource(TestConfiguration.getTestConfiguration().getVaultWallet().getUser())).getPath();
        try {
            importSecretFileSetUp(secretFilePath, "1");
            startForgingSetUp();
            setUpTestData();
        } catch (Exception ex) {
            fail("Precondition FAILED: " + ex.getMessage(), ex);
        }
        log.info("Preconditions finished");
    }

    @AfterAll
    @Step("AfterAll")
    static void afterAll() {

    }

    //Static need for a BeforeAll method
    private static void importSecretFileSetUp(String pathToSecretFile, String pass) {
        String path = "/rest/keyStore/upload";
        given().log().all()
            .spec(restHelper.getPreconditionSpec())
            .header("Content-Type", "multipart/form-data")
            .multiPart("keyStore", new File(pathToSecretFile))
            .formParam("passPhrase", pass)
            .when()
            .post(path);
    }

    public static void setUpTestData() {
        CreateTransactionResponse transactionResponse;
        if (getBalanceSetUP(TestConfiguration.getTestConfiguration().getStandartWallet()).getBalanceATM() < 90000000000000L) {
            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getGenesisWallet(),
                TestConfiguration.getTestConfiguration().getStandartWallet().getUser(), 1000000);
            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());
        }

        transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getStandartWallet(),
            TestConfiguration.getTestConfiguration().getStandartWallet().getUser(), 10);
        verifyTransactionInBlockSetUp(transactionResponse.getTransaction());

        if (getBalanceSetUP(TestConfiguration.getTestConfiguration().getVaultWallet()).getBalanceATM() < 90000000000000L) {
            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getGenesisWallet(),
                TestConfiguration.getTestConfiguration().getVaultWallet().getUser(), 1000000);
            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());
        }

        transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getVaultWallet(),
            TestConfiguration.getTestConfiguration().getVaultWallet().getUser(), 10);
        verifyTransactionInBlockSetUp(transactionResponse.getTransaction());
    }

    private static CreateTransactionResponse sendMoneySetUp(Wallet wallet, String recipient, int moneyAmount) {
        addParameters(RequestType.requestType, RequestType.sendMoney);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount + "00000000");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    private static boolean verifyTransactionInBlockSetUp(String transaction) {
        boolean inBlock = false;
        try {
            inBlock = Failsafe.with(retryPolicy).get(() -> getTransactionSetUP(transaction).getConfirmations() >= 0);
            assertTrue(inBlock);
        } catch (Exception e) {
            fail("Transaction does't add to block. Transaction " + transaction);
        }
        assertTrue(inBlock, "Transaction does't add to block. Transaction " + transaction);
        return inBlock;
    }

    private static TransactionDTO getTransactionSetUP(String transaction) {
        addParameters(RequestType.requestType, RequestType.getTransaction);
        addParameters(Parameters.transaction, transaction);
        return getInstanse(TransactionDTO.class);
    }

    private static BalanceDTO getBalanceSetUP(Wallet wallet) {
        addParameters(RequestType.requestType, getBalance);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(BalanceDTO.class);
    }

    private static void startForgingSetUp() {
        List<String> peersIp;
        String path;
        if (TestConfiguration.getTestConfiguration().getBaseURL().equals("localhost")) {
            path = "/rest/networking/peer/all";
            List<String> peers = given().log().uri()
                .spec(restHelper.getPreconditionSpec())
                .when()
                .get(path).as(GetPeersIpResponse.class).getPeers();

            if (peers.size() > 0) {
                //TODO: Change on REST Easy
                HashMap<String, String> param = new HashMap();
                param.put(RequestType.requestType.toString(), RequestType.getBlockchainStatus.toString());

                path = "/apl";
                BlockchainInfoDTO status = given().log().all()
                    .spec(restHelper.getPreconditionSpec())
                    .contentType(ContentType.URLENC)
                    .formParams(param)
                    .when()
                    .post(path)
                    .then()
                    .assertThat().statusCode(200)
                    .extract().body().jsonPath()
                    .getObject("", BlockchainInfoDTO.class);

                peersIp = TestConfiguration.getTestConfiguration().getHostsByChainID(status.getChainId());

            } else {
                peersIp = TestConfiguration.getTestConfiguration().getPeers();
            }
            if (peersIp != null && peersIp.size() > 0) {

                boolean isForgingEnableOnGen = false;
                try {

                    for (String ip : peersIp) {

                        HashMap<String, String> param = new HashMap();
                        param.put(RequestType.requestType.toString(), RequestType.getForging.toString());
                        param.put(Parameters.adminPassword.toString(), getTestConfiguration().getAdminPass());

                        path = "/apl";
                        ForgingResponse forgingResponse = given().log().all()
                            .baseUri(String.format("http://%s:%s", ip, 7876))
                            .contentType(ContentType.URLENC)
                            .formParams(param)
                            .when()
                            .post(path)
                            .then()
                            .assertThat().statusCode(200)
                            .extract().body().jsonPath()
                            .getObject("", ForgingResponse.class);

                        if (forgingResponse.getGenerators().size() > 0) {
                            isForgingEnableOnGen = true;
                            break;
                        }

                    }

                    if (!isForgingEnableOnGen) {
                        addParameters(RequestType.requestType, startForging);
                        addParameters(Parameters.wallet, TestConfiguration.getTestConfiguration().getGenesisWallet());
                        addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
                        getInstanse(ForgingDetails.class);
                    }
                } catch (Exception ex) {
                    log.error("FAILED: Get Forging. " + ex.getMessage());
                }
            }

        } else {
            addParameters(RequestType.requestType, getForging);
            addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
            ForgingResponse forgingResponse = getInstanse(ForgingResponse.class);

            if (forgingResponse.getGenerators() != null && forgingResponse.getGenerators().size() == 0) {
                System.out.println("Start Forging on APL-NZKH-MZRE-2CTT-98NPZ");
                addParameters(RequestType.requestType, startForging);
                addParameters(Parameters.wallet, TestConfiguration.getTestConfiguration().getGenesisWallet());
                addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
                getInstanse(ForgingDetails.class);
            }
        }

    }

    private static void checkForgingAccountsBalance() {
        for (int i = 1; i < 200; i++) {
            addParameters(RequestType.requestType, getAccountId);
            addParameters(Parameters.secretPhrase, i);
            AccountDTO accountID = getInstanse(AccountDTO.class);
            addParameters(RequestType.requestType, getAccount);
            addParameters(Parameters.account, accountID.getAccount());
            GetAccountResponse account = getInstanse(GetAccountResponse.class);
            if (Long.valueOf(account.getBalanceATM()) < 10000000000000L) {
                sendMoneySetUp(TestConfiguration.getTestConfiguration().getGenesisWallet(), account.getAccountRS(), 5000000);
            }
        }
    }

    @BeforeEach
    @Step("Before test")
    public void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    @AfterEach
    @Step("AfterEach")
    public void tearDown() {
        this.testInfo = null;
    }
}
