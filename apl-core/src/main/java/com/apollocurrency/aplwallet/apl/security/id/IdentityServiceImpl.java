/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.security.id;



import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import io.firstbridge.identity.handler.IdValidator;
import io.firstbridge.identity.handler.IdValidatorImpl;
import io.firstbridge.identity.handler.ThisActorIdHandler;
import io.firstbridge.identity.handler.ThisActorIdHandlerImpl;
import java.io.File;
import javax.inject.Inject;

/**
 * Identity service implementation
 *
 * @author alukin@gmail.com
 */
public class IdentityServiceImpl implements IdentityService {

    private final ThisActorIdHandler thisNodeIdHandler;
    private final IdValidator peerIdValidator;
    private final ConfigDirProvider dirProvider;

    @Inject
    public IdentityServiceImpl(ConfigDirProvider dirProvider) {
        this.thisNodeIdHandler = new ThisActorIdHandlerImpl();
        this.peerIdValidator = new IdValidatorImpl();
        this.dirProvider = dirProvider;
    }

    @Override
    public ThisActorIdHandler getThisNodeIdHandler() {
        return thisNodeIdHandler;
    }

    @Override
    public IdValidator getPeerIdValidator() {
        return peerIdValidator;
    }

    @Override
    public void loadAll() {
        String confName = dirProvider.getConfigName();
        String[] confLocations = {dirProvider.getInstallationConfigLocation(),
            dirProvider.getSysConfigLocation(),
            dirProvider.getUserConfigLocation()};
        for (String loc : confLocations) {
            String path = loc + File.separator + confName;
            System.out.println("========= " + path);
        }
    }

}
