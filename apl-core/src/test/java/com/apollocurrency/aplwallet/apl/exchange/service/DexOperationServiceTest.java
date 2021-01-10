package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexOperation;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOperationDao;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DexOperationServiceTest {

    @Mock
    private DexOperationDao dexOperationDao;
    @Mock
    private TaskDispatchManager dispatchManager;

    private DexOperation testOp;

    private DexOperationService dexOperationService;

    @BeforeEach
    void setUp() {
        dexOperationService = new DexOperationService(7776000, false, dexOperationDao, dispatchManager);

        testOp = DexOperation.builder()
            .account("testAccount")
            .stage(DexOperation.Stage.APL_CONTRACT_S1)
            .eid("testEID")
            .build();
    }

    @Test
    void saveIfExist() {
        doReturn(testOp).when(dexOperationDao).getBy(testOp.getAccount(), testOp.getStage(), testOp.getEid());
        doReturn(1).when(dexOperationDao).updateByDbId(testOp);

        dexOperationService.save(testOp);

        verify(dexOperationDao).updateByDbId(testOp);
    }

    @Test
    void saveIfNotExist() {
        doReturn(null).when(dexOperationDao).getBy(testOp.getAccount(), testOp.getStage(), testOp.getEid());

        dexOperationService.save(testOp);

        verify(dexOperationDao).add(testOp);
    }
}