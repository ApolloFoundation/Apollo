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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;


@DisplayName("TaggedData")
@Epic(value = "TaggedData")
public class TestTaggedData extends TestBaseOld {

    private final File image = TestConfiguration.getTestConfiguration().getDefaultImage();
    private String Name;
    private String description;
    private String tag;
    private String channel;

    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) {
        super.setUp(testInfo);
        this.Name = RandomStringUtils.randomAlphabetic(5);
        this.description = RandomStringUtils.randomAlphabetic(5);
        this.channel = RandomStringUtils.randomAlphabetic(5);
        StringBuilder tags = new StringBuilder();
        String symbols = "!@$^&*()_+{}:'./,\"";
        for (int i = 0; i < RandomUtils.nextInt(2, 5); i++) {
            if (i > 0) {
                tags.append(symbols.charAt(RandomUtils.nextInt(0, symbols.length())));
            }
            tags.append(RandomStringUtils.randomAlphabetic(3, 5));
        }
        this.tag = tags.toString();

        log.info("Data Name: {}", Name);
        log.info("Data Tag: {}", tag);
        log.info("Data description: {}", description);
        log.info("Data channel: {}", channel);
    }

    @DisplayName("upload TaggedData")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    void TaggedDataTest(Wallet wallet) {
        Long tagsQuantity = getDataTagCount().getNumberOfDataTags();
        log.info("Tags quantity: {} before uploading new TaggedData", tagsQuantity);
        CreateTransactionResponse uploadData = uploadTaggedData(wallet, Name, description, tag, channel, image);
        verifyCreatingTransaction(uploadData);
        verifyTransactionInBlock(uploadData.getTransaction());
        int parsedTags = getTaggedData(uploadData.getTransaction()).getParsedTags().size();
        log.info("Tags quantity which is created after uploading new TaggedData equals {} ", parsedTags);
        assertEquals(Name, getTaggedData(uploadData.getTransaction()).getName(), "names are not the same");
        assertEquals(description, getTaggedData(uploadData.getTransaction()).getDescription(), "descriptions are not the same");
        assertEquals(channel, getTaggedData(uploadData.getTransaction()).getChannel(), "channels are not the same");
        assertEquals(tagsQuantity + parsedTags, getDataTagCount().getNumberOfDataTags(), "quantity of tags are different");
        assertEquals(Name, searchTaggedDataByName(Name).getData().get(0).getName(), "search by name doesn't work");
        log.info(" Tag from getTaggedData =  {} ", getTaggedData(uploadData.getTransaction()).getParsedTags().get(0));
        assertEquals(getTaggedData(uploadData.getTransaction()).getParsedTags().get(0), searchTaggedDataByTag(tag).getData().get(0).getParsedTags().get(0), "search by tag doesn't work");
        log.info(" Tag from searchTaggedData =  {} ", searchTaggedDataByTag(tag).getData().get(0).getParsedTags().get(0));
    }

    @DisplayName("upload TaggedData Test with extend TaggedData request")
    @ParameterizedTest(name = "{displayName} {arguments}")
    @ArgumentsSource(WalletProvider.class)
    @Disabled
    void extendTaggedDataTest(Wallet wallet) {
        Long tagsQuantity = getDataTagCount().getNumberOfDataTags();
        log.info("Tags quantity: {} before uploading new TaggedData", tagsQuantity);
        CreateTransactionResponse uploadData = uploadTaggedData(wallet, Name, description, tag, channel, image);
        verifyCreatingTransaction(uploadData);
        verifyTransactionInBlock(uploadData.getTransaction());
        int parsedTags = getTaggedData(uploadData.getTransaction()).getParsedTags().size();
        log.info("Tags quantity which is created after uploading new TaggedData equals {} ", parsedTags);
        assertEquals(Name, getTaggedData(uploadData.getTransaction()).getName(), "names are not the same");
        assertEquals(description, getTaggedData(uploadData.getTransaction()).getDescription(), "descriptions are not the same");
        assertEquals(channel, getTaggedData(uploadData.getTransaction()).getChannel(), "channels are not the same");
        assertEquals(tagsQuantity + parsedTags, getDataTagCount().getNumberOfDataTags(), "quantity of tags are different");
        assertEquals(Name, searchTaggedDataByName(Name).getData().get(0).getName(), "search by name doesn't work");
        log.info(" Tag from getTaggedData =  {} ", getTaggedData(uploadData.getTransaction()).getParsedTags().get(0));
        log.info(" Tag from searchTaggedData =  {} ", searchTaggedDataByTag(tag).getData().get(0).getParsedTags().get(0));
        assertEquals(getTaggedData(uploadData.getTransaction()).getParsedTags().get(0), searchTaggedDataByTag(tag).getData().get(0).getParsedTags().get(0), "search by tag doesn't work");
        CreateTransactionResponse extendData = extendTaggedData(wallet, uploadData.getTransaction());
        verifyCreatingTransaction(extendData);
        verifyTransactionInBlock(extendData.getTransaction());
    }


}
