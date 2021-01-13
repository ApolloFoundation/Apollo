/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.core;

import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;

import java.util.Objects;

public class FileUpdateAttachment {
    private OS OS;
    private Arch architecture;
    private String urlFirstPart;
    private String urlSecondPart;
    private String version;
    private String hash;
    private int level;

    public FileUpdateAttachment() {
    }

    public FileUpdateAttachment(OS OS, Arch architecture, String urlFirstPart, String urlSecondPart, String version, String hash, int level) {
        this.OS = OS;
        this.architecture = architecture;
        this.urlFirstPart = urlFirstPart;
        this.urlSecondPart = urlSecondPart;
        this.version = version;
        this.hash = hash;
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileUpdateAttachment)) return false;
        FileUpdateAttachment that = (FileUpdateAttachment) o;
        return level == that.level &&
            OS == that.OS &&
            architecture == that.architecture &&
            Objects.equals(urlFirstPart, that.urlFirstPart) &&
            Objects.equals(urlSecondPart, that.urlSecondPart) &&
            Objects.equals(version, that.version) &&
            Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(OS, architecture, urlFirstPart, urlSecondPart, version, hash, level);
    }

    public OS getOS() {
        return OS;
    }

    public void setOS(OS OS) {
        this.OS = OS;
    }

    public Arch getArchitecture() {
        return architecture;
    }

    public void setArchitecture(Arch architecture) {
        this.architecture = architecture;
    }

    public String getUrlFirstPart() {
        return urlFirstPart;
    }

    public void setUrlFirstPart(String urlFirstPart) {
        this.urlFirstPart = urlFirstPart;
    }

    public String getUrlSecondPart() {
        return urlSecondPart;
    }

    public void setUrlSecondPart(String urlSecondPart) {
        this.urlSecondPart = urlSecondPart;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

}
