/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.TemporaryFolderExtension;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

@EnableWeld
public class ShardingHashCalculatorTest {

//    @WeldSetup
//    WeldInitiator weldInitiator = WeldInitiator.from(BlockchainImpl.class)
    @RegisterExtension
    TemporaryFolderExtension temporaryFolder = new TemporaryFolderExtension();
    @Test
    public void testCalculateHash() throws IOException {
        temporaryFolder.newFile("Filename");
    }
}
