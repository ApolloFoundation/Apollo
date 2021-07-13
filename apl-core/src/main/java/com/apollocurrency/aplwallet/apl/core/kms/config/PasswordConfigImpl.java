package com.apollocurrency.aplwallet.apl.core.kms.config;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import io.firstbridge.kms.security.PasswordConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Singleton
@NoArgsConstructor
public class PasswordConfigImpl implements PasswordConfig {

    private String HMacName;
    private String secretKey;
    private int cost;

    public PasswordConfigImpl(
        @Property(name = "kms.main.password.config.hmac.name") String HMacName,
        @Property(name = "kms.main.password.config.secret.key") String secretKey,
        @Property(name = "kms.main.password.config.cost", defaultValue = "-1") int cost) {
        this.HMacName = HMacName;
        this.secretKey = secretKey;
        this.cost = cost;
    }

    @Override
    public String getHMacName() {
        return this.HMacName;
    }

    @Override
    public String secretKey() {
        return this.secretKey;
    }

    @Override
    public int cost() {
        return this.cost;
    }
}
