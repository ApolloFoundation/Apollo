/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import org.json.simple.JSONObject;

public class ExportKeyStore {

    private byte[] file;
    private String fileName;

    public ExportKeyStore(byte[] file, String name) {
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

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("file", this.file);
        jsonObject.put("fileName", this.fileName);

        return jsonObject;
    }
}
