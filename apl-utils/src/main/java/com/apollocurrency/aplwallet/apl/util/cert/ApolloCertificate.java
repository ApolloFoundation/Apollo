package com.apollocurrency.aplwallet.apl.util.cert;

import io.firstbridge.cryptolib.KeyReader;
import io.firstbridge.cryptolib.KeyWriter;
import io.firstbridge.cryptolib.impl.KeyReaderImpl;
import io.firstbridge.cryptolib.impl.KeyWriterImpl;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents X.509 certificate with Apollo-specific attributes and signed by
 * Apollo CA or self-signed
 *
 * @author alukin@gmail.com
 */
public class ApolloCertificate extends CertBase {

    private static final Logger log = LoggerFactory.getLogger(ApolloCertificate.class);

    private final X509Certificate certificate;
    private final CertAttributes cert_attr;
    private final CertAttributes issuer_attr;
    
    public static ApolloCertificate loadPEMFromPath(String path) throws  ApolloCertificateException, IOException {
        ApolloCertificate res = null;
        try(FileInputStream fis = new FileInputStream(path)){
           res = ApolloCertificate.loadPEMFromStream(fis);
        }
        return res;
    }
    
    public static ApolloCertificate loadPEMFromStream(InputStream is) throws IOException, ApolloCertificateException {
        KeyReader kr = new KeyReaderImpl();
        X509Certificate cert = kr.readX509CertPEMorDER(is);
        ApolloCertificate ac = new ApolloCertificate(cert);
        return ac;
    }
    
    public ApolloCertificate(X509Certificate certificate) throws ApolloCertificateException {
        if (certificate == null) {
            throw new ApolloCertificateException("Null certificate");
        }
        this.certificate = certificate;
        pubKey = certificate.getPublicKey();
        cert_attr = new CertAttributes();
        issuer_attr = new CertAttributes();
        cert_attr.setSubjectStr(certificate.getSubjectX500Principal().toString());
        issuer_attr.setSubjectStr(certificate.getIssuerX500Principal().toString());
    }


    public BigInteger getApolloId() {
        return cert_attr.getApolloId();
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

    public String fromList(List<String> sl) {
        String res = "";
        for (int i = 0; i < sl.size(); i++) {
            String semicolon = i < sl.size() - 1 ? ";" : "";
            res += sl.get(i) + semicolon;
        }
        return res;
    }

    public List<String> fromString(String l) {
        List<String> res = new ArrayList<>();
        String[] ll = l.split(";");
        for (String s : ll) {
            if (!s.isEmpty()) {
                res.add(s);
            }
        }
        return res;
    }
    
    @Override
    public String toString() {
        String res = "Apollo X.509 Certificate:\n";
        res += "CN=" + cert_attr.getCn() + "\n"
                + "ApolloID=" + getApolloId().toString(16) + "\n";

        res += "emailAddress=" + getEmail() + "\n";
        res += "Country=" + getCountry() + " State/Province=" + getStateOrProvince()
                + " City=" + getCity();
        res += "Organization=" + getOrganization() + " Org. Unit=" + getOrganizationUnit() + "\n";
        res += "IP address=" + fromList(getIPAddresses()) + "\n";
        res += "DNS names=" + fromList(getDNSNames()) + "\n";
        return res;
    }

    public String getPEM() {
        String res = "";
        KeyWriter kw = new KeyWriterImpl();
        try {
            res = kw.getX509CertificatePEM(certificate);
        } catch (IOException ex) {
            log.error("Can not get certificate PEM", ex);
        }
        return res;
    }

    public boolean isValid(Date date) {
        boolean dateOK=false;
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
    
    public CertAttributes getIssuerAttrinutes(){
        return issuer_attr;
    }
    
    public boolean verify(){
        boolean res = true;
        //TODO: implement
        return res;
    }
}
