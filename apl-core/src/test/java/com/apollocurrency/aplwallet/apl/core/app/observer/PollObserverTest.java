package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollObserverTest {

    @Mock
    PollService pollService;
    private PollObserver observer;

    @BeforeEach
    void setUp() {
        this.observer = new PollObserver(pollService);
    }

    @Test
    void onBlockApplied() {
        Block block = mock(Block.class);
        when(block.getHeight()).thenReturn(1000).thenReturn(1000).thenReturn(1000);
        doNothing().when(pollService).checkPolls(1000);
//        when(pollService.checkPolls(block.getHeight());

        this.observer.onBlockApplied(block);

        verify(pollService).checkPolls(block.getHeight());
    }
}