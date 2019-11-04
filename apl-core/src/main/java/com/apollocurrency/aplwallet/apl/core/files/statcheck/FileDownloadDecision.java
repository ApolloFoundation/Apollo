/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.statcheck;

/**
 *
 * @author al
 */
public enum FileDownloadDecision {
    AbsOK, //network is 100% consistent
    OK, //network is OK but contains some small number of bad hosts
    Risky, // network contains significant number of bad hosts but still usable
    NeedsInvestigation, //network contains critical number of bad host and may be unusable
    Bad, // network is unusable
    NoPeers, // Just no peers with requested file
    NotReady; // Decision is not ready, not started or in progress
}
