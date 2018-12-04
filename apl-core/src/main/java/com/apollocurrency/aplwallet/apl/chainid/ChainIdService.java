package com.apollocurrency.aplwallet.apl.chainid;

import java.io.IOException;

public interface ChainIdService {
    Chain getActiveChain() throws IOException;
}
