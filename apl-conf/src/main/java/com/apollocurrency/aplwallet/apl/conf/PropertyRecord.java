/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.conf;

import com.apollocurrency.aplwallet.apl.util.Version;
import lombok.Builder;
import lombok.Builder.Default;

/**
 *
 * @author Oleksiy Lukin alukin@gmail.com
 */
@Builder
public class PropertyRecord {
    public String name;
    public String defaultValue;
    public String description;
    public Version sinceRelease;
    public Version deprecatedSince;
    @Default
    public boolean isRequired = false;
}
