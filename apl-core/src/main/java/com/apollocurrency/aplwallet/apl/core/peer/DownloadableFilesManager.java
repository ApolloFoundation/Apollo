/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Downloadable files info
 * @author alukin@gmail.com
 */

@Singleton
public class DownloadableFilesManager {
    
    public final static long FDI_TTL=24*3600*1000; //one day in ms
    public final static int FILE_CHUNK_SIZE=32768;
    private final Map<String,FileDownloadInfo> fdiCache = new HashMap<>();
    private String fileBaseDir="/home/at/testfiles";
    
    public FileInfo getFileInfo(String fileId){
        FileInfo fi;
        FileDownloadInfo fdi = fdiCache.get(fileId);
        if(fdi==null){
            fdi = createFileDownloadInfo(fileId);
        }        
        fi = fdi.fileInfo;
        return fi;
    }
    
    public FileDownloadInfo getFileDownloadInfo(String fileId){
        FileDownloadInfo fdi = fdiCache.get(fileId);
        if(fdi==null){
            FileInfo fi = getFileInfo(fileId);
        }
        fdi = fdiCache.get(fileId);
        return fdi;
    }

    private FileDownloadInfo createFileDownloadInfo(String fileId) {
        FileDownloadInfo res = new FileDownloadInfo();
        Path fpath=mapFileIdToLocalPath(fileId);
        if(fpath!=null){
            res.fileInfo.isPresent=true;
            res.fileInfo.fileId=fileId;
            res.created=new Date();
            ChunkedFileOps fops = new ChunkedFileOps(fpath);
            res.fileInfo.size=fops.getFileSize();
            if(res.fileInfo.size<0){
               res.fileInfo.isPresent=false;                
            }else{
                res.fileInfo.fileDate=fops.getFileDate();
                res.fileInfo.hash=Convert.toHexString(fops.getFileHashSums(FILE_CHUNK_SIZE));
                res.fileInfo.chunkSize=FILE_CHUNK_SIZE;
                res.fileInfo.originHostSignature="";
                List<ChunkedFileOps.ChunkInfo> crcs = fops.getChunksCRC();
                for(int i=0; i<crcs.size() ;i++){
                    FileChunkInfo fci = new FileChunkInfo();
                    ChunkedFileOps.ChunkInfo ci=crcs.get(i);
                    fci.crc=ci.crc;
                    fci.fileId=fileId;
                    fci.offset=ci.offset;
                    fci.present=true;
                    fci.size=ci.size;
                    fci.chunkId=i;
                    res.chunks.add(fci);
                }
                fdiCache.put(fileId, res);
            }
        }else{
            res.fileInfo.fileId=fileId;
            res.fileInfo.isPresent=false;
        }
        return res;
    }
    
    public Path mapFileIdToLocalPath(String fileId){
       String abspath = fileBaseDir+File.separator+fileId;
       Path res = Paths.get(abspath);
       if(!Files.exists(res)){
          res=null;
       }
       return res;
    }
}
