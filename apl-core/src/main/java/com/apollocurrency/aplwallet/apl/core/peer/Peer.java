/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.request.BaseP2PRequest;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.peer.parser.JsonReqRespParser;
import com.apollocurrency.aplwallet.apl.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Set;
import java.util.UUID;

public interface Peer extends Comparable<Peer> {
    /** max time difference allowed between this and remote node to
     * check siganture of epoch time
     */
    int MAX_TIME_DIFF = 2;
    /**
     * @return ID of peer. It is UID field of X.509 certificate
     */
    String getIdentity();

    boolean providesService(Service service);

    boolean providesServices(long services);

    String getHost();

    int getPort();

    String getHostWithPort();

    String getAnnouncedAddress();

    void setAnnouncedAddress(String addr);

    PeerState getState();

    Version getVersion();

    String getApplication();

    String getPlatform();

    String getSoftware();

    int getApiPort();

    int getApiSSLPort();

    Set<APIEnum> getDisabledAPIs();

    int getApiServerIdleTimeout();

    BlockchainState getBlockchainState();

    Hallmark getHallmark();

    int getWeight();

    boolean shareAddress();

    boolean isBlacklisted();

    void blacklist(Exception cause);

    void blacklist(String cause);

    void unBlacklist();

    void deactivate(String reason);

    void remove();

    long getDownloadedVolume();

    long getUploadedVolume();

    int getLastUpdated();

    int getLastConnectAttempt();

    boolean isInbound();

    boolean isOutbound();

    boolean isOpenAPI();

    boolean isApiConnectable();

    UUID getChainId();

    StringBuilder getPeerApiUri();

    String getBlacklistingCause();

    @Deprecated
    JSONObject send(JSONStreamAware request, UUID chainId) throws PeerNotConnectedException;

    //TODO: add base response
    JSONObject send(BaseP2PRequest request) throws PeerNotConnectedException;

    <T> T send(BaseP2PRequest request, JsonReqRespParser<T> parser) throws PeerNotConnectedException;

    PeerTrustLevel getTrustLevel();

    void sendAsync(BaseP2PRequest request);

    long getServices();

    long getLastActivityTime();

    Peer2PeerTransport getP2pTransport();

    boolean processError(JSONObject request);

    void setServices(long code);

    void setLastUpdated(int time);

    String getX509pem();

    enum Service {
        HALLMARK(1),                    // Hallmarked node
        PRUNABLE(2),                    // Stores expired prunable messages
        API(4),                         // Provides open API access over http
        API_SSL(8),                     // Provides open API access over https
        CORS(16);                       // API CORS enabled

        private final long code;        // Service code - must be a power of 2

        Service(int code) {
            this.code = code;
        }

        public long getCode() {
            return code;
        }
    }
}
