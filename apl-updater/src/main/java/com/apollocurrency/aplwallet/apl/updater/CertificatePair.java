/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.security.cert.Certificate;
import java.util.Objects;

public class CertificatePair {
    private Certificate firstCertificate;
    private Certificate secondCertificate;

    public CertificatePair() {
    }

    public CertificatePair(Certificate firstCertificate, Certificate secondCertificate) {
        this.firstCertificate = firstCertificate;
        this.secondCertificate = secondCertificate;
    }

    @Override
    public String toString() {
        return "CertificatePair{" +
            "firstCertificate=" + UpdaterUtil.getStringRepresentation(firstCertificate) +
            ", secondCertificate=" + UpdaterUtil.getStringRepresentation(secondCertificate) +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CertificatePair)) return false;
        CertificatePair that = (CertificatePair) o;
        return Objects.equals(firstCertificate, that.firstCertificate) &&
            Objects.equals(secondCertificate, that.secondCertificate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(firstCertificate, secondCertificate);
    }

    public Certificate getFirstCertificate() {
        return firstCertificate;
    }

    public void setFirstCertificate(Certificate firstCertificate) {
        this.firstCertificate = firstCertificate;
    }

    public Certificate getSecondCertificate() {
        return secondCertificate;
    }

    public void setSecondCertificate(Certificate secondCertificate) {
        this.secondCertificate = secondCertificate;
    }
}
