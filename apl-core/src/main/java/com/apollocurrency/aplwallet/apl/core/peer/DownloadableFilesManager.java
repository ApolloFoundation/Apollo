/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import static org.slf4j.LoggerFactory.getLogger;

import static com.apollocurrency.aplwallet.api.p2p.FileChunkInfoPresent.SAVED;

import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileInfo;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Downloadable files info
 * @author alukin@gmail.com
 */

//TODO: cache purging
@Singleton
public class DownloadableFilesManager {
    private static final Logger log = getLogger(DownloadableFilesManager.class);

    public final static long FDI_TTL=7*24*3600*1000; //7 days in ms
    public final static int FILE_CHUNK_SIZE=32768; //32K because 64K is maximum for WebSocket
    public final static String FILES_SUBDIR="downloadables";
    private final Map<String,FileDownloadInfo> fdiCache = new HashMap<>();
    private String fileBaseDir;
    
    @Inject
    public DownloadableFilesManager(DirProvider dirProvider) {
        Objects.requireNonNull(dirProvider, "dirProvider is NULL");
        Objects.requireNonNull(dirProvider.getDataExportDir(), "dataExportDir in dirProvider is NULL");
        this.fileBaseDir = dirProvider.getDataExportDir().toString();
        log.debug("Node's dataExportDir = {}", this.fileBaseDir);
    }
    
    public FileInfo getFileInfo(String fileId){
        Objects.requireNonNull(fileId, "fileId is NULL");
        FileInfo fi;
        FileDownloadInfo fdi = fdiCache.get(fileId);
        if(fdi == null){
            fdi = createFileDownloadInfo(fileId);
        }        
        fi = fdi.fileInfo;
        return fi;
    }
    
    public FileDownloadInfo getFileDownloadInfo(String fileId) {
        Objects.requireNonNull(fileId, "fileId is NULL");
        FileDownloadInfo fdi = fdiCache.get(fileId);
        if(fdi == null){
            FileInfo fi = getFileInfo(fileId);
        }
        fdi = fdiCache.get(fileId);
        return fdi;
    }

    private FileDownloadInfo createFileDownloadInfo(String fileId) {
        Objects.requireNonNull(fileId, "fileId is NULL");
        FileDownloadInfo downloadInfo = new FileDownloadInfo();
        Path fpath = mapFileIdToLocalPath(fileId);
        if(fpath != null){
            downloadInfo.fileInfo.isPresent = true;
            downloadInfo.fileInfo.fileId = fileId;
            downloadInfo.created = Instant.now(); // in UTC
            ChunkedFileOps fops = new ChunkedFileOps(fpath);
            downloadInfo.fileInfo.size=fops.getFileSize();
            if (downloadInfo.fileInfo.size<0) {
               downloadInfo.fileInfo.isPresent=false;
            } else {
                downloadInfo.fileInfo.fileDate=fops.getFileDate();
                downloadInfo.fileInfo.hash=Convert.toHexString(fops.getFileHashSums(FILE_CHUNK_SIZE));
                downloadInfo.fileInfo.chunkSize=FILE_CHUNK_SIZE;
                downloadInfo.fileInfo.originHostSignature="";
                List<ChunkedFileOps.ChunkInfo> crcs = fops.getChunksCRC();
                for(int i=0; i<crcs.size() ;i++){
                    FileChunkInfo fci = new FileChunkInfo();
                    ChunkedFileOps.ChunkInfo ci=crcs.get(i);
                    fci.crc=ci.crc;
                    fci.fileId=fileId;
                    fci.offset=ci.offset;
                    fci.present = SAVED;
                    fci.size=ci.size;
                    fci.chunkId=i;
                    downloadInfo.chunks.add(fci);
                }
                fdiCache.put(fileId, downloadInfo);
            }
        } else {
            downloadInfo.fileInfo.fileId=fileId;
            downloadInfo.fileInfo.isPresent=false;
        }
        return downloadInfo;
    }

    /**
     * Find the real ZIP file in folder by specified fileId.
     *
     * @param fileId example = shard::123
     * @return real path
     */
    public Path mapFileIdToLocalPath(String fileId) {
        Objects.requireNonNull(fileId, "fileId is NULL");
        if (fileId.isEmpty()) {
            log.error("fileId is '{}' empty", fileId);
            return null;
        }

        String absPath;
        if (fileId.contains("shard::")) {
            String realShardId = fileId.substring(fileId.lastIndexOf("::") + 2);
            long shardId = 0;
            try {
                shardId = Long.valueOf(realShardId);
            } catch (NumberFormatException e) {
                log.warn("Incorrect shardId value found in parameter = '{}'", fileId);
                return null;
            }
            // that will be only shard archive file present in folder
            String fileName = ShardNameHelper.getShardArchiveNameByShardId(shardId);
            absPath = this.fileBaseDir + File.separator + fileName + ".zip";
        } else {
            // that will be any file present in folder
            absPath = this.fileBaseDir + File.separator + fileId;
        }
        Path res = Paths.get(absPath);
        if(!Files.exists(res)){
            res = null;
        }
        return res;
    }
}
