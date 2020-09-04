/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */
package com.apollocurrency.apl.id.cert;

import com.apollocurrency.apl.id.utils.StringList;
import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.KeyGenerator;
import io.firstbridge.cryptolib.KeyWriter;
import io.firstbridge.cryptolib.csr.CertificateRequestData;
import io.firstbridge.cryptolib.csr.X509CertOperations;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Certificate signing request with additional identity-specific attributes
 *
 * @author alukin@gmail.com
 */
public class CSRHelper extends CertBase {

    private static final Logger log = LoggerFactory.getLogger(CSRHelper.class);
    private final CertificateRequestData csrData = new CertificateRequestData(CertificateRequestData.CSRType.HOST);
    private String challengePassword = "";
    private BigInteger actorID;
    private AuthorityID authorityID;
    private final KeyWriter kw;
    
    public CSRHelper() {
        actorID = new BigInteger(128, new SecureRandom());
        authorityID = new AuthorityID();
        kw = factory.getKeyWriter();
    }

    public static CSRHelper loadCSR(String path) {
        PKCS10CertificationRequest cr;
        CSRHelper res = null;
        try (FileReader fr = new FileReader(path)) {
            PEMParser parser = new PEMParser(fr);
            cr = (PKCS10CertificationRequest) parser.readObject();
            res = CSRHelper.fromPKCS10(cr);
        } catch (IOException ex) {
            log.error("Can not read PKCS#10 file: " + path, ex);
        }
        return res;
    }

    public static CSRHelper fromPKCS10(PKCS10CertificationRequest cr) {
        CSRHelper res = new CSRHelper();
        try {
            CertAttributes va  = new CertAttributes();
            va.setSubject(cr.getSubject());
            va.setAttributes(cr.getAttributes());
            res.setCN(va.getCn());
            res.setAuthorityId(va.getAuthorityId());
            res.setActorId(va.getApolloId());
            res.setCountry(va.getCountry());
            res.setState(va.getState());
            res.setCity(va.getCity());
            res.setOrg(va.getO());
            res.setOrgUnit(va.getOu());

            SubjectPublicKeyInfo pkInfo = cr.getSubjectPublicKeyInfo();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            res.pubKey = converter.getPublicKey(pkInfo);

        } catch (CertException | IOException ex) {
            log.error("Error reading public key frpm PKSC#10", ex);
        }
        return res;
    }

    public static CSRHelper fromCertificate(CertHelper cert) {
        CSRHelper res = new CSRHelper();
        res.setAuthorityId(cert.getAuthorityId().getAuthorityID());
        BigInteger vid = cert.getApolloId();
        if (vid == null || vid == BigInteger.ZERO) {
            vid = new BigInteger(128, new SecureRandom());
        }
        res.setActorId(vid);
        res.setCN(cert.getCN());
        res.setEmail(cert.getEmail());
        res.setOrg(cert.getOrganization());
        res.setOrgUnit(cert.getOrganizationUnit());
        res.setCountry(cert.getCountry());
        res.setState(cert.getStateOrProvince());
        res.setCity(cert.getCity());
        res.setIP(StringList.fromList(cert.getIPAddresses()));
        res.setDNSNames(StringList.fromList(cert.getDNSNames()));
        res.pubKey = cert.getPublicKey();
        res.pvtKey = cert.getPrivateKey();
        return res;
    }

    public static boolean isValidIPAddresList(String ipList) {
        boolean res = true;
        String[] addr = ipList.split(",");
        for (String a : addr) {
            res = IPAddress.isValid(a) || IPAddress.isValidWithNetMask(a);
            if (!res) {
                break;
            }
        }
        return res;
    }

    public static boolean isVaidDNSNameList(String nameList) {
        boolean res = true;
        String[] names = nameList.split(",");
        String pattern = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        for (String n : names) {
            res = n.matches(pattern);
            if (!res) {
                break;
            }
        }
        return res;
    }

    public BigInteger getActorId() {
        return actorID;
    }

    public void setActorId(BigInteger id) {
        actorID = id;
        csrData.setSubjectAttribute("UID", actorID.toString(16));
    }

    public AuthorityID getAuthorityId() {
        return authorityID;
    }

    public void setAuthorityId(BigInteger id) {
        authorityID = new AuthorityID(id);
        csrData.setSubjectAttribute("businessCategory", authorityID.getAuthorityID().toString(16));
    }

    public void setAuthorityId(AuthorityID authID) {
        this.authorityID = authID;
        csrData.setSubjectAttribute("businessCategory", authID.getAuthorityID().toString(16));
    }

    public String getCN() {
        String res = csrData.getSubjectAttribute("CN");
        if (res == null) {
            res = "";
        }
        return res;
    }

    public void setCN(String cn) {
        csrData.setSubjectAttribute("CN", cn);
    }

    public String getEmial() {
        String res = csrData.getSubjectAttribute("emailAddress");
        if (res == null) {
            res = "";
        }
        return res;
    }

    public void setEmail(String email) {
        csrData.setSubjectAttribute("emailAddress", email);
    }

    public String getIP() {
        return csrData.getExtendedAttribute("subjaltnames.ipaddress");
    }

    public void setIP(String ip) {
        if (ip != null && !ip.isEmpty()) {
            if (isValidIPAddresList(ip)) {
                csrData.setExtendedAttribute("subjaltnames.ipaddress", ip);
            } else {
                throw new IllegalArgumentException("Invalid IP4 or IP6 addres: " + ip);
            }
        }
    }

    public String getDNSNames() {
        return csrData.getExtendedAttribute("subjaltnames.dnsname");
    }

    public void setDNSNames(String n) {
        if (n != null && !n.isEmpty()) {
            if (isVaidDNSNameList(n)) {
                csrData.setExtendedAttribute("subjaltnames.dnsname", n);
            } else {
                throw new IllegalArgumentException("Invalid DNS name: " + n);
            }
        }
    }

    public String getOrgUnit() {
        return csrData.getSubjectAttribute("OU");
    }

    public void setOrgUnit(String ou) {
        csrData.setSubjectAttribute("OU", ou);
    }

    public String getOrg() {
        return csrData.getSubjectAttribute("O");
    }

    public void setOrg(String o) {
        csrData.setSubjectAttribute("O", o);
    }

    public String getCountry() {
        return csrData.getSubjectAttribute("C");
    }

    public void setCountry(String c) {
        csrData.setSubjectAttribute("C", c);
    }

    public String getState() {
        return csrData.getSubjectAttribute("ST");
    }

    public void setState(String c) {
        csrData.setSubjectAttribute("ST", c);
    }

    public String getCity() {
        return csrData.getSubjectAttribute("L");
    }

    public void setCity(String c) {
        csrData.setSubjectAttribute("L", c);
    }

    public String getChallengePassword() {
        return challengePassword;
    }

    public void setChallengePassword(String challengePassword) {
        this.challengePassword = challengePassword;
    }

    public String getPemPKCS10() {
        String pem = "";
        try {
            csrData.processCertData(false);
            if (pvtKey == null) {
                newKeyPair();
            }
            KeyPair kp = new KeyPair(pubKey, pvtKey);
            X509CertOperations certOps = factory.getX509CertOperations();
            PKCS10CertificationRequest cr = certOps.createX509CertificateRequest(kp, csrData, false, challengePassword);
            pem = kw.getCertificateRequestPEM(cr);
        } catch (IOException ex) {
            log.error("Can not generate PKSC10 CSR", ex);
        } catch (CryptoNotValidException ex) {
            log.error("Can not generate PKSC10 CSR, Invalid data", ex);
        }
        return pem;
    }

    public String getPrivateKeyPEM() {
        String pem = "";
        try {
            pem = kw.getPvtKeyPEM(pvtKey);
        } catch (IOException ex) {
            log.error("Can not get PEM of private key", ex);
        }
        return pem;
    }

    public String getSelfSignedX509PEM() {
        String pem = "";
        try {
            csrData.processCertData(true);
            if (pvtKey == null) {
                newKeyPair();
            }
            KeyPair kp = new KeyPair(pubKey, pvtKey);
            X509CertOperations certOps = factory.getX509CertOperations();            
            X509Certificate cert = certOps.createSelfSignedX509v3(kp, csrData);
            pem = kw.getX509CertificatePEM(cert);
        } catch (CryptoNotValidException | IOException ex) {
            log.error("Can not generate self-signed PEM", ex);
        }
        return pem;
    }

    @Override
    public String toString() {
        String res = "X.509 Certificate:\n";
        res += "CN=" + getCN() + "\n"
                + "ActorID=" + getActorId().toString(16) + "\n";
        res += "emailAddress=" + getEmial() + "\n";
        res += "Country=" + getCountry() + " State/Province=" + getState()
                + " City=" + getCity();
        res += "Organization=" + getOrg() + " Org. Unit=" + getOrgUnit() + "\n";
        res += "IP address=" + getIP() + "\n";
        res += "DNS names=" + getDNSNames() + "\n";
        return res;
    }

    private void newKeyPair() {
        KeyGenerator kg = factory.getKeyGenerator();
        KeyPair kp = kg.generateKeys();
        pubKey = kp.getPublic();
        pvtKey = kp.getPrivate();
    }

}
