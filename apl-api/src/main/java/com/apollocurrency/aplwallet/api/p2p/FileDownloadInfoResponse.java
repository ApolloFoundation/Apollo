/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author alukin@gmail.com
 */
@Getter @Setter
public class FileDownloadInfoResponse extends BaseP2PResonse{
    public FileDownloadInfo downloadInfo=new FileDownloadInfo(); 
}
