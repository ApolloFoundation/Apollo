/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.vault;

public class UserKeyStoreDTO {
    private byte[] file;
    private String fileName;

    public UserKeyStoreDTO(byte[] file, String name) {
        this.file = file;
        this.fileName = name;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
