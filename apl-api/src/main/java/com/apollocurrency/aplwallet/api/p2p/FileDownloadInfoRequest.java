/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

/**
 *
 * @author alukin@gmail.com
 */
public class FileDownloadInfoRequest  extends BaseP2PRequest{
    public String fileId;
    public boolean full;
}
