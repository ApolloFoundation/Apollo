/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

/**
 *
 * @author alukin@gmail.com
 */
public class FileDownloadInfoResponse extends BaseP2PResonse{
    public FileDownloadInfo downloadInfo=new FileDownloadInfo(); 
    public FileInfo fileInfo = new FileInfo();
}
