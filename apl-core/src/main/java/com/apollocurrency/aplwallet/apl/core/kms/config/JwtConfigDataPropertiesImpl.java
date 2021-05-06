package com.apollocurrency.aplwallet.apl.core.kms.config;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import io.firstbridge.kms.security.JwtConfigData;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Properties to be loaded from 'apl-blockchain.properties' file
 */
@Singleton
@Setter
@Getter
@ToString
public class JwtConfigDataPropertiesImpl implements JwtConfigData {

    @Inject
    public JwtConfigDataPropertiesImpl(
        @Property(name = "apl.jwt.issuer", defaultValue = "") String issuer,
        @Property(name = "apl.jwt.secret", defaultValue = "") String secret,
        @Property(name = "apl.jwt.access.expiration.time", defaultValue = "") String accessTokenExpirationTime,
        @Property(name = "apl.jwt.admin.access.expiration.time", defaultValue = "") String accessAdminTokenExpirationTime,
        @Property(name = "apl.jwt.refresh.expiration.time", defaultValue = "") String refreshTokenExpirationTime
    ) {
        this.issuer = issuer;
        this.secret = secret;
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.accessAdminTokenExpirationTime = accessAdminTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    private String issuer;
    private String secret;
    private String accessTokenExpirationTime;
    private String accessAdminTokenExpirationTime;
    private String refreshTokenExpirationTime;

}
