package com.apollocurrency.aplwallet.apl.core.kms.config;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import io.firstbridge.kms.security.JwtConfigData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Properties to be loaded from 'apl-blockchain.properties' file
 */
@Setter
@Getter
@ToString
@Singleton
@NoArgsConstructor
public class JwtConfigDataPropertiesImpl implements JwtConfigData {

    private String issuer;
    private String privateKeyFileName;
    private String publicKeyFileName;
    private String accessTokenExpirationTime;
    private String accessAdminTokenExpirationTime;
    private String refreshTokenExpirationTime;

    @Inject // mandatory annotation
    public JwtConfigDataPropertiesImpl (
        @Property(name = "kms.main.jwt.issuer", defaultValue = "") String issuer,
        @Property(name = "kms.main.jwt.private.key.file.name") String privateKeyFileName,
        @Property(name = "kms.main.jwt.public.key.file.name") String publicKeyFileName,
        @Property(name = "kms.main.jwt.access.expiration.time", defaultValue = "") String accessTokenExpirationTime,
        @Property(name = "kms.main.jwt.admin.access.expiration.time", defaultValue = "") String accessAdminTokenExpirationTime,
        @Property(name = "kms.main.jwt.refresh.expiration.time", defaultValue = "") String refreshTokenExpirationTime
    ) {
        this.issuer = issuer;
        this.privateKeyFileName = privateKeyFileName;
        this.publicKeyFileName = publicKeyFileName;
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.accessAdminTokenExpirationTime = accessAdminTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

}
