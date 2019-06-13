/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;


import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileChunkState;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileInfo;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Downloadable files info
 *
 * @author alukin@gmail.com
 */
//TODO: cache purging
@Singleton
public class DownloadableFilesManager {

    private static final Logger log =LoggerFactory.getLogger(DownloadableFilesManager.class);
    public final static long FDI_TTL = 7 * 24 * 3600 * 1000; //7 days in ms
    public final static int FILE_CHUNK_SIZE = 32768; //32K because 64K is maximum for WebSocket
    public final static String FILES_SUBDIR = "downloadables";
    private final Map<String, FileDownloadInfo> fdiCache = new HashMap<>();
    public static final Map<String, Integer> LOCATION_KEYS = Map.of("shard", 0, "attachment", 1, "file", 2, "debug", 3);
    public static final String MOD_CHAINID="chainid";
    public static final Map<String, Integer> LOCATION_MODIFIERS = Map.of(MOD_CHAINID, 0);

    
    private final ShardNameHelper shardNameHelper;
    private final DirProvider dirProvider;
    private final ChainsConfigHolder chainsConfig;
    
    private class ParsedFileId {
        Integer key = -1;
        String fileId;
        Map<String, String> modifiers = new HashMap<>();
    }

    @Inject
    public DownloadableFilesManager(DirProvider dirProvider, ShardNameHelper shardNameHelper, ChainsConfigHolder chainsConfig) {
        Objects.requireNonNull(dirProvider, "dirProvider is NULL");
        Objects.requireNonNull(dirProvider.getDataExportDir(), "dataExportDir in dirProvider is NULL");
        this.dirProvider=dirProvider;
        this.shardNameHelper = shardNameHelper;
        this.chainsConfig = chainsConfig;
    }

    public FileInfo getFileInfo(String fileId) {
        Objects.requireNonNull(fileId, "fileId is NULL");
        FileInfo fi;
        FileDownloadInfo fdi = fdiCache.get(fileId);
        if (fdi == null) {
            fdi = createFileDownloadInfo(fileId);
        }
        fi = fdi.fileInfo;
        return fi;
    }

    public FileDownloadInfo getFileDownloadInfo(String fileId) {
        Objects.requireNonNull(fileId, "fileId is NULL");
        FileDownloadInfo fdi = fdiCache.get(fileId);
        if (fdi == null) {
            FileInfo fi = getFileInfo(fileId);
        }
        fdi = fdiCache.get(fileId);
        return fdi;
    }

    private FileDownloadInfo createFileDownloadInfo(String fileId) {
        Objects.requireNonNull(fileId, "fileId is NULL");
        FileDownloadInfo downloadInfo = new FileDownloadInfo();
        Path fpath = mapFileIdToLocalPath(fileId);
        if (fpath != null) {
            downloadInfo.fileInfo.isPresent = true;
            downloadInfo.fileInfo.fileId = fileId;
            downloadInfo.created = Instant.now(); // in UTC
            ChunkedFileOps fops = new ChunkedFileOps(fpath);
            downloadInfo.fileInfo.size = fops.getFileSize();
            if (downloadInfo.fileInfo.size < 0) {
                downloadInfo.fileInfo.isPresent = false;
            } else {
                downloadInfo.fileInfo.fileDate = fops.getFileDate();
                downloadInfo.fileInfo.hash = Convert.toHexString(fops.getFileHashSums(FILE_CHUNK_SIZE));
                downloadInfo.fileInfo.chunkSize = FILE_CHUNK_SIZE;
                downloadInfo.fileInfo.originHostSignature = "";
                List<ChunkedFileOps.ChunkInfo> crcs = fops.getChunksCRC();
                for (int i = 0; i < crcs.size(); i++) {
                    FileChunkInfo fci = new FileChunkInfo();
                    ChunkedFileOps.ChunkInfo ci = crcs.get(i);
                    fci.crc = ci.crc;
                    fci.fileId = fileId;
                    fci.offset = ci.offset;
                    fci.present = FileChunkState.PRESENT;
                    fci.size = ci.size;
                    fci.chunkId = i;
                    downloadInfo.chunks.add(fci);
                }
                fdiCache.put(fileId, downloadInfo);
            }
        } else {
            downloadInfo.fileInfo.fileId = fileId;
            downloadInfo.fileInfo.isPresent = false;
        }
        return downloadInfo;
    }

    private ParsedFileId parseFileId(String fileId) {
        ParsedFileId res = new ParsedFileId();
        String[] all = fileId.split(";");
        for (String kv : all) {
            String[] kva = kv.split("::");
            Integer key = LOCATION_KEYS.get(kva[0]);
            if (key != null) {
                res.key = key;
                if (kva.length != 2) {
                    log.warn("Error parsing download file string. Location key: {} must have value.", kva[0]);
                } else {
                    res.fileId = kva[1];
                }
            } else {
                Integer mod = LOCATION_MODIFIERS.get(kva[0]);
                if (mod != null) {
                    if (kva.length != 2) {
                        res.modifiers.put(kva[0], "");
                    } else {
                        res.modifiers.put(kva[0], kva[1]);
                    }
                }
            }
        }
        return res;
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

        ParsedFileId parsed = parseFileId(fileId);

        String absPath = "";
        switch (parsed.key) {
            case 0: //shard
            {
                long shardId = 0;
                try {
                    shardId = Long.valueOf(parsed.fileId);
                    UUID chainId;
                    if(parsed.modifiers.isEmpty()){
                      chainId=chainsConfig.getActiveChain().getChainId();
                    }else{
                       String chainIdStr = parsed.modifiers.get(MOD_CHAINID);
                       chainId=UUID.fromString(chainIdStr);
                    }
                    
                    String fileName = shardNameHelper.getShardArchiveNameByShardId(shardId,chainId);
                    String fileBaseDir = dirProvider.getDataExportDir().toString();
                    absPath = fileBaseDir + File.separator + fileName;
                } catch (NumberFormatException e) {
                    log.warn("Incorrect shardId value found in parameter = '{}'", fileId);
                }
            };
            break;
            case 1: //attachment
            {
                 log.warn("Attachmednt  downloading is not implemented yet");
            };
            break;
            case 2: //file
            {
                 log.warn("File  downloading is not implemented yet");
            };
            break;
            case 3: //debug and tests
            {
                String fileBaseDir =System.getProperty("java.io.tmpdir");
                absPath = fileBaseDir + File.separator + parsed.fileId;
            };
            break;
            default:{
                
            };
        }
        if(StringUtils.isBlank(absPath)){
            return null;
        }
        Path res = Paths.get(absPath);
        return res;
    }
}
