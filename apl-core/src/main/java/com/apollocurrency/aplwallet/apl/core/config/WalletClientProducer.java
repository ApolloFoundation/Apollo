package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WalletClientProducer {

    @Inject
    private PropertiesHolder propertiesHolder;

    @Produces
    public Web3j initETHClient(){
        String ethNodeUrl = propertiesHolder.getStringProperty("apl.eth.node.url");
        String ethNodePort = propertiesHolder.getStringProperty("apl.eth.node.port");
        //TODO move HttpService config to config files.
        String fullUrl = ethNodeUrl;

        if(!StringUtils.isBlank(ethNodePort)) {
            fullUrl = fullUrl.concat(":" + ethNodePort);
        }

        Web3j web3 = Web3j.build(new HttpService(fullUrl));  // defaults to http://localhost:8545/
        return web3;
    }

}
