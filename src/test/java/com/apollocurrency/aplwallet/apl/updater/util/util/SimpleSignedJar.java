/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.util;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generator of signed Jars. It stores some data in memory therefore it is not suited for creation of large files. The
 * usage:
 * <pre>
 * KeyStore keystore = KeyStore.getInstance("JKS");
 * keyStore.load(keystoreStream, "keystorePassword");
 * SimpleSignedJar jar = new SimpleSignedJar(out, keyStore, "keyAlias", "keyPassword");
 * signedJar.addManifestAttribute("Main-Class", "com.example.MainClass");
 * signedJar.addManifestAttribute("Application-Name", "Example");
 * signedJar.addManifestAttribute("Permissions", "all-permissions");
 * signedJar.addManifestAttribute("Codebase", "*");
 * signedJar.addFileContents("com/example/MainClass.class", clsData);
 * signedJar.addFileContents("JNLP-INF/APPLICATION.JNLP", generateJnlpContents());
 * signedJar.close();
 * </pre>
 *
 * @see <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html#Signed_JAR_File">JAR format
 *      specification</a>
 */
public class SimpleSignedJar extends SimpleJar {
    private static final String MANIFEST_FN = "META-INF/MANIFEST.MF";
    private static final String SIG_FN = "META-INF/SIGNUMO.SF";
    private static final String SIG_RSA_FN = "META-INF/SIGNUMO.RSA";

    private final KeyStore keyStore;
    private final String keyAlias;
    private final String password;

    private final HashFunction hashFunction;
    private final String hashFunctionName;

    private String manifestHash;
    private String manifestMainHash;
    private final Map<String, String> fileDigests;
    private final Map<String, String> sectionDigests;

    /**
     * Constructor.
     *
     * @param out         the output stream to write JAR data to
     * @param keyStore    the key store to load given key from
     * @param keyAlias    the name of the key in the store, this key is used to sign the JAR
     * @param keyPassword the password to access the key
     */
    public SimpleSignedJar(OutputStream out, KeyStore keyStore,  String keyAlias, String keyPassword) {
        super(out);
        this.keyStore = checkNotNull(keyStore, "keyStore");
        this.keyAlias = checkNotNull(keyAlias, "keyAlias");
        this.password = checkNotNull(keyPassword, "keyPassword");

        this.fileDigests = Maps.newLinkedHashMap();
        this.sectionDigests = Maps.newLinkedHashMap();

        this.hashFunction = Hashing.sha256();
        this.hashFunctionName = "SHA-256";
    }

    /**
     * Adds a header to the manifest of the JAR.
     *
     * @param name  name of the attribute, it is placed into the main section of the manifest file, it cannot be longer
     *              than {@value #MANIFEST_ATTR_MAX_LEN} bytes (in utf-8 encoding)
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
        super.addFileContents(filename, contents);

        HashCode hashCode = hashFunction.hashBytes(contents);

        String hashCode64 = BaseEncoding.base64().encode(hashCode.asBytes());
        fileDigests.put(filename, hashCode64);
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
        byte sig[] = writeSigFile();
        writeSignature(sig);
        zos.finish();
    }


    /** Creates the beast that can actually sign the data. */
    private CMSSignedDataGenerator createSignedDataGenerator() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        List<Certificate> certChain = Lists.newArrayList(keyStore.getCertificateChain(keyAlias));
        Store certStore = new JcaCertStore(certChain);
        Certificate cert = keyStore.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, password.toCharArray());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WITHRSA").setProvider("BC").build(privateKey);
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        DigestCalculatorProvider dcp = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
        SignerInfoGenerator sig = new JcaSignerInfoGeneratorBuilder(dcp).build(signer, (X509Certificate) cert);
        generator.addSignerInfoGenerator(sig);
        generator.addCertificates(certStore);
        return generator;
    }

    /** Returns the CMS signed data. */
    private byte[] signSigFile(byte[] sigContents) throws Exception {
        CMSSignedDataGenerator gen = createSignedDataGenerator();
        CMSTypedData cmsData = new CMSProcessableByteArray(sigContents);
        CMSSignedData signedData = gen.generate(cmsData, true);
        return signedData.getEncoded();
    }

    /**
     * Signs the .SIG file and writes the signature (.RSA file) to the JAR.
     *
     * @throws java.io.IOException
     * @throws RuntimeException    if the signing failed
     */
    private void writeSignature(byte[] sigFile) throws IOException {
        zos.putNextEntry(new ZipEntry(SIG_RSA_FN));
        try {
            byte[] signature = signSigFile(sigFile);
            zos.write(signature);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Signing failed.", e);
        }
        zos.closeEntry();
    }

    /**
     * Writes the .SIG file to the JAR.
     *
     * @return the contents of the file as bytes
     */
    private byte[] writeSigFile() throws IOException {
        zos.putNextEntry(new ZipEntry(SIG_FN));
        Manifest man = new Manifest();
        // main section
        Attributes mainAttributes = man.getMainAttributes();
        mainAttributes.put(Attributes.Name.SIGNATURE_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Created-By"), CREATED_BY);
        mainAttributes.put(new Attributes.Name(hashFunctionName + "-Digest-Manifest"), manifestHash);
        mainAttributes.put(new Attributes.Name(hashFunctionName + "-Digest-Manifest-Main-Attributes"), manifestMainHash);

        // individual files sections
        Attributes.Name digestAttr = new Attributes.Name(hashFunctionName + "-Digest");
        for (Map.Entry<String, String> entry : sectionDigests.entrySet()) {
            Attributes attributes = new Attributes();
            man.getEntries().put(entry.getKey(), attributes);
            attributes.put(digestAttr, entry.getValue());
        }

        man.write(zos);
        zos.closeEntry();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        man.write(baos);
        return baos.toByteArray();
    }

    /** Helper for {@link #writeManifest()} that creates the digest of one entry. */
    private String hashEntrySection(String name, Attributes attributes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        manifest.write(o);
        int emptyLen = o.toByteArray().length;

        manifest.getEntries().put(name, attributes);

        manifest.write(o);
        byte[] ob = o.toByteArray();
        ob = Arrays.copyOfRange(ob, emptyLen, ob.length);
        return BaseEncoding.base64().encode(hashFunction.hashBytes(ob).asBytes());
    }

    /** Helper for {@link #writeManifest()} that creates the digest of the main section. */
    private String hashMainSection(Attributes attributes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putAll(attributes);
        Hasher hasher = hashFunction.newHasher();
        SimpleSignedJar.HashingOutputStream o = new SimpleSignedJar.HashingOutputStream(ByteStreams.nullOutputStream(), hasher);
        manifest.write(o);
        return BaseEncoding.base64().encode(hasher.hash().asBytes());
    }

    /**
     * Writes the manifest to the JAR. It also calculates the digests that are required to be placed in the the signature
     * file.
     *
     * @throws java.io.IOException
     */
    @Override
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

        // individual files sections
        Attributes.Name digestAttr = new Attributes.Name(hashFunctionName + "-Digest");
        for (Map.Entry<String, String> entry : fileDigests.entrySet()) {
            Attributes attributes = new Attributes();
            man.getEntries().put(entry.getKey(), attributes);
            attributes.put(digestAttr, entry.getValue());
            sectionDigests.put(entry.getKey(), hashEntrySection(entry.getKey(), attributes));
        }

        Hasher hasher = hashFunction.newHasher();
        OutputStream out = new SimpleSignedJar.HashingOutputStream(zos, hasher);
        man.write(out);
        zos.closeEntry();

        manifestHash = BaseEncoding.base64().encode(hasher.hash().asBytes());
        manifestMainHash = hashMainSection(man.getMainAttributes());
    }

    /** Helper output stream that also sends the data to the given {@link com.google.common.hash.Hasher}. */
    private static class HashingOutputStream extends OutputStream {
        private final OutputStream out;
        private final Hasher hasher;

        public HashingOutputStream(OutputStream out, Hasher hasher) {
            this.out = out;
            this.hasher = hasher;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            hasher.putByte((byte) b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b);
            hasher.putBytes(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            hasher.putBytes(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }
}