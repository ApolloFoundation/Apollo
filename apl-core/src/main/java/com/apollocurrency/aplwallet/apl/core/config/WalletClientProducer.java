package com.apollocurrency.aplwallet.apl.core.config;

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
        String ethNodeUrl = propertiesHolder.getStringProperty("eth.node.url");
        String ethNodePort = propertiesHolder.getStringProperty("eth.node.port");
        //TODO move HttpService config to config files.
        Web3j web3 = Web3j.build(new HttpService(ethNodeUrl + ":" + ethNodePort));  // defaults to http://localhost:8545/
        return web3;
    }

}
