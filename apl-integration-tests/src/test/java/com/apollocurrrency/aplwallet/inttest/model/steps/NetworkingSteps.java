package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.response.GetPeersIpResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static io.restassured.RestAssured.given;

public class NetworkingSteps extends StepBase {

    @Step
    @DisplayName("Get All Peers")
    public List<String> getPeers() {
        String path = "/rest/networking/peer/all";
        return given().log().uri()
            .spec(restHelper.getSpec())
            .when()
            .get(path).as(GetPeersIpResponse.class).getPeers();

    }

    @Step
    @DisplayName("Get Peer")
    public PeerDTO getPeer(String peer) {
        String path = String.format("/rest/networking/peer?peer=%s", peer);
        return given().log().uri()
            .spec(restHelper.getSpec())
            .when()
            .get(path).as(PeerDTO.class);
    }

    @Step
    public PeerDTO addPeer(String ip) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.ADD_PEER);
        param.put(ReqParam.PEER, ip);
        param.put(ReqParam.ADMIN_PASSWORD, getTestConfiguration().getAdminPass());

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", PeerDTO.class);
    }

    @Step("Get My Peer Info")
    public PeerInfo getMyInfo() {
        String path = "/rest/networking/peer/mypeerinfo";
        return given().log().uri()
            .spec(restHelper.getSpec())
            .when()
            .get(path).as(PeerInfo.class);
    }

    @Step
    public BlockchainInfoDTO getBlockchainStatus() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCKCHAIN_STATUS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", BlockchainInfoDTO.class);
    }
}
