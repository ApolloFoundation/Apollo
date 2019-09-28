package com.apollocurrency.aplwallet.apl.core.app;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrimConfig {
    private boolean enableTrim;
    private boolean clearTrimQueue;
}
