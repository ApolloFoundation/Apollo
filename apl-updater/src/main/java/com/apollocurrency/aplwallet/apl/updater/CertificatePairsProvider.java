package com.apollocurrency.aplwallet.apl.updater;

import java.util.Set;

public interface CertificatePairsProvider {
    Set<UpdaterUtil.CertificatePair> getPairs();
}
