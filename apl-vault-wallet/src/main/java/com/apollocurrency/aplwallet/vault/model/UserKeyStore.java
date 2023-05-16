package com.apollocurrency.aplwallet.vault.model;

public class UserKeyStore {
    private byte[] file;
    private String fileName;

    public UserKeyStore(byte[] file, String name) {
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
