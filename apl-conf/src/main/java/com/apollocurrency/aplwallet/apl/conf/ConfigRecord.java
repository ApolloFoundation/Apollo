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
public class ConfigRecord {
    public String name;
    @Default
    public String defaultValue="";
    public String description;
    @Default
    public Version sinceRelease = new Version("1.0.0");
    public Version deprecatedSince;
    @Default
    public String cmdLineOpt="";
    @Default
    public String envVar="";
    @Default
    public boolean isRequired = false;
}
