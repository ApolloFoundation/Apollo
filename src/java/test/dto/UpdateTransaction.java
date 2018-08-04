/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package test.dto;

import apl.Version;
import apl.updater.Architecture;
import apl.updater.Platform;
import apl.util.Convert;

import java.util.Arrays;
import java.util.Objects;

public class UpdateTransaction extends Transaction {

    private UpdateAttachment attachment;

    public UpdateAttachment getAttachment() {
        return attachment;
    }

    public void setAttachment(UpdateAttachment attachment) {
        this.attachment = attachment;
    }

    public UpdateTransaction() {
    }

    public UpdateTransaction(UpdateAttachment attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {

        return "UpdateTransaction{" +
                "attachment=" + attachment +
                "transactionData=" + super.toString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateTransaction)) return false;
        if (!super.equals(o)) return false;
        UpdateTransaction that = (UpdateTransaction) o;
        return Objects.equals(attachment, that.attachment);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), attachment);
    }

    public static class UpdateAttachment {
        private Platform platform;
        private Architecture architecture;
        private String url;
        private Version version;
        private byte[] hash;
        private byte[] signature;

        public Platform getPlatform() {
            return platform;
        }

        public void setPlatform(Platform platform) {
            this.platform = platform;
        }

        public Architecture getArchitecture() {
            return architecture;
        }

        public void setArchitecture(Architecture architecture) {
            this.architecture = architecture;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Version getVersion() {
            return version;
        }

        public void setVersion(Version version) {
            this.version = version;
        }

        public byte[] getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = Convert.parseHexString(hash);
        }

        public byte[] getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = Convert.parseHexString(signature);
        }

        @Override
        public String toString() {
            return "UpdateAttachment{" +
                    "platform=" + platform +
                    ", architecture=" + architecture +
                    ", url='" + url + '\'' +
                    ", version=" + version +
                    ", hash=" + Arrays.toString(hash) +
                    ", signature=" + Arrays.toString(signature) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UpdateAttachment)) return false;
            UpdateAttachment that = (UpdateAttachment) o;
            return platform == that.platform &&
                    architecture == that.architecture &&
                    Objects.equals(url, that.url) &&
                    Objects.equals(version, that.version) &&
                    Arrays.equals(hash, that.hash) &&
                    Arrays.equals(signature, that.signature);
        }

        @Override
        public int hashCode() {

            int result = Objects.hash(platform, architecture, url, version);
            result = 31 * result + Arrays.hashCode(hash);
            result = 31 * result + Arrays.hashCode(signature);
            return result;
        }

        public UpdateAttachment() {
        }

        public UpdateAttachment(Platform platform, Architecture architecture, String url, Version version, byte[] hash, byte[] signature) {
            this.platform = platform;
            this.architecture = architecture;
            this.url = url;
            this.version = version;
            this.hash = hash;
            this.signature = signature;
        }
    }
}
