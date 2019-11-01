/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple class to indicate which file is downloaded
 * @author al
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileEventData {
    public String fileId;
    public Boolean fileOk;
    public String reason;
}
