package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.nio.file.Path;

public class ShardExportDirProducer {

    public ShardExportDirProducer() {
    }

    @Produces
    @Named("dataExportDir")
    public Path getDataExportDir() {
        return RuntimeEnvironment.getInstance().getDirProvider().getDataExportDir();
    }
}
