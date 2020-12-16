package com.apollocurrency.aplwallet.apl.eth.dao;

import com.apollocurrency.aplwallet.apl.exchange.mapper.UserErrorMessageMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.UserErrorMessage;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface UserErrorMessageDao {
    @Transactional(readOnly = true)
    @RegisterRowMapper(UserErrorMessageMapper.class)
    @SqlQuery("SELECT * from user_error_message where address = :address and db_id < :dbId order by db_id desc LIMIT :limit")
    List<UserErrorMessage> getAllByAddress(@Bind("address") String address, @Bind("dbId") long toDbId, @Bind("limit") int limit);


    @Transactional
    @SqlUpdate("INSERT INTO user_error_message (address, error, operation, details, `timestamp`) VALUES (:address, :error, :operation, :details, :timestamp)")
    void add(@BindBean UserErrorMessage errorMessage);

    @Transactional(readOnly = true)
    @RegisterRowMapper(UserErrorMessageMapper.class)
    @SqlQuery("SELECT * from user_error_message where db_id < :dbId order by db_id desc LIMIT :limit")
    List<UserErrorMessage> getAll(@Bind("dbId") long toDbId, @Bind("limit") int limit);

    @Transactional
    @SqlUpdate("DELETE FROM user_error_message WHERE `timestamp` < :timestamp")
    int deleteByTimestamp(@Bind("timestamp") long timestamp);
}
