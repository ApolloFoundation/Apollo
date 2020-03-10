package com.apollocurrency.aplwallet.apl.core.transaction.messages.update;

import com.apollocurrency.aplwallet.apl.util.Architecture;
import com.apollocurrency.aplwallet.apl.util.Platform;
import lombok.Data;

@Data
public class PlatformSpec {
    final Platform platform;
    final Architecture architecture;


}


