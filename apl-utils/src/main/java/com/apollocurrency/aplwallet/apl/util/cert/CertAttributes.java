package com.apollocurrency.aplwallet.apl.util.cert;

import java.math.BigInteger;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x500.X500Name;

/**
 * PKCS#10 and X.509 attribute parser
 *
 * @author alukin@gmail.com
 */
public class CertAttributes {

    private BigInteger apolloId = BigInteger.ZERO;
    private AuthorityID apolloAuthorityId = new AuthorityID();
    private String cn = "";
    private String o = "";
    private String ou = "";
    private String country = "";
    private String state = "";
    private String city = "";
    private String eMail = "";

    public CertAttributes() {
    }

    public void setAttributes(Attribute[] aa) {

    }

    public void setSubject(X500Name sn) throws ApolloCertificateException {
        String name = sn.toString();
        setSubjectStr(name);
    }

    public void setSubjectStr(String name) throws ApolloCertificateException {
        System.out.println("NAME: " + name);
        String[] names = name.split(",");
        try {
            for (String name1 : names) {
                String[] nvs = name1.split("=");
                String an = nvs[0].trim();
                String av = nvs.length>1 ? nvs[1].trim():"";
                if (an.equalsIgnoreCase("CN")) {
                    cn = av;
                } else if (an.equalsIgnoreCase("O")) {
                    o = av;
                } else if (an.equalsIgnoreCase("OU")) {
                    ou = av;
                } else if (an.equalsIgnoreCase("C")) {
                    country = av;
                } else if (an.equalsIgnoreCase("ST")) {
                    state = av;
                } else if (an.equalsIgnoreCase("L")) {
                    city = av;
                } else if (an.equalsIgnoreCase("EMAILADDRESS")) {
                    eMail = av;
                } else if (an.trim().equalsIgnoreCase("UID")) {
                    apolloId = new BigInteger(av, 16);
                } else if (an.equalsIgnoreCase("businessCategory")
                        || an.equalsIgnoreCase("OID.2.5.4.15")) {
                    apolloAuthorityId = new AuthorityID(new BigInteger(av, 16));
                }
            }
        } catch (NumberFormatException ex) {
            throw new ApolloCertificateException(ex.getMessage());
        }
    }

    public BigInteger getApolloId() {
        return apolloId;
    }

    public void setApolloId(BigInteger id) {
        this.apolloId = id;
    }

    public AuthorityID getAuthorityId() {
        return apolloAuthorityId;
    }

    public void setAuthorityId(AuthorityID authorityId) {
        this.apolloAuthorityId = authorityId;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getO() {
        return o;
    }

    public void setO(String o) {
        this.o = o;
    }

    public String getOu() {
        return ou;
    }

    public void setOu(String ou) {
        this.ou = ou;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String geteMail() {
        return eMail;
    }

    public void seteMail(String eMail) {
        this.eMail = eMail;
    }

}
