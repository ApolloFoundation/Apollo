/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import java.util.Objects;

public class Version implements Comparable<Version> {
    private final int majorVersion;
    private final int intermediateVersion;
    private final int minorVersion;

    public Version(int majorVersion, int intermediateVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.intermediateVersion = intermediateVersion;
        this.minorVersion = minorVersion;
    }

    public Version(String versionString) {
        Objects.requireNonNull(versionString);
        if (!versionString.matches("\\d+\\.\\d+\\.\\d+")) {
            throw new RuntimeException("Incorrect versionString :  " + versionString);
        }
        String[] versionNumbers = versionString.split("\\.");
        majorVersion = (Integer.parseInt(versionNumbers[0]));
        intermediateVersion = Integer.parseInt(versionNumbers[1]);
        minorVersion = (Integer.parseInt(versionNumbers[2]));
    }
    @Override
    public String toString() {
        return majorVersion +"."+ intermediateVersion + "." + minorVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version)) return false;
        Version version = (Version) o;
        return majorVersion == version.majorVersion &&
                intermediateVersion == version.intermediateVersion &&
                minorVersion == version.minorVersion;
    }

    @Override
    public int hashCode() {

        return Objects.hash(majorVersion, intermediateVersion, minorVersion);
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getIntermediateVersion() {
        return intermediateVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    @Override
    public int compareTo(Version v) {
        int majorVersionCompare = Integer.compare(majorVersion, v.getMajorVersion());
        if (majorVersionCompare != 0 )
            return majorVersionCompare;
        int intermediateVersionCompare = Integer.compare(intermediateVersion, v.getIntermediateVersion());
        if (intermediateVersionCompare != 0 )
            return intermediateVersionCompare;
        return Integer.compare(minorVersion, v.getMinorVersion());
    }

    public boolean greaterThan(Version v) {
        return compareTo(v) > 0;
    }

    public Version incrementVersion() {
        return new Version(getMajorVersion(), getIntermediateVersion(), getMinorVersion() + 1);
    }
    public boolean lessThan(Version v) {
        return compareTo(v) < 0;
    }
}
