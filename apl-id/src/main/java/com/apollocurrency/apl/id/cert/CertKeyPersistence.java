package com.apollocurrency.apl.id.cert;

import io.firstbridge.cryptolib.KeyReader;
import io.firstbridge.cryptolib.impl.KeyReaderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

/**
 * Handler of certificate, ExtCSR and key names and directories placed in map with ID as a key
 *
 * @author alukin@gmail.com
 */
public class CertKeyPersistence {
    public static final String PVT_SEARCH_PATH = "private/";
    private static final Logger log = LoggerFactory.getLogger(CertKeyPersistence.class);
    private final Map<BigInteger, List<ExtCert>> certMap = new TreeMap<>();
    public static final String[] sfxes = {"_pvtkey", "_req", "_cert", "_selfcert", "_csr"}; 
    
    public static String rmSuffixes(String fn) {
        String name = new String(fn);
        String ext = "";
        int last_dot = fn.lastIndexOf(".");
        if (last_dot >= 0) {
            ext = fn.substring(last_dot + 1);
            name = fn.substring(0, last_dot);
        }

        String[] sfxes = {"_pvtkey", "_req", "_cert", "_selfcert", "_csr"};
        for (String s : sfxes) {
            int idx = name.indexOf(s);
            if (idx >= 0) {
                name = name.substring(0, idx);
            }
        }
        return name;
    }

    public static String pvtKeyFileName(String fn) {
        String suffix = "_pvtkey";
        String name = rmSuffixes(fn);
        return name + suffix + ".pem";
    }

    public static String selfSignedFileName(String fn) {
        String suffix = "_selfcert";
        String name = rmSuffixes(fn);
        return name + suffix + ".pem";
    }

    public static String certFileName(String fn) {
        String suffix = "_cert";
        String name = rmSuffixes(fn);
        return name + suffix + ".pem";
    }

    public static String csrFileName(String fn) {
        String suffix = "_csr";
        String name = rmSuffixes(fn);
        return name + suffix + ".pem";
    }

    public List<ExtCert> getCert(BigInteger id) {
        return certMap.get(id);
    }

    public void put(BigInteger id, ExtCert cert) {
        List<ExtCert> cl = certMap.get(id);
        if (cl == null) {
            cl = new ArrayList<>();
        }
        cl.add(cert);
        certMap.put(id, cl);
    }

    public int size() {
        return certMap.size();
    }

    public void readCertDirectory(String path, boolean loadPrivateKey) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            File[] filesList = dir.listFiles();
            for (File f : filesList) {
                if (f.isFile() && f.canRead()) {
                    ExtCert ac = null;
                    try {
                        ac = loadPEMFromPath(f.toPath());
                    } catch (IOException ex) {
                        //impossible here
                    } catch (CertException ex) {
                        log.error("Certificate load exception wilr loading " + f.getAbsolutePath(), ex);
                    }
                    if (ac != null) {
                        put(ac.getActorId(), ac);
                        if (loadPrivateKey) {
                            String parent = f.getParent();
                            String fn = f.getName();
                            PrivateKey pk = null;
                            pk = readPvtKey(parent + "/" + pvtKeyFileName(fn));
                            if (pk == null) {//no key in the same dir, try "private/"
                                pk = readPvtKey(parent + PVT_SEARCH_PATH + pvtKeyFileName(fn));
                            }
                            if (pk != null) {
                                if (ac.checkKeys(pk)) {
                                    ac.setPrivateKey(pk);
                                } else {
                                    log.error("Private key file does not correspond to certificate: {}" + f.getAbsolutePath());
                                }
                            } else {
                                log.error("Private key file not foud for certificate: {}", f.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } else {
            log.error("Can not read certificates.Directory: {} does not exist!", path);
        }
    }

    public PrivateKey readPvtKey(String filePath) {
        KeyReader kr = new KeyReaderImpl();
        PrivateKey res = null;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            res = kr.readPrivateKeyPEM(fis);
        } catch (IOException ex) {
            log.trace("Can not read private key: {}", filePath);
        }
        return res;
    }
  
    public static ExtCert loadPEMFromPath(Path path) throws CertException, IOException {
        ExtCert res = null;
        try (FileInputStream fis = new FileInputStream(path.toString())) {
            res = loadPEMFromStream(fis);
        }
        return res;
    }

    public static ExtCert loadPEMFromStream(InputStream is) throws IOException, CertException {
        KeyReader kr = new KeyReaderImpl();
        X509Certificate cert = kr.readX509CertPEMorDER(is);
        ExtCert ac = new ExtCert(cert);
        //try to load corresponding key
        return ac;
    } 

    public static ExtCSR loadCSR(String path) {
        PKCS10CertificationRequest cr;
        ExtCSR res = null;
        try (FileReader fr = new FileReader(path)) {
            PEMParser parser = new PEMParser(fr);
            cr = (PKCS10CertificationRequest) parser.readObject();
            res = ExtCSR.fromPKCS10(cr);
        } catch (IOException ex) {
            log.error("Can not read PKCS#10 file: " + path, ex);
        }
        return res;
    }    
}
