package com.apollocurrency.aplwallet.apl.util.cert;

import com.apollocurrency.aplwallet.apl.util.cls.BasicClassificator;
import com.apollocurrency.aplwallet.apl.util.cls.ClsItem;
import com.apollocurrency.aplwallet.apl.util.cls.AplClassificators;
import io.firstbridge.cryptolib.FBCryptoParams;
import io.firstbridge.cryptolib.KeyWriter;
import io.firstbridge.cryptolib.csr.CertificateRequestData;
import io.firstbridge.cryptolib.csr.KeyGenerator;
import io.firstbridge.cryptolib.CryptoNotValidException;
import io.firstbridge.cryptolib.impl.KeyWriterImpl;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Certificate signing request with Apollo-specific attributes
 *
 * @author alukin@gmail.com
 */
public class ApolloCSR extends CertBase{

    private static final Logger log = LoggerFactory.getLogger(ApolloCSR.class);

    public static ApolloCSR fromPKCS10(PKCS10CertificationRequest cr) {
        ApolloCSR res = new ApolloCSR();        
        try {
            CertAttributes va = new CertAttributes();
            va.setSubject(cr.getSubject());
            va.setAttributes(cr.getAttributes());
            res.setCN(va.getCn());
            res.setAuthID(va.getAuthorityId());
            res.setApolloID(va.getApolloId());
            res.setCountry(va.getCountry());
            res.setState(va.getState());
            res.setCity(va.getCity());
            res.setOrg(va.getO());
            res.setOrgUnit(va.getOu());

            SubjectPublicKeyInfo pkInfo = cr.getSubjectPublicKeyInfo();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            res.pubKey = converter.getPublicKey(pkInfo);
           
        } catch (ApolloCertificateException |IOException ex) {
            log.error("Error reading public key frpm PKSC#10",ex);
        }
        return res;
    }

 
    private final CertificateRequestData certData = new CertificateRequestData(CertificateRequestData.CSRType.HOST);
    private boolean allowCertSign = false;
    private String challengePassword = "";
    private BigInteger apolloID;
    private AuthorityID apolloAuthID = new AuthorityID();
    private  KeyWriter kw = new KeyWriterImpl();

    public static ApolloCSR fromCertificate(ApolloCertificate cert) {
        ApolloCSR res = new ApolloCSR();
        res.setApolloAuthorityID(cert.getAuthorityId().getAuthorityID());
        BigInteger vid = cert.getApolloId();
        if(vid==null||vid==BigInteger.ZERO){
            vid = new BigInteger(128, new SecureRandom());
        }
        res.setApolloID(vid);
        res.setCN(cert.getCN());
        res.setEmail(cert.getEmail());
        res.setOrg(cert.getOrganization());
        res.setOrgUnit(cert.getOrganizationUnit());
        res.setCountry(cert.getCountry());
        res.setState(cert.getStateOrProvince());
        res.setCity(cert.getCity());
        res.setIP(cert.fromList(cert.getIPAddresses()));
        res.setDNSNames(cert.fromList(cert.getDNSNames()));
        res.pubKey = cert.getPublicKey();
        res.pvtKey = cert.getPrivateKey();
        return res;
    }

    public ApolloCSR() {
        apolloID = new BigInteger(128, new SecureRandom());
    }

    public BigInteger getApolloID() {
        return apolloID;
    }

    public void setApolloID(BigInteger id) {
        apolloID = id;
        certData.setSubjectAttribute("UID", apolloID.toString(16));
    }

    public AuthorityID getApolloAuthorityID() {
        return apolloAuthID;
    }


    public void setApolloAuthorityID(BigInteger id) {
        apolloAuthID = new AuthorityID(id);
        certData.setSubjectAttribute("businessCategory", apolloAuthID.getAuthorityID().toString(16));
    }

    public AuthorityID getAuthID() {
        return apolloAuthID;
    }

    public void setAuthID(AuthorityID authID) {
        this.apolloAuthID = authID;
        certData.setSubjectAttribute("businessCategory", authID.getAuthorityID().toString(16));
    }


    public String getCN() {
        String res = certData.getSubjectAttribute("CN");
        if(res==null){
            res="";
        }
        return res;
    }

    public void setCN(String cn) {
          certData.setSubjectAttribute("CN", cn);
    }

    public String getEmial() {
        String res =  certData.getSubjectAttribute("emailAddress");
        if(res==null){
            res="";
        }
        return res;        
    }

    public void setEmail(String email) {
        certData.setSubjectAttribute("emailAddress", email);
    }

    public String getIP() {
        return certData.getExtendedAttribute("subjaltnames.ipaddress");
    }

    public String getDNSNames() {
        return certData.getExtendedAttribute("subjaltnames.dnsname");
    }

    public void setIP(String ip) {
        if(ip!=null && !ip.isEmpty()){
        if(isValidIPAddresList(ip)){
            certData.setExtendedAttribute("subjaltnames.ipaddress", ip);
        }else{
            throw new IllegalArgumentException("Invalid IP4 or IP6 addres: "+ip);
        }
        }
    }

    public void setDNSNames(String n) {
       if(n!=null && !n.isEmpty()){
        if(isVaidDNSNameList(n)){
          certData.setExtendedAttribute("subjaltnames.dnsname", n);
        }else{
            throw new IllegalArgumentException("Invalid DNS name: "+n); 
        }
       }
    }

    public String getOrgUnit() {
        return certData.getSubjectAttribute("OU");
    }

    public void setOrgUnit(String ou) {
        certData.setSubjectAttribute("OU", ou);
    }

    public String getOrg() {
        return certData.getSubjectAttribute("O");
    }

    public void setOrg(String o) {
        certData.setSubjectAttribute("O", o);
    }

    public void setCountry(String c) {
        certData.setSubjectAttribute("C", c);
    }

    public String getCountry() {
        return certData.getSubjectAttribute("C");
    }

    public void setState(String c) {
        certData.setSubjectAttribute("ST", c);
    }

    public String getState() {
        return certData.getSubjectAttribute("ST");
    }

    public void setCity(String c) {
        certData.setSubjectAttribute("L", c);
    }

    public String getCity() {
        return certData.getSubjectAttribute("L");
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
            certData.processCertData(false);
            if(pvtKey==null){
                newKeyPair();
            }
            KeyPair kp = new KeyPair(pubKey, pvtKey);
            KeyGenerator kg = new KeyGenerator(FBCryptoParams.createDefault());
            PKCS10CertificationRequest cr = kg.createX509CertificateRequest(kp, certData, false, challengePassword);
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
            certData.processCertData(true);
            if(pvtKey==null){
                newKeyPair();
            }
            KeyPair kp = new KeyPair(pubKey, pvtKey);
            KeyGenerator kg = new KeyGenerator(FBCryptoParams.createDefault());
            X509Certificate cert = kg.createSerlfSignedX509v3(kp, certData);
            pem = kw.getX509CertificatePEM(cert);            
        } catch (CryptoNotValidException | IOException ex) {
            log.error("Can not generate self-signed PEM", ex);
        }
        return pem;
    }
    
    public static boolean isValidIPAddresList(String ipList){
       boolean res = true;
       String[] addr = ipList.split(",");
       for(String a: addr){
           res = IPAddress.isValid(a)||IPAddress.isValidWithNetMask(a);
           if(!res){
               break;
           }
       }
       return res;
    }
    
    public static boolean isVaidDNSNameList(String nameList){
        boolean res = true;
        String[] names = nameList.split(",");
        String pattern = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        for (String n: names){
            res = n.matches(pattern);
            if(!res){
                break;
            }
        }
        return res;
    }
    
    @Override
    public String toString(){
        String res = "X.509 Certificate:\n";
        res+=   "CN="+getCN()+"\n"
               +"ApolloID="+getApolloID().toString(16)+"\n";
        res+="emailAddress="+getEmial()+"\n";
        res+="Country="+getCountry()+ " State/Province="+getState()
                +" City="+getCity();
        res+="Organization="+getOrg()+" Org. Unit="+getOrgUnit()+"\n";
        res+="IP address="+getIP()+"\n";
        res+="DNS names="+getDNSNames()+"\n";
        return res; 
    }

    private void newKeyPair() {
        KeyGenerator kg = new KeyGenerator(FBCryptoParams.createDefault());
        KeyPair kp = kg.generateKeys();
        pubKey = kp.getPublic();
        pvtKey = kp.getPrivate();
    }

    public void setCertPurpose(String certPurpose) {
        BasicClassificator cps = AplClassificators.getCls(AplClassificators.CERTPURPOSE_CLS);
        ClsItem item = cps.getItem(certPurpose);
        if(item==null){
          certData.setCSRType(CertificateRequestData.CSRType.HOST);
        }else{
            certData.setCSRType(CertificateRequestData.CSRType.valueOf(item.value));
        }
    }

}
