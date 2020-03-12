package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.CreateDexOrderResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.DexAccountInfoResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.FilledOrdersResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;

public class DexSteps extends StepBase {
    private final int ETH = 1;
    private final int PAX = 2;
    private final int ORDER_SELL = 1;
    private final int ORDER_BUY = 0;

    //TODO add: boolean isAvailableForNow, int minAskPrice, int maxBidPrice
    @Step("Get Dex Orders with param: Type {orderType}, Pair Currency {pairCurrency}, Order Status {status}, AccountId {accountId}")
    public List<DexOrderDto> getDexOrders(String orderType, String pairCurrency, String status, String accountId) {
        String path = "/rest/dex/offers";

        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ORDER_TYPE, orderType);
        param.put(ReqParam.PAIR_CURRENCY, pairCurrency);
        param.put(ReqParam.STATUS, status);
        param.put(ReqParam.ACCOUNT_ID, accountId);

        return given().log().all()
            .spec(restHelper.getSpec())
            .formParams(param)
            .when()
            .get(path)
            .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Dex Orders with param: Order Status {status}, AccountId {accountId}")
    public List<DexOrderDto> getDexOrders(String status, String accountId) {

        String path = "/rest/dex/offers";
        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.STATUS, status);
        param.put(ReqParam.ACCOUNT_ID, accountId);

        return given().log().all()
            .spec(restHelper.getSpec())
            .formParams(param)
            .when()
            .get(path)
            .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step
    public List<DexOrderDto> getDexOrders(String accountId) {
        String path = "/rest/dex/offers";

        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ACCOUNT_ID, accountId);

        return given().log().all()
            .spec(restHelper.getSpec())
            .formParams(param)
            .when()
            .get(path)
            .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Dex Orders")
    public List<DexOrderDto> getDexOrders() {
        String path = "/rest/dex/offers";
        return given().log().all()
            .spec(restHelper.getSpec())
            .when()
            .get(path)
            .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Dex Order")
    public DexOrderDto getDexOrder(String orderId) {
        HashMap<String, String> param = new HashMap();
        param.put("orderId", orderId);

        String path = "/rest/dex/orders/" + orderId;
        return given().log().all()
            .spec(restHelper.getSpec())
            .when()
            .get(path)
            .as(DexOrderDto.class);
    }


    @Step("Get Dex History (CLOSED ORDERS) with param: Account: {0}, Pair: {1} , Type: {2}")
    public List<DexOrderDto> getDexHistory(String account, boolean isEth, boolean isSell) {
        String path = "/rest/dex/offers";


        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ACCOUNT_ID, account);
        param.put(ReqParam.STATUS, "5");

        int pair = (isEth)? ETH : PAX;
        int orderType = (isSell)? ORDER_SELL : ORDER_BUY;

        param.put(ReqParam.PAIR_CURRENCY, String.valueOf(pair));
        param.put(ReqParam.ORDER_TYPE, String.valueOf(orderType));

        return given().log().all()
            .spec(restHelper.getSpec())
            .formParams(param)
            .when()
            .get(path)
            .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Dex History (CLOSED ORDERS) for certain account")
    public List<DexOrderDto> getDexHistory(String account) {
        String path = "/rest/dex/offers";

        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ACCOUNT_ID, account);
        param.put(ReqParam.STATUS, "5");

        return given().log().all()
            .spec(restHelper.getSpec())
            .formParams(param)
            .when()
            .get(path)
            .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Eth Gas Info")
    public EthGasInfoResponse getEthGasInfo() {
        String path = "/rest/dex/ethInfo";
        return given().log().uri()
            .spec(restHelper.getSpec())
            .when()
            .get(path).as(EthGasInfoResponse.class);
    }

    @Step("Get history for certain Currency and period")
    public TradingDataOutputDTO getDexTradeInfo(boolean isEth, String resolution) {
        String path = "/rest/dex/history";
        HashMap<String, String> param = new HashMap();
        Date today = Calendar.getInstance().getTime();
        long epochTime = today.getTime();

        param.put(ReqParam.RESOLUTION, resolution);
        param.put(ReqParam.FROM, String.valueOf(epochTime/1000-864000));
        param.put(ReqParam.TO, String.valueOf(epochTime/1000));
        log.info("start = " + (epochTime/1000-864000));
        log.info("finish = " + (epochTime/1000));

        String pair = (isEth)? "APL_ETH" : "APL_PAX";
        param.put(ReqParam.SYMBOL, pair);

        return given().log().all()
            .spec(restHelper.getSpec())
            .formParams(param)
            .when()
            .get(path)
            .getBody().as(TradingDataOutputDTO.class);
    }

    //TODO: edit to new RESPONSEDTO, not STRING
    @Step("dexGetBalances endpoint returns cryptocurrency wallets' (ETH/PAX) balances")
    public Account2FAResponse getDexBalances(String ethAddress) {
        String path = "/rest/dex/balance";

        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ETH, ethAddress);

        return given().log().all()
            .spec(restHelper.getSpec())
            .formParams(param)
            .when()
            .get(path)
            .getBody().as(Account2FAResponse.class);
    }

    @Step
    public WithdrawResponse dexWidthraw(String fromAddress, Wallet wallet, String toAddress, String amount, String transferFee, boolean isEth) {
        String path = "/rest/dex/withdraw";

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqParam.FROM_ADDRESS, fromAddress);
        param.put(ReqParam.TO_ADDRESS, toAddress);
        param.put(ReqParam.AMOUNT, amount);
        param.put(ReqParam.TRANSFER_FEE, transferFee);
        final int eth = 1;
        final int pax = 2;

        int cryptocurrency = (isEth)? eth : pax;
        param.put(ReqParam.CRYPTO_CURRENCY, String.valueOf(cryptocurrency));

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .as(WithdrawResponse.class);

    }

    @Step
    public CreateTransactionResponse dexCancelOrder(String orderId, Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        String path = "/rest/dex/offer/cancel";

        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqParam.ORDER_ID, orderId);
        param.put(ReqParam.FEE, "100000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path).as(CreateTransactionResponse.class);
    }

    @Step
    public CreateDexOrderResponse createDexOrder(String pairRate, String offerAmount, Wallet wallet, boolean isBuyOrder, boolean isEth) {
        String path = "/rest/dex/offer";
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        int offerType = (isBuyOrder) ? ORDER_BUY : ORDER_SELL;
        int pairCurrency = (isEth) ? ETH : PAX;

        param.put(ReqParam.OFFER_TYPE, String.valueOf(offerType));
        param.put(ReqParam.PAIR_CURRENCY, String.valueOf(pairCurrency));

        param.put(ReqParam.PAIR_RATE, pairRate);
        param.put(ReqParam.OFFER_AMOUNT, offerAmount + "000000000");
        param.put(ReqParam.ETH_WALLET_ADDRESS, wallet.getEthAddress());
        param.put(ReqParam.AMOUNT_OF_TIME, "86400");
        param.put(ReqParam.FEE, "200000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .as(CreateDexOrderResponse.class);
    }


    @Step
    public CreateDexOrderResponse createDexOrderWithAmountOfTime(String pairRate, String offerAmount, Wallet wallet, boolean isBuyOrder, boolean isEth, String amountOfTime) {
        String path = "/rest/dex/offer";
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        int offerType = (isBuyOrder) ? ORDER_BUY : ORDER_SELL;
        int pairCurrency = (isEth) ? ETH : PAX;

        param.put(ReqParam.OFFER_TYPE, String.valueOf(offerType));
        param.put(ReqParam.PAIR_CURRENCY, String.valueOf(pairCurrency));

        param.put(ReqParam.PAIR_RATE, pairRate);
        param.put(ReqParam.OFFER_AMOUNT, offerAmount + "000000000");
        param.put(ReqParam.ETH_WALLET_ADDRESS, wallet.getEthAddress());
        param.put(ReqParam.AMOUNT_OF_TIME, amountOfTime);
        param.put(ReqParam.FEE, "200000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path).as(CreateDexOrderResponse.class);
    }


    @Step
    public List<FilledOrdersResponse> getFilledOrders(){
        String path = "/rest/dex/eth/filled-orders";
        return given().log().all()
            .spec(restHelper.getSpec())
            .when().log().body()
            .get(path)
            .getBody().jsonPath().getList("", FilledOrdersResponse.class);
    }

    @Step
    public List<FilledOrdersResponse> getActiveDeposits(){
        String path = "/rest/dex/eth/active-deposits";
        return given().log().all()
            .spec(restHelper.getSpec())
            .when().log().body()
            .get(path)
            .getBody().jsonPath().getList("", FilledOrdersResponse.class);
    }

    @Step
    public DexAccountInfoResponse logInDex (Wallet wallet){
        String path = "/rest/keyStore/accountInfo";

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param, wallet);
        param.put(ReqParam.ACCOUNT, wallet.getAccountId());
        param.put(ReqParam.PASS_PHRASE, wallet.getPass());

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .log().body()
            .post(path)
            .as(DexAccountInfoResponse.class);
    }

}
