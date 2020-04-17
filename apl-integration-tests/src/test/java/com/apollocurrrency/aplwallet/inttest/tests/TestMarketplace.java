package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.DGSGoodsDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Marketplace")
@Epic(value = "Marketplace")
public class TestMarketplace extends TestBaseOld {
    private final File image = TestConfiguration.getTestConfiguration().getDefaultImage();
    //TODO: Need implement in test "Purchase"
    private final int DELIVERY_TYPE_GOODS_TO_ENCRYPT = 1;
    private final int DELIVERY_TYPE_GOODS_IS_TEXT = 2;
    private final int DELIVERY_TYPE_GOODS_DATA = 3;
    private final int DELIVERY_TYPE_GOODS_NONCE = 4;
    String symbols = "!@$^&*()_+{}:'./,\"";
    private String dgsName;
    private String description;
    private String tag;
    private int price;
    private int quantity;

    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) {
        super.setUp(testInfo);
        this.dgsName = RandomStringUtils.randomAlphabetic(5);
        this.description = RandomStringUtils.randomAlphabetic(5);
        StringBuilder tags = new StringBuilder();
        for (int i = 0; i < RandomUtils.nextInt(2, 5); i++) {
            if (i > 0) {
                tags.append(symbols.charAt(RandomUtils.nextInt(0, symbols.length())));
            }
            tags.append(RandomStringUtils.randomAlphabetic(3, 5));
        }
        this.tag = tags.toString();
        this.price = RandomUtils.nextInt(1, 1000);
        this.quantity = RandomUtils.nextInt(10, 1000);

        log.info("DGS Name: {}", dgsName);
        log.info("DGS Tag: {}", tag);
        log.info("DGS Quantity: {}", quantity);
    }

    @DisplayName("DGS Listing -> Delisting")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    void dgsListingTest(Wallet wallet) {
        CreateTransactionResponse dgs = dgsListing(wallet, dgsName, description, tag, quantity, price, image);
        verifyCreatingTransaction(dgs);
        verifyTransactionInBlock(dgs.getTransaction());
        CreateTransactionResponse dgsDelisting = dgsDelisting(wallet, dgs.getTransaction());
        verifyCreatingTransaction(dgsDelisting);
    }

    @DisplayName("DGS Quantity and Price Change")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    void quantityChangeTest(Wallet wallet) {
        final int deltaQuantity = 10;
        final int newPrice = 10;
        CreateTransactionResponse dgs = dgsListing(wallet, dgsName, description, tag, quantity, price, image);
        verifyCreatingTransaction(dgs);
        verifyTransactionInBlock(dgs.getTransaction());

        CreateTransactionResponse dgsQuantityChange = dgsQuantityChange(wallet, dgs.getTransaction(), deltaQuantity);
        verifyCreatingTransaction(dgsQuantityChange);
        verifyTransactionInBlock(dgsQuantityChange.getTransaction());

        CreateTransactionResponse dgsPriceChange = dgsPriceChange(wallet, dgs.getTransaction(), newPrice);
        verifyCreatingTransaction(dgsPriceChange);
        verifyTransactionInBlock(dgsPriceChange.getTransaction());

        DGSGoodsDTO goodsDTO = getDGSGood(dgs.getTransaction());
        assertEquals(quantity + deltaQuantity, goodsDTO.getQuantity());
        assertEquals(newPrice, goodsDTO.getPriceATM());
    }

    @DisplayName("DGS Purchase -> Delivery -> Feedback -> Refund")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    void dgsPurchaseTest(Wallet wallet) {
        Wallet randomWallet = getRandomStandartWallet();
        DGSGoodsDTO goodsDTO = null;
        CreateTransactionResponse delivery = null;
        CreateTransactionResponse purchase = null;
        CreateTransactionResponse feedback = null;
        CreateTransactionResponse refund = null;

        // min value - 165
        int deliveryDeadlineTimeInHours = 165;

        CreateTransactionResponse dgs = dgsListing(wallet, dgsName, description, tag, quantity, price, image);
        verifyCreatingTransaction(dgs);
        verifyTransactionInBlock(dgs.getTransaction());
        goodsDTO = getDGSGood(dgs.getTransaction());
        log.info("DGS ID: {}", dgs.getTransaction());

        purchase = dgsPurchase(randomWallet, dgs.getTransaction(), goodsDTO.getPriceATM(), 1, deliveryDeadlineTimeInHours);
        verifyCreatingTransaction(purchase);
        verifyTransactionInBlock(purchase.getTransaction());
        log.info("Purchase ID: {}", purchase.getTransaction());

        delivery = dgsDelivery(wallet, purchase.getTransaction(), RandomStringUtils.randomAlphabetic(5), 0);
        verifyCreatingTransaction(delivery);
        verifyTransactionInBlock(delivery.getTransaction());
        log.info("Delivery ID: {}", delivery.getTransaction());

        feedback = dgsFeedback(randomWallet, purchase.getTransaction(), RandomStringUtils.randomAlphabetic(3, 5) + symbols.charAt(RandomUtils.nextInt(0, symbols.length())));
        verifyCreatingTransaction(feedback);
        verifyTransactionInBlock(feedback.getTransaction());
        log.info("Feedback ID: {}", feedback.getTransaction());

        refund = dgsRefund(wallet, purchase.getTransaction(), 1, RandomStringUtils.randomAlphabetic(3, 5) + symbols.charAt(RandomUtils.nextInt(0, symbols.length())));
        verifyCreatingTransaction(refund);
        verifyTransactionInBlock(refund.getTransaction());
        log.info("Refund ID: {}", refund.getTransaction());


    }

    @Step
    private Wallet getRandomStandartWallet() {
        String randomPass = String.valueOf(RandomUtils.nextInt(1, 199));
        Wallet wallet = new Wallet(getAccountId(randomPass).getAccountRS(), randomPass);
        log.info(String.format("Standard Wallet: %s pass: %s", wallet.getUser(), wallet.getPass()));
        return wallet;
    }

}
