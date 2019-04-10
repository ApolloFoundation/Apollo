package com.apollocurrency.aplwallet.apl.util.cert;

import io.firstbridge.cryptolib.KeyReader;
import io.firstbridge.cryptolib.KeyWriter;
import io.firstbridge.cryptolib.impl.KeyReaderImpl;
import io.firstbridge.cryptolib.impl.KeyWriterImpl;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    private final CertAttributes va  = new CertAttributes();

    public static ApolloCertificate loadPEMFromPath(String path) throws FileNotFoundException, ApolloCertificateException {
        KeyReader kr = new KeyReaderImpl();
        X509Certificate cert = kr.readX509CertPEMorDER(new FileInputStream(path));
        ApolloCertificate vc = new ApolloCertificate(cert);
        vc.parseAttributes();
        return vc;
    }

    public ApolloCertificate(X509Certificate certificate) throws ApolloCertificateException {
        if (certificate == null) {
            throw new ApolloCertificateException("Null certificate");
        }
        this.certificate = certificate;
        parseAttributes();
        pubKey = certificate.getPublicKey();
    }

    public final void parseAttributes() throws ApolloCertificateException {
        va.setSubjectStr(certificate.getSubjectX500Principal().toString());
    }

    public BigInteger getApolloId() {
        return va.getApolloId();
    }

    public AuthorityID getAuthorityId() {
        return va.getAuthorityId();
    }


    public String getCN() {
        return va.getCn();
    }

    public String getOrganization() {
        return va.getO();
    }

    public String getOrganizationUnit() {
        return va.getOu();
    }

    public String getCountry() {
        return va.getCountry();
    }

    public String getCity() {
        return va.getCity();
    }

    public String getCertificatePurpose() {
        return "Node";
        //TODO: implement recognitioin from extended attributes
    }

    public List<String> getIPAddresses() {
        return null;
        //TODO: imiplement
    }

    public List<String> getDNSNames() {
        return null;
        //TODO: implement
    }

    public String getStateOrProvince() {
        return null;
    }

    public String getEmail() {
        return null;
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
        res += "CN=" + va.getCn() + "\n"
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
}
