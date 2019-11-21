package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.helper.WalletProvider;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Epic;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;

@DisplayName("Marketplace")
@Epic(value = "Marketplace")
public class TestMarketplace extends TestBaseOld {
    private String dgsName;
    private String description;
    private String tag;
    private int price;
    private int quantity;
    private final File image = TestConfiguration.getTestConfiguration().getDefaultImage();

    @BeforeEach
    @Override
    public void setUP(TestInfo testInfo) {
        super.setUP(testInfo);
        this.dgsName = RandomStringUtils.randomAlphabetic(5);
        this.description = RandomStringUtils.randomAlphabetic(5);
        StringBuilder tags =  new StringBuilder();
        String symbols = "!@#$^&*()_+{}:'./,\"";
        for (int i = 0; i < RandomUtils.nextInt(2,5) ; i++) {
            if (i > 0) {tags.append(symbols.charAt(RandomUtils.nextInt(0,symbols.length())));}
                tags.append(RandomStringUtils.randomAlphabetic(3,5));
        }
        this.tag = tags.toString();
        this.price = RandomUtils.nextInt(1,1000);
        this.quantity = RandomUtils.nextInt(10,1000);

        log.info("DGS Name: {}",dgsName);
        log.info("DGS Tag: {}",tag);
    }

    @DisplayName("DGS Listing")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    void dgsListingTest(Wallet wallet){
        CreateTransactionResponse  dgs =  dgsListing(wallet,dgsName,description,tag,quantity,price,image);
        verifyCreatingTransaction(dgs);
        verifyTransactionInBlock(dgs.getTransaction());
    }


}
