package com.apollocurrency.aplwallet.apl.core.chainid;

import java.io.IOException;
import java.util.List;

public interface ChainIdService {
    Chain getActiveChain() throws IOException;

    List<Chain> getAll() throws IOException;
}
