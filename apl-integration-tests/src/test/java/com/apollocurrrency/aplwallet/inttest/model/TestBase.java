package com.apollocurrrency.aplwallet.inttest.model;


import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetPeersIpResponse;
import com.apollocurrrency.aplwallet.inttest.helper.RestHelper;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.google.common.collect.ImmutableMap;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.params.CoreConnectionPNames;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static com.github.automatedowl.tools.AllureEnvironmentWriter.allureEnvironmentWriter;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;


public abstract class TestBase {
    public static final Logger log = LoggerFactory.getLogger(TestBase.class);
    public static RetryPolicy retryPolicy = new RetryPolicy()
        .retryWhen(false)
        .withMaxRetries(50)
        .withDelay(10, TimeUnit.SECONDS);
    static RestHelper restHelper = RestHelper.getRestHelper();

    private TestInfo testInfo;
    private static RestAssuredConfig  config = RestAssured.config()
        .httpClient(HttpClientConfig.httpClientConfig()
            .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
            .setParam(CoreConnectionPNames.SO_TIMEOUT, 10000));
    private String path = "/apl";

    @BeforeAll
    public synchronized static void initAll() {
        log.info("Preconditions started");
        String secretFilePath = Objects.requireNonNull(TestBase.class.getClassLoader()
            .getResource(TestConfiguration.getTestConfiguration()
            .getVaultWallet().getUser()))
            .getPath();

        allureEnvironmentWriter(
            ImmutableMap.<String, String>builder()
                .put("URL", TestConfiguration.getTestConfiguration().getBaseURL())
                .build());

            importSecretFileSetUp(secretFilePath, TestConfiguration.getTestConfiguration().getVaultWallet().getPass());
            startForgingSetUp();
            setUpTestData();

        log.info("Preconditions finished");
    }

    @BeforeEach
    @Step("Before test")
    public void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
        log.info("Test started: "+ testInfo.getDisplayName());
    }


    @AfterEach
    @Step("AfterEach")
    public void tearDown() {
        log.info("Test finished: "+testInfo.getDisplayName());
    }

    @AfterAll
    @Step("AfterAll")
    static void afterAll() {

    }

    //Static need for a BeforeAll method
    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ)
    protected synchronized static void importSecretFileSetUp(String pathToSecretFile, String pass) {
        try {
        String path = "/rest/keyStore/upload";
        given().log().all()
                .spec(restHelper.getPreconditionSpec())
                .header("Content-Type", "multipart/form-data")
                .multiPart("keyStore", new File(pathToSecretFile))
                .formParam("passPhrase", pass)
                .when()
                .post(path);
        }catch (Exception e){
            fail("Import secret file failed: "+e.getMessage());
        }
    }

    public static void setUpTestData() {
      try {
        log.info("Balance check started");
        CreateTransactionResponse transactionResponse;
        if (getBalanceSetUP(TestConfiguration.getTestConfiguration().getStandartWallet()).getBalanceATM() < 90000000000000L) {
            log.info("Send money on: "+TestConfiguration.getTestConfiguration().getStandartWallet());

            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getGenesisWallet(),
                    TestConfiguration.getTestConfiguration().getStandartWallet().getUser(), 1000000);

            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());

            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getStandartWallet(),
                TestConfiguration.getTestConfiguration().getStandartWallet().getUser(), 10);

            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());

        }

        if (getBalanceSetUP(TestConfiguration.getTestConfiguration().getVaultWallet()).getBalanceATM() < 90000000000000L) {
            log.info("Send money on: "+TestConfiguration.getTestConfiguration().getVaultWallet());

            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getGenesisWallet(),
                    TestConfiguration.getTestConfiguration().getVaultWallet().getUser(), 1000000);
            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());

            log.info("Verify account: "+TestConfiguration.getTestConfiguration().getVaultWallet());
            transactionResponse = sendMoneySetUp(TestConfiguration.getTestConfiguration().getVaultWallet(),
                TestConfiguration.getTestConfiguration().getVaultWallet().getUser(), 10);
            verifyTransactionInBlockSetUp(transactionResponse.getTransaction());
        }
        }catch (Exception e){
                fail("Send money failed: " + e.getMessage());
        }
    }

    protected synchronized static CreateTransactionResponse sendMoneySetUp(Wallet wallet, String recipient, int moneyAmount) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE,ReqType.SEND_MONEY);
        param.put(ReqParam.RECIPIENT, recipient);
        param.put(ReqParam.AMOUNT_ATM, moneyAmount + "000000000");
        param.put(ReqParam.FEE, "500000000");
        param.put(ReqParam.DEADLINE, "1440");

        String path = "/apl";
        return given().log().all()
            .spec(restHelper.getPreconditionSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("",CreateTransactionResponse.class);

    }

    protected synchronized static boolean verifyTransactionInBlockSetUp(String transaction) {
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
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE,ReqType.GET_TRANSACTION);
        param.put(ReqParam.TRANSACTION, transaction);
        String path = "/apl";
        return given().log().all()
            .spec(restHelper.getPreconditionSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("",TransactionDTO.class);
    }

    protected synchronized static BalanceDTO getBalanceSetUP(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BALANCE);
        param = restHelper.addWalletParameters(param, wallet);

        String path = "/apl";
        return given().log().all()
            .spec(restHelper.getPreconditionSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .extract().body().jsonPath()
            .getObject("",BalanceDTO.class);
    }

    private static void startForgingSetUp() {

        List<String> peersIp;
        String path = "/rest/networking/peer/all";
            List<String> peers = given().log().uri()
                    .spec(restHelper.getPreconditionSpec())
                    .when()
                    .get(path).as(GetPeersIpResponse.class).getPeers();

            if (peers.size() > 0) {
                try {
                HashMap<String, String> param = new HashMap();
                param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCKCHAIN_STATUS);
                path = "/apl";
                BlockchainInfoDTO status = given().config(config).log().all()
                        .spec(restHelper.getPreconditionSpec())
                        .contentType(ContentType.URLENC)
                        .formParams(param)
                        .when()
                        .post(path)
                        .then()
                        .assertThat().statusCode(200)
                    .extract().body().jsonPath()
                    .getObject("",BlockchainInfoDTO.class);

                peersIp = TestConfiguration.getTestConfiguration().getHostsByChainID(status.getChainId());
                }catch (Exception e){
                    peersIp = TestConfiguration.getTestConfiguration().getPeers();
                    log.warn("FAILED: GET BLOCKCHAIN STATUS. " + e.getMessage());
                }

            } else {
                peersIp = TestConfiguration.getTestConfiguration().getPeers();
            }


            if (peersIp != null && peersIp.size() > 0) {
                boolean isForgingEnableOnGen = false;
                    log.info("Check Forging on peers");
                    for (String ip : peersIp) {
                    log.info("Check Forging on: " + ip);
                    HashMap<String, String> param = new HashMap();
                    param.put(ReqType.REQUEST_TYPE,ReqType.GET_FORGING);
                    param.put(ReqParam.ADMIN_PASSWORD, getTestConfiguration().getAdminPass());
                    path = "/apl";
                 try {
                       ForgingResponse forgingResponse = given().config(config).log().all()
                                .baseUri(String.format("http://%s:%s", ip, 7876))
                                .contentType(ContentType.URLENC)
                                .formParams(param)
                                .when()
                                .post(path)
                                .then()
                                .assertThat().statusCode(200)
                                .extract().body().jsonPath()
                                .getObject("",ForgingResponse.class);

                        if (forgingResponse.getGenerators().size() > 0) {
                            log.info("Forgers founded");
                            isForgingEnableOnGen = true;
                            break;
                        }else{
                            log.info("Forgers not founded on: "+ip);
                        }
                    } catch (Exception ex) {
                        log.warn("FAILED: Get Forging. " + ex.getMessage());
                    }

                    }
                try {
                   if (!isForgingEnableOnGen) {
                       log.info("Start forging on APL-NZKH-MZRE-2CTT-98NPZ account");
                       HashMap<String, String> param = new HashMap();
                       param.put(ReqType.REQUEST_TYPE, ReqType.START_FORGING);
                       param.put(ReqParam.ADMIN_PASSWORD, getTestConfiguration().getAdminPass());
                       param = restHelper.addWalletParameters(param, TestConfiguration.getTestConfiguration().getGenesisWallet());

                       path = "/apl";
                       given().log().all()
                           .spec(restHelper.getPreconditionSpec())
                           .contentType(ContentType.URLENC)
                           .formParams(param)
                           .when()
                           .post(path)
                           .then()
                           .assertThat().statusCode(200)
                           .extract().body().jsonPath()
                           .getObject("",ForgingDetails.class);
                    }
                } catch (Exception ex) {
                    log.warn("FAILED: Check Forging on peers. " + ex.getMessage());
                }
            }
        }


    @Step
    public boolean verifyTransactionInBlock(String transaction) {
        boolean inBlock = false;
        if (transaction != null) {
            try {
                inBlock = Failsafe.with(retryPolicy).get(() -> getTransaction(transaction).getConfirmations() >= 0);
            } catch (Exception e) {
                fail("Transaction does't add to block. Transaction " + transaction + " Exception: " + e.getMessage());
            }
            assertTrue(inBlock, String.format("Transaction %s in block: ", transaction));
        }else{
            fail("Transaction is null");
        }
        return inBlock;
    }

    @Step
    @DisplayName("Get Transaction")
    public TransactionDTO getTransaction(String transaction) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_TRANSACTION);
        param.put(ReqParam.TRANSACTION, transaction);

        return given()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", TransactionDTO.class);
    }


}
