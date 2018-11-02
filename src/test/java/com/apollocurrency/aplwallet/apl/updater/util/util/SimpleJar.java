/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.util;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SimpleJar {
    protected static final String CREATED_BY = "APOLLO";
    protected static final String MANIFEST_FN = "META-INF/MANIFEST.MF";
    protected static final int MANIFEST_ATTR_MAX_LEN = 70;
    protected final Map<String, String> manifestAttributes;
    protected ZipOutputStream zos;


    public SimpleJar(OutputStream os) {
        this.zos = new ZipOutputStream(os);
        manifestAttributes = Maps.newLinkedHashMap();
    }
    /**
     * Adds a header to the manifest of the JAR.
     *
     * @param name  name of the attribute, it is placed into the main section of the manifest file, it cannot be longer
     *              than  bytes (in utf-8 encoding)
     * @param value value of the attribute
     */
    public void addManifestAttribute(String name, String value) {
        checkNotNull(name, "name");
        checkArgument(name.getBytes(Charsets.UTF_8).length <= MANIFEST_ATTR_MAX_LEN, "attribute name too long");
        checkNotNull(value, "value");
        manifestAttributes.put(name, value);
    }

    /**
     * Adds a file to the JAR. The file is immediately added to the zipped output stream. This method cannot be called once
     * the stream is closed.
     *
     * @param filename name of the file to add (use forward slash as a path separator)
     * @param contents contents of the file
     * @throws java.io.IOException
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public void addFileContents(String filename, byte[] contents) throws IOException {
        checkNotNull(filename, "filename");
        checkNotNull(contents, "contents");
        zos.putNextEntry(new ZipEntry(filename));
        zos.write(contents);
        zos.closeEntry();

    }

    public void addDirectory(String dirname) throws IOException {
        checkNotNull(dirname, "dirname");
        zos.putNextEntry(new ZipEntry(dirname + "/"));
        zos.closeEntry();
    }


    /**
     * Finishes the JAR file by writing the manifest and signature data to it and finishing the ZIP entries. It leaves the
     * underlying stream open.
     *
     * @throws java.io.IOException
     * @throws RuntimeException    if the signing goes wrong
     */
    public void finish() throws IOException {
        writeManifest();
        zos.finish();
    }

    /**
     * Closes the JAR file by writing the manifest and signature data to it and finishing the ZIP entries. It closes the
     * underlying stream.
     *
     * @throws java.io.IOException
     * @throws RuntimeException    if the signing goes wrong
     */
    public void close() throws IOException {
        finish();
        zos.close();
    }
    /**
     * Writes the manifest to the JAR. It also calculates the digests that are required to be placed in the the signature
     * file.
     *
     * @throws java.io.IOException
     */
    protected void writeManifest() throws IOException {
        zos.putNextEntry(new ZipEntry(MANIFEST_FN));
        Manifest man = new Manifest();

        // main section
        Attributes mainAttributes = man.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Created-By"), CREATED_BY);

        for (Map.Entry<String, String> entry : manifestAttributes.entrySet()) {
            mainAttributes.put(new Attributes.Name(entry.getKey()), entry.getValue());
        }
        man.write(zos);
        zos.closeEntry();
    }
}
