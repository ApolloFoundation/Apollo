/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.security.id;



import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import io.firstbridge.cryptolib.CryptoFactory;
import io.firstbridge.cryptolib.KeyWriter;
import io.firstbridge.identity.cert.ActorType;
import io.firstbridge.identity.cert.AuthorityID;
import io.firstbridge.identity.cert.CertAndKey;
import io.firstbridge.identity.cert.ExtCSR;
import io.firstbridge.identity.cert.ExtCert;
import io.firstbridge.identity.handler.CertificateLoader;
import io.firstbridge.identity.handler.CertificateLoaderImpl;
import io.firstbridge.identity.handler.IdValidator;
import io.firstbridge.identity.handler.IdValidatorImpl;
import io.firstbridge.identity.handler.PrivateKeyLoader;
import io.firstbridge.identity.handler.PrivateKeyLoaderImpl;
import io.firstbridge.identity.handler.ThisActorIdHandler;
import io.firstbridge.identity.handler.ThisActorIdHandlerImpl;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.List;

/**
 * Identity service implementation
 *
 * @author alukin@gmail.com
 */
@Slf4j
@Singleton
public class IdentityServiceImpl implements IdentityService {

    private ThisActorIdHandler thisNodeIdHandler;
    private final IdValidator peerIdValidator;
    public Path myCertPath;
    public Path myKeyPath;
    public Path apolloCaPath;

    private final ConfigDirProvider dirProvider;

    @Inject
    public IdentityServiceImpl(ConfigDirProvider dirProvider) {
        this.dirProvider=dirProvider;
        this.peerIdValidator = new IdValidatorImpl();
        myCertPath = Path.of(dirProvider.getUserConfigLocation())
                .resolve("certificates")
                .resolve(dirProvider.getChainIdPart())
                .resolve("this_node.crt");
        myKeyPath =  Path.of(dirProvider.getUserConfigLocation())
                .resolve("keys")
                .resolve(dirProvider.getChainIdPart())
                .resolve("this_node.key");
        apolloCaPath =  Path.of(dirProvider.getInstallationConfigLocation())
                .resolve("conf")
                .resolve("CA-certs");

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
    public boolean loadMyIdentity() {
        boolean res = true;
        CertificateLoader cl = new CertificateLoaderImpl();
        ExtCert myCert = cl.loadCert(myCertPath);
        PrivateKey privKey = null;
        if(myCert!=null){
            PrivateKeyLoader pkl = new PrivateKeyLoaderImpl();
            privKey = pkl.loadAndCheckPrivateKey(myKeyPath, myCert, null);
            if(privKey==null){
                return false;
            }
            thisNodeIdHandler = new ThisActorIdHandlerImpl(myCert, privKey);
        }else{ //we do not have node certificate yet, have to generate it
            thisNodeIdHandler = new ThisActorIdHandlerImpl();
            CertAndKey certAndKey = thisNodeIdHandler.generateSelfSignedCert(fillCertProperties());

            KeyWriter kw = CryptoFactory.newInstance().getKeyWriter();

            try {
                myKeyPath.getParent().toFile().mkdirs();
                kw.writePvtKeyPEM(myKeyPath.toString(), certAndKey.getPvtKey());
                myCertPath.getParent().toFile().mkdirs();
                kw.writeX509CertificatePEM(myCertPath.toString(), certAndKey.getCert());
            } catch (IOException ex) {
                log.error("Can not wirite generated node keys");
                res=false;
            }

        }

        return res;
    }

    /**
     * Fill the fields of X.509 certificate actually
     * with some "placeholders" and generated NodeID
     * @return filled CSR ready to sign or self-sign
     */
    private ExtCSR fillCertProperties() {
        //generate random 256-bit NodeID
        byte[] nodeId = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(nodeId);
        ExtCSR csr = new ExtCSR();

        csr.setActorId(nodeId);

        AuthorityID authId = new AuthorityID();
        authId.setActorType(ActorType.NODE);
        authId.setNetId(dirProvider.getChainId());
        authId.setAuthorityCode(0);
        authId.setBusinessCode(0);

        csr.setAuthorityId(authId);
        String email = csr.getActorIdAsHex()+"@apollowallet.org";
        csr.setCN(email);
        csr.setOrg("Apollo blockchain");
        csr.setOrgUnit("Apollo-nodes");
        csr.setCity("Anywhere");
        csr.setCountry("US");
        csr.setEmail(email);
        csr.setIP("127.0.0.1");
        csr.setDNSNames(csr.getActorIdAsHex()+".apollowallet.org");

        return csr;
    }

    @Override
    public boolean loadTrustedCaCerts() {
        CertificateLoader cl = new CertificateLoaderImpl();
        List<ExtCert> caList = cl.loadCertsFromDir(apolloCaPath);
        if(caList.isEmpty()){
            return false;
        }
        caList.forEach(cert -> {
            peerIdValidator.addTrustedSignerCert(cert.getCertificate());
        });
        return true;
    }

}
