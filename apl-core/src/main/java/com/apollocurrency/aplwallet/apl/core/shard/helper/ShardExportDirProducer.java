package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.nio.file.Path;

public class ShardExportDirProducer {

    private Path unitTestPath; // TODO: replace by mock?

    public ShardExportDirProducer() {
    }

    public ShardExportDirProducer(Path unitTestPath) {
        this.unitTestPath = unitTestPath;
    }

    @Produces
    @Named("dataExportDir")
    public Path getDataExportDir() {
        if (unitTestPath == null) {
            return RuntimeEnvironment.getInstance().getDirProvider().getDataExportDir();
        } else {
            return unitTestPath;
        }
    }
}
