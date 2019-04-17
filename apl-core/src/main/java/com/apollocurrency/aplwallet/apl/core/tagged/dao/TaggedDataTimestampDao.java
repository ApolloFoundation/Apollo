/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.dao;

import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataTimestamp;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * Jdbi DAO for TaggedDataTimestamp
 */
public interface TaggedDataTimestampDao extends SqlObject {

    @SqlQuery("SELECT " +
            "   id, " +
            "   timestamp " +
            "FROM tagged_data_timestamp " +
            "WHERE id = :id")
    @RegisterBeanMapper(TaggedDataTimestamp.class)
    TaggedDataTimestamp get(@Bind("id") long id);

    @SqlUpdate("MERGE INTO tagged_data_timestamp (id, timestamp, height, latest) KEY (id, height) " +
            "VALUES (:id, :timestamp, :height, TRUE)")
    void save(@BindBean TaggedDataTimestamp taggedDataTimestamp, @Bind("height") int height);

    @SqlUpdate("UPDATE tagged_data_timestamp " +
            "SET latest = FALSE " +
            "WHERE id = :id AND latest = TRUE LIMIT 1")
    void updateLatestFalse(@Bind("id") long id);

}
