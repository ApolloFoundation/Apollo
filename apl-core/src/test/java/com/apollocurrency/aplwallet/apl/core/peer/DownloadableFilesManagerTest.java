package com.apollocurrency.aplwallet.apl.core.peer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.testutil.ResourceFileLoader;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import java.io.File;
import java.util.UUID;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;

@EnableWeld
class DownloadableFilesManagerTest {
    private static final Logger log = getLogger(DownloadableFilesManagerTest.class);
    private Path csvResourcesPath = new ResourceFileLoader().getResourcePath().toAbsolutePath();
    private DirProvider dirProvider = mock(DirProvider.class);
    private ChainsConfigHolder chainCoinfig;
    private Chain chain;
    private final UUID chainId=UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5");    
    {
        doReturn(csvResourcesPath).when(dirProvider).getDataExportDir();
        chain = mock(Chain.class);
        when(chain.getChainId()).thenReturn(chainId);
        chainCoinfig = mock(ChainsConfigHolder.class);
        when(chainCoinfig.getActiveChain()).thenReturn(chain);        
    }

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
               ShardNameHelper.class, 
               DownloadableFilesManager.class)
            .addBeans(MockBean.of(dirProvider, DirProvider.class))
            .addBeans(MockBean.of(chainCoinfig, ChainsConfigHolder.class))
            .build();

    String zipFileName = "apl-blockchain-arch-1.zip";
    @Inject
    private ShardNameHelper shardNAmeHelper;
    
    @Inject
    private DownloadableFilesManager filesManager;
    
    private void createTestZip(){
        String fileBaseDir =System.getProperty("java.io.tmpdir");
        File tmpDir = new File(fileBaseDir);
        File wDir = new File(tmpDir.getAbsolutePath()+"/"+"apl-test-zip");
        if(wDir.exists()){
            wDir.delete();
        }
        wDir.mkdir();
        Zip zip = new ZipImpl();
        zip.compress(zipFileName, fileBaseDir, Long.MIN_VALUE, null,false);
    }
    
    @Test
    void getFileDownloadInfo() {
        // create ZIP in temp folder for unit test


        FileDownloadInfo fi = filesManager.getFileDownloadInfo("debug::"+zipFileName);
        assertNotNull(fi);
        log.debug("File download Info = {}", fi);
        assertEquals(
                "c8d3a0c3d323366c711e4d05d5706db27da385a0181ae7584013b641c2b97221",
                fi.fileInfo.hash);
        log.debug("Parsed bytes from string = {}", Convert.parseHexString(fi.fileInfo.hash) );
        assertEquals(zipFileName, fi.fileInfo.fileId);
    }

    @Test
    void getMissingResource() {
        String zipFileName = "MISSING-archive.zip";

        FileDownloadInfo fi = filesManager.getFileDownloadInfo(zipFileName);
        assertNull(fi);
    }

    @Test
    void mapFileIdToLocalPath() {
        // parse real/existing shard name + ID
        Path pathToShardArchive = filesManager.mapFileIdToLocalPath("shard::1");
        assertNotNull(pathToShardArchive);
        assertEquals(zipFileName, pathToShardArchive.getFileName().toString());
        
        pathToShardArchive = filesManager.mapFileIdToLocalPath("shard::1;chainid::b5d7b697-f359-4ce5-a619-fa34b6fb01a5");
        assertEquals(zipFileName, pathToShardArchive.getFileName().toString());
        
//        String fpath = filesManager.mapFileIdToLocalPath("attachment::123;chainid::3ef0").toString();
//        assertEquals("123", fpath);
        
//        fpath = filesManager.mapFileIdToLocalPath("debug::123").toString();
//        assertEquals("123", fpath);
        
        // parse simple file name
//        fpath = filesManager.mapFileIdToLocalPath("file::phasing_poll.csv").toString();
//        assertEquals("123", fpath);;
        
//        assertEquals("phasing_poll.csv", pathToShardArchive.getFileName().toString());

        pathToShardArchive = filesManager.mapFileIdToLocalPath("shard::");
        assertNull(pathToShardArchive);

        assertThrows(NullPointerException.class, () -> filesManager.mapFileIdToLocalPath(null));

        pathToShardArchive = filesManager.mapFileIdToLocalPath("");
        assertNull(pathToShardArchive);
    }
}