/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read/Write file by chunks
 * @author alukin@gmail.com
 */

public class ChunkedFileOps {
    private final Path absPath;
    private Long lastRDChunkCrc;
    private Long lastWRChunkCrc;
    public static final String DIGESTER="SHA-256";
    private static final Logger log = LoggerFactory.getLogger(ChunkedFileOps.class);
    public ChunkedFileOps(String absPath) {
        this.absPath = Paths.get(absPath);        
    }
    public ChunkedFileOps(Path path) {
        this.absPath = path;        
    }
    
    public class ChunkInfo{
        public Long offset;
        public Long size;
        public Long crc;
    }
    private final List<ChunkInfo> fileCRCs = new ArrayList<>();
    
    public synchronized int writeChunk(Long offset, byte[] data, long crc) throws IOException{
        int res=0;
        CheckSum cs = new CheckSum();
        cs.update(data);
        lastWRChunkCrc=cs.finish();
        if(lastWRChunkCrc!=crc){
            throw new BadCheckSumException(absPath.toString());
        }
        if(!absPath.getParent().toFile().exists()){
            absPath.getParent().toFile().mkdirs();
        }
        RandomAccessFile rf=null;
        try  {
            rf = new RandomAccessFile(absPath.toFile(),"rw");  
            rf.seek(offset);
            rf.write(data);
        }catch( IOException e){
           log.error("Can not write file: {}",absPath.toAbsolutePath().toString());
        }finally{
            if(rf!=null){
              rf.close();
            }
        }
        return res;
    }
    
    public int readChunk(int offset, int size, byte[] dataBuf) throws IOException{
        int res;
        if(!absPath.toFile().exists()){
           res=-2;
           return res;
        }        
        RandomAccessFile rf = new RandomAccessFile(absPath.toFile(),"r");
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
    
    public long getFileSize(){
        long res = -1L;
        try {  
            BasicFileAttributes attrs = Files.readAttributes(absPath, BasicFileAttributes.class);
            res = attrs.size();
        } catch (IOException ex) {            
        }
      return res;   
    }   
    
    /**
     * Calculates file hash and partial CRCss
     * Should depends on crypto settings, but at the time it is SHA-256
     * @param chunkSize size of file chunks to calculate partial CRCs
     * @return Crypto hash sum of entire file
     */
    public byte[] getFileHashSums(int chunkSize){
        byte[] hash = null;
        byte[] buf = new byte[chunkSize];
        fileCRCs.clear();
        try (RandomAccessFile rf = new RandomAccessFile(absPath.toFile(),"r")) {
            //TODO: use FBCryptoDigest after FBCrypto update for stream operations
            MessageDigest dgst = MessageDigest.getInstance(DIGESTER);
            Integer rd;
            long offset=0;
            while((rd=rf.read(buf))>0){
                dgst.update(buf,0,rd);
                CheckSum cs = new CheckSum();
                cs.update(buf,rd);
                ChunkInfo ci = new ChunkInfo();
                ci.offset=offset;
                ci.size=rd.longValue();
                ci.crc=cs.finish();
                fileCRCs.add(ci);
                offset=offset+rd;
            }
            hash=dgst.digest();
        } catch (IOException | NoSuchAlgorithmException ex) {
        }
       return hash;    
    }
    
    public List<ChunkInfo> getChunksCRC(){
        return fileCRCs;
    }

    public Date getFileDate() {
        Long res=1L;
        try {  
            BasicFileAttributes attrs = Files.readAttributes(absPath, BasicFileAttributes.class);
            res = attrs.lastModifiedTime().toMillis();
        } catch (IOException ex) {            
        }
       return new Date(res);       
    }
}
