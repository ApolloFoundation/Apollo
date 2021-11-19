/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.asset;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.AssetTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;
import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ASSET_DESCRIPTION_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@Tag("slow")
class AssetTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, Map.of("asset", List.of("name,description")));

    AssetTable table;
    AssetTestData td;
    AccountTestData accountTestData;
    Event<FullTextOperationData> fullTextOperationDataEvent = mock(Event.class);

    Comparator<Asset> assetComparator = Comparator
        .comparing(Asset::getId)
        .thenComparing(Asset::getAccountId);

    @BeforeEach
    void setUp() {
        when(fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})).thenReturn(fullTextOperationDataEvent);
        table = new AssetTable(dbExtension.getDatabaseManager(), fullTextOperationDataEvent);
        td = new AssetTestData();
        accountTestData = new AccountTestData();
        dbExtension.cleanAndPopulateDb();
    }

    @Tag("skip-fts-init")
    @Test
    void testLoad() {
        Asset asset = table.get(table.getDbKeyFactory().newKey(td.ASSET_0));
        assertNotNull(asset);
        assertEquals(td.ASSET_0, asset);
    }

    @Tag("skip-fts-init")
    @Test
    void testLoad_returnNull_ifNotExist() {
        Asset asset = table.get(table.getDbKeyFactory().newKey(td.ASSET_NEW));
        assertNull(asset);
    }

    @Tag("skip-fts-init")
    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        Asset previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ASSET_NEW));
        Asset actual = table.get(table.getDbKeyFactory().newKey(td.ASSET_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.ASSET_NEW.getAccountId(), actual.getAccountId());
        assertEquals(td.ASSET_NEW.getId(), actual.getId());
    }

    @Tag("skip-fts-init")
    @Test
    void testSave_MaxDescriptionLength() {
        String description = RandomStringUtils.randomAlphabetic(MAX_ASSET_DESCRIPTION_LENGTH);
        Asset asset = td.ASSET_NEW;
        asset.setDescription(description);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ASSET_NEW));
        Asset actual = table.get(table.getDbKeyFactory().newKey(td.ASSET_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.ASSET_NEW.getAccountId(), actual.getAccountId());
        assertEquals(td.ASSET_NEW.getId(), actual.getId());
    }

    @Tag("skip-fts-init")
    @Test
    void testSave_OverDescriptionLength() {
        String description = RandomStringUtils.randomAlphabetic(MAX_ASSET_DESCRIPTION_LENGTH + 1);
        Asset asset = td.ASSET_NEW;
        asset.setDescription(description);

        assertThrows(RuntimeException.class, () -> {
            DbUtils.checkAndRunInTransaction(dbExtension, (conn) -> table.insert(td.ASSET_NEW));
        });
    }

    @Tag("skip-fts-init")
    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        Asset previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_1));
        assertNotNull(previous);
        previous.setQuantityATU(previous.getQuantityATU() + 100);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        Asset actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertEquals(100, actual.getQuantityATU() - td.ASSET_1.getQuantityATU());
        assertEquals(previous.getQuantityATU(), actual.getQuantityATU());
        assertEquals(previous.getId(), actual.getId());
    }

    @Tag("skip-fts-init")
    @Test
    void testDefaultSort() {
        assertNotNull(table.defaultSort());
        List<Asset> expectedAll = td.ALL_ASSETS_ORDERED_BY_ID.stream().sorted(assetComparator).collect(Collectors.toList());
        List<Asset> actualAll = toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(expectedAll, actualAll);
    }

    @Tag("skip-fts-init")
    @Test
    void testGetAssetCount() {
        long count = table.getCount();
        assertEquals(8, count);
    }

    @Tag("skip-fts-init")
    @Test
    void getAssetsIssuedBy() {
        List<Asset> expected = toList(table.getManyBy(
            new DbClause.LongClause("account_id", td.ASSET_1.getAccountId()), 0, 1));
        assertEquals(2, expected.size());
    }

    @Tag("skip-fts-init")
    @Test
    void testRollback() {
        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(td.ASSET_3.getHeight()));
        verify(fullTextOperationDataEvent, times(4)).select(new AnnotationLiteral<TrimEvent>() {});
    }

}