/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author alukin@gmail.com
 */
public class FileDownloadInfo {
    public FileInfo fileInfo=new FileInfo();    
    public List<FileChunkInfo> chunks = new ArrayList<>();
    @JsonIgnore
    /** record creation date, needed by cache */
    public Date created;
}
