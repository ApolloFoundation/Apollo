/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package dto;

import com.apollocurrency.aplwallet.apl.Version;
import com.apollocurrency.aplwallet.apl.updater.Architecture;
import com.apollocurrency.aplwallet.apl.updater.DoubleByteArrayTuple;
import com.apollocurrency.aplwallet.apl.updater.Platform;
import com.apollocurrency.aplwallet.apl.util.Convert;

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

    public UpdateTransaction() {}

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

    public static class UpdateAttachment {
        private Platform platform;
        private Architecture architecture;
        private DoubleByteArrayTuple url;
        private Version version;
        private byte[] hash;

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

        public DoubleByteArrayTuple getUrl() {
            return url;
        }

        public void setUrl(DoubleByteArrayTuple url) {
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

        public UpdateAttachment(Platform platform, Architecture architecture, DoubleByteArrayTuple url, Version version, byte[] hash) {
            this.platform = platform;
            this.architecture = architecture;
            this.url = url;
            this.version = version;
            this.hash = hash;
        }

        public UpdateAttachment() {
        }

        @Override
        public String toString() {
            return "UpdateAttachment{" +
                    "platform=" + platform +
                    ", architecture=" + architecture +
                    ", url=" + url +
                    ", version=" + version +
                    ", hash=" + Arrays.toString(hash) +
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
                    Arrays.equals(hash, that.hash);
        }

        @Override
        public int hashCode() {

            int result = Objects.hash(platform, architecture, url, version);
            result = 31 * result + Arrays.hashCode(hash);
            return result;
        }
    }
}
