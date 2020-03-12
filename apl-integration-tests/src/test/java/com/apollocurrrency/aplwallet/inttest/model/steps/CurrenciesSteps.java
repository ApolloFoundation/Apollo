package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.Currency;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.CurrenciesResponse;
import com.apollocurrency.aplwallet.api.response.CurrencyAccountsResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class CurrenciesSteps extends StepBase {
    @Step("Issue Currency with param: Type: {2}")
    public CreateTransactionResponse issueCurrency(Wallet wallet, int type, String name, String description, String code, int initialSupply, int maxSupply, int decimals) {
        int currentHeight = getBlock().getHeight();
        int issuanceHeight = currentHeight + 11;

        final int EXCHANGEABLE = 1;
        final int CONTROLLABLE = 2;
        final int RESERVABLE = 4;
        final int CLAIMABLE = 8;
        final int MINTABLE = 16;
        final int NON_SHUFFLEABLE = 32;

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.ISSUE_CURRENCY);
        param.put(ReqParam.NAME, name);
        param.put(ReqParam.CODE, code);
        param.put(ReqParam.DESCRIPTION, description);
        param.put(ReqParam.TYPE, String.valueOf(type));
        param.put(ReqParam.INITIAL_SUPPLY, String.valueOf(initialSupply));
        param.put(ReqParam.DECIMALS, String.valueOf(decimals));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.ISSUANCE_HEIGHT, "0");
        param.put(ReqParam.MAX_SUPPLY, String.valueOf(maxSupply));
        param.put(ReqParam.RESERVE_SUPPLY, "0");

        if ((type & RESERVABLE) == RESERVABLE) {
            param.put(ReqParam.MAX_SUPPLY, String.valueOf(maxSupply+50));
            param.put(ReqParam.RESERVE_SUPPLY, String.valueOf(maxSupply+50));
            param.put(ReqParam.ISSUANCE_HEIGHT, String.valueOf(issuanceHeight));
            param.put(ReqParam.MIN_RESERVE_PER_UNIT, String.valueOf(1));
        }
        if ((type & CLAIMABLE) == CLAIMABLE) {
            param.put(ReqParam.INITIAL_SUPPLY, "0");
        }
        if ((type & MINTABLE) == MINTABLE && (type & RESERVABLE) == RESERVABLE) {
            param.put(ReqParam.ALGORITHM, "2");
            param.put(ReqParam.MIN_DIFFICULTY, "1");
            param.put(ReqParam.MAX_DIFFICULTY, "2");
            param.put(ReqParam.MAX_SUPPLY, String.valueOf(maxSupply+50));
            param.put(ReqParam.RESERVE_SUPPLY, String.valueOf(maxSupply+10));
        }

        if ((type & MINTABLE) == MINTABLE && (type & RESERVABLE) != RESERVABLE) {
            param.put(ReqParam.ALGORITHM, "2");
            param.put(ReqParam.MIN_DIFFICULTY, "1");
            param.put(ReqParam.MAX_DIFFICULTY, "2");
            param.put(ReqParam.RESERVE_SUPPLY, "0");
        }

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);


    }

    @Step
    public CurrenciesResponse getAllCurrencies() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALL_CURRENCIES);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CurrenciesResponse.class);
    }

    @Step
    public Currency getCurrency(String CurrencyId) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_CURRENCY);
        param.put(ReqParam.CURRENCY, CurrencyId);
        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", Currency.class);
    }

    @Step
    public CurrencyAccountsResponse getCurrencyAccounts(String CurrencyId) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_CURRENCY_ACCOUNTS);
        param.put(ReqParam.CURRENCY, CurrencyId);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CurrencyAccountsResponse.class);
    }

    @Step
    public CreateTransactionResponse deleteCurrency(Wallet wallet, String CurrencyId) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DELETE_CURRENCY);
        param.put(ReqParam.CURRENCY, CurrencyId);
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse transferCurrency(String recipient, String currency, Wallet wallet, int units) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.TRANSFER_CURRENCY);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.RECIPIENT, recipient);
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyReserveClaim(String currency, Wallet wallet, int units) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_RESERVE_CLAIM);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyReserveIncrease(String currency, Wallet wallet, int amountPerUnitATM) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_RESERVE_INCREASE);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.AMOUNT_PER_UNIT, String.valueOf(amountPerUnitATM));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse publishExchangeOffer(String currency, Wallet wallet, int buyRateATM, int sellRateATM, int initialBuySupply, int initialSellSupply) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.PUBLISH_EXCHANGE_OFFER);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.BUY_RATE, String.valueOf(buyRateATM));
        param.put(ReqParam.SELL_RATE, String.valueOf(sellRateATM));
        param.put(ReqParam.TOTAL_BUY_LIMIT, String.valueOf(1000));
        param.put(ReqParam.TOTAL_SELL_LIMIT, String.valueOf(1000));
        param.put(ReqParam.INITIAL_BUY_SUPPLY, String.valueOf(initialBuySupply));
        param.put(ReqParam.INITIAL_SELL_SUPPLY, String.valueOf(initialSellSupply));
        param.put(ReqParam.EXPIRATION_HEIGHT, "999999999");
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencySell(String currency, Wallet wallet, int units, int rate) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_SELL);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.RATE, String.valueOf(rate));
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyBuy(String currency, Wallet wallet, int units, int rate) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_BUY);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.RATE, String.valueOf(rate));
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse scheduleCurrencyBuy(String currency, Wallet wallet, int units, int rate, String offerIssuer) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_BUY);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.RATE, String.valueOf(rate));
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public AccountCurrencyResponse getAccountCurrencies(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_CURRENCIES);
        param.put(ReqParam.ACCOUNT, wallet.getUser());
        param.put(ReqParam.INCLUDE_CURRENCY_INFO, "true");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrencyResponse.class);
    }


}
