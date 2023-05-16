package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.dex.core.dao.UserErrorMessageDao;
import com.apollocurrency.aplwallet.apl.dex.core.model.UserErrorMessage;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
@Slf4j
public class UserErrorMessageServiceImpl implements UserErrorMessageService {
    private static final int DELAY = 30 * 60 * 1000;// 30 min in ms
    private static final int DEFAULT_DELETE_OFFSET = 90 * 24 * 60 * 60;// 90 days in  seconds
    private UserErrorMessageDao dao;
    private TaskDispatchManager taskDispatchManager;
    private PropertiesHolder propertiesHolder;

    @Inject
    public UserErrorMessageServiceImpl(UserErrorMessageDao dao, TaskDispatchManager taskDispatchManager, PropertiesHolder propertiesHolder) {
        this.dao = dao;
        this.taskDispatchManager = taskDispatchManager;
        this.propertiesHolder = propertiesHolder;
    }

    @PostConstruct
    public void init() {
        long offset = propertiesHolder.getIntProperty("apl.userErrorMessage.lifetime", DEFAULT_DELETE_OFFSET) * 1000;
        if (offset > 0) {
            taskDispatchManager.newScheduledDispatcher("UserErrorMessageService")
                .schedule(Task.builder()
                    .name("Expired errors removing")
                    .delay(DELAY)
                    .task(() -> deleteByTimestamp(System.currentTimeMillis() - offset))
                    .build());
        } else {
            log.warn("Will not delete expired user error messages");
        }
    }

    @Override
    public List<UserErrorMessage> getAllByAddress(String address, long toDbId, int limit) {
        return dao.getAllByAddress(address, toDbId, limit);
    }

    @Override
    public void add(UserErrorMessage errorMessage) {
        dao.add(errorMessage);
    }

    @Override
    public List<UserErrorMessage> getAll(long toDbId, int limit) {
        return dao.getAll(toDbId, limit);
    }

    @Override
    public int deleteByTimestamp(long timestamp) {
        int deleted = dao.deleteByTimestamp(timestamp);
        log.info("Deleted {} error messages", deleted);
        return deleted;
    }
}
