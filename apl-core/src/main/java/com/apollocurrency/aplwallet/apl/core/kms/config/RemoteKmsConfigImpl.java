package com.apollocurrency.aplwallet.apl.core.kms.config;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import io.firstbridge.kms.infrastructure.web.resource.WorkMode;
import io.firstbridge.kms.security.RemoteKmsConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Singleton
@NoArgsConstructor
public class RemoteKmsConfigImpl  implements RemoteKmsConfig {

    private boolean remoteServerModeOn;
    private String address;
    private int grpcPort;
    private int httpPort;

    public RemoteKmsConfigImpl(
        @Property(name = "kms.main.remote.server.mode.on", defaultValue = "false") boolean remoteServerModeOn,
        @Property(name = "kms.main.remote.server.address", defaultValue = "") String address,
        @Property(name = "kms.main.remote.server.grpc.port", defaultValue = "-1") int grpcPort,
        @Property(name = "kms.main.remote.server.http.port", defaultValue = "-1") int httpPort
    ) {
        this.remoteServerModeOn = remoteServerModeOn;
        this.address = address;
        this.grpcPort = grpcPort;
        this.httpPort = httpPort;
    }

    @Override
    public String getRemoteAddress() {
        return this.address;
    }

    public void setRemoteServerModeOn(boolean remoteServerModeOn) {
        this.remoteServerModeOn = remoteServerModeOn;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public WorkMode getRemoteServerModeOn() {
        return remoteServerModeOn ? WorkMode.REMOTE_SERVER : WorkMode.LOCAL_CODE;
    }
}
