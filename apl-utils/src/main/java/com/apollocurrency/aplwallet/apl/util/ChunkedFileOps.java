/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Read/Write file by chunks
 * @author alukin@gmail.com
 */
public class ChunkedFileOps {
    private String absPath;
    private Long lastRDChunkCrc;
    private Long lastWRChunkCrc;
    
    public ChunkedFileOps(String absPath) {
        this.absPath = absPath;
    }
    
    public int writeChunk(int offset, byte[] data, long crc) throws IOException{
        int res=0;
        CheckSum cs = new CheckSum();
        cs.update(data);
        lastWRChunkCrc=cs.finish();
        if(lastWRChunkCrc!=crc){
            throw new BadCheckSumException(absPath);
        }
        Path path = Paths.get(absPath);
        if(!path.toFile().exists()){
            Files.createFile(path);
        }
        RandomAccessFile rf = new RandomAccessFile(absPath,"rw");
        rf.skipBytes(offset);
        rf.write(data);
        rf.close();
        return res;
    }
    
    public int readChunk(int offset, int size, byte[] dataBuf) throws IOException{
        int res=0;
        Path path = Paths.get(absPath);
        if(!path.toFile().exists()){
           res=-2;
           return res;
        }        
        RandomAccessFile rf = new RandomAccessFile(absPath,"rw");
        rf.skipBytes(offset);
        res = rf.read(dataBuf,0,size);
        CheckSum cs = new CheckSum();
        cs.update(dataBuf,size);
        lastRDChunkCrc=cs.finish();
        return res;
    }

    public long getLastWRChunkCrc() {
        return lastWRChunkCrc;
    }
    public long getLastRDChunkCrc() {
        return lastRDChunkCrc;
    }    
}
