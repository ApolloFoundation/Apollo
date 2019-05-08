/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alukin@gmail.com
 */
public class FileDownloadInfo {
    public FileInfo fileInfo;
    public List<FileChunkInfo> chunks = new ArrayList<>();
}
