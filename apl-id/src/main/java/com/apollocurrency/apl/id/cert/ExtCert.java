package com.apollocurrency.apl.id.cert;

import com.apollocurrency.apl.id.utils.StringList;
import io.firstbridge.cryptolib.KeyWriter;
import io.firstbridge.cryptolib.impl.KeyWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import lombok.Getter;

/**
 * Represents X.509 certificate with Apollo-specific attributes and signed by
 * Apollo CA or self-signed
 *
 * @author alukin@gmail.com
 */
public class ExtCert extends CertBase {

    private static final Logger log = LoggerFactory.getLogger(ExtCert.class);

    @Getter
    private final X509Certificate certificate;
    private final CertAttributes cert_attr;
    private final CertAttributes issuer_attr;




    public ExtCert(X509Certificate certificate) throws CertException {
        if (certificate == null) {
            throw new CertException("Null certificate");
        }
        this.certificate = certificate;
        pubKey = certificate.getPublicKey();
        cert_attr = new CertAttributes();
        issuer_attr = new CertAttributes();
        cert_attr.setSubjectStr(certificate.getSubjectX500Principal().toString());
        issuer_attr.setSubjectStr(certificate.getIssuerX500Principal().toString());
    }

    
    
    public BigInteger getActorId() {
        return cert_attr.getActorId();
    }

    public AuthorityID getAuthorityId() {
        return cert_attr.getAuthorityId();
    }


    public String getCN() {
        return cert_attr.getCn();
    }

    public String getOrganization() {
        return cert_attr.getO();
    }

    public String getOrganizationUnit() {
        return cert_attr.getOu();
    }

    public String getCountry() {
        return cert_attr.getCountry();
    }

    public String getCity() {
        return cert_attr.getCity();
    }

    public String getCertificatePurpose() {
        return "Node";
        //TODO: implement recognitioin from extended attributes
    }

    public List<String> getIPAddresses() {
        return cert_attr.IpAddresses();
    }

    public List<String> getDNSNames() {
        return null;
        //TODO: implement
    }

    public String getStateOrProvince() {
        return null;
    }

    public String getEmail() {
        return cert_attr.geteMail();
    }

    @Override
    public String toString() {
        String res = "X.509 Certificate:\n";
        res += "CN=" + cert_attr.getCn() + "\n"
            + "ApolloID=" + getActorId().toString(16) + "\n";

        res += "emailAddress=" + getEmail() + "\n";
        res += "Country=" + getCountry() + " State/Province=" + getStateOrProvince()
            + " City=" + getCity();
        res += "Organization=" + getOrganization() + " Org. Unit=" + getOrganizationUnit() + "\n";
        res += "IP address=" + StringList.fromList(getIPAddresses()) + "\n";
        res += "DNS names=" + StringList.fromList(getDNSNames()) + "\n";
        return res;
    }

    public String getCertPEM() {
        KeyWriter kw = new KeyWriterImpl();
        String res="";
        try {
            res = kw.getX509CertificatePEM(certificate);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ExtCert.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }

    public boolean isValid(Date date) {
        boolean dateOK = false;
        Date start = certificate.getNotBefore();
        Date end = certificate.getNotAfter();
        if (date != null && start != null && end != null) {
            if (date.after(start) && date.before(end)) {
                dateOK = true;
            } else {
                dateOK = false;
            }
        }
        //TODO: implement more checks
        return dateOK;
    }

    public BigInteger getSerial() {
        return certificate.getSerialNumber();
    }

    public CertAttributes getIssuerAttrinutes() {
        return issuer_attr;
    }

    public boolean verify(X509Certificate certificate) {
        try {
            this.certificate.verify(certificate.getPublicKey());
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
            return false;
        }
        return true;
    }
    
    public boolean isSelfSigned(){
        //TODO: implement
        return true;
    }

    public boolean isSignedBy(X509Certificate signerCert) {
        return verify(signerCert);
    }

}
