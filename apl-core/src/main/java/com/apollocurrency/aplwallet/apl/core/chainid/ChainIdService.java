package com.apollocurrency.aplwallet.apl.core.chainid;

import java.io.IOException;

public interface ChainIdService {
    Chain getActiveChain() throws IOException;
}
