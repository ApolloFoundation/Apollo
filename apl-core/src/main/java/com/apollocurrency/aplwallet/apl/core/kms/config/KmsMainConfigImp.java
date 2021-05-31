package com.apollocurrency.aplwallet.apl.core.kms.config;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.firstbridge.kms.security.JwtConfigData;
import io.firstbridge.kms.security.KmsMainConfig;
import io.firstbridge.kms.security.RemoteKmsConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Singleton
public class KmsMainConfigImp implements KmsMainConfig {

    private JwtConfigData jwtConfigDataProperties;
    private RemoteKmsConfig remoteKmsConfig;

    @Inject
    public KmsMainConfigImp(
        JwtConfigDataPropertiesImpl jwtConfigDataProperties,
        RemoteKmsConfigImpl remoteKmsConfig
    ) {
        this.jwtConfigDataProperties = jwtConfigDataProperties;
        this.remoteKmsConfig = remoteKmsConfig;
    }

    @Override
    public JwtConfigData getJwtConfigData() {
        return this.jwtConfigDataProperties;
    }

    @Override
    public RemoteKmsConfig getRemoteKmsConfig() {
        return this.remoteKmsConfig;
    }
}
