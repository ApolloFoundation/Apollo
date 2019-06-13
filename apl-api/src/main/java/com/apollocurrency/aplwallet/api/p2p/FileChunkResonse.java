/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author alukin@gmail.com
 */
@Getter @Setter 
public class FileChunkResonse extends BaseP2PResonse{
    public FileChunk chunk;
}
