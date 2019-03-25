package com.apollocurrency.aplwallet.apl.util.cert;

import io.firstbridge.cryptolib.KeyReader;
import io.firstbridge.cryptolib.impl.KeyReaderImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holder of available cerificates placed in map with ID as a key
 *
 * @author alukin@gmail.com
 */
public class CertificateHolder {
    public static final String PVT_SEARCH_PATH="/../private/";   
    private final Map<BigInteger, List<ApolloCertificate>> certMap = new TreeMap<>();

    private static final Logger log = LoggerFactory.getLogger(CertificateHolder.class);

    public List<ApolloCertificate> getCert(BigInteger id) {
        return certMap.get(id);
    }

    public void put(BigInteger id, ApolloCertificate cert) {
        List<ApolloCertificate> cl = certMap.get(id);
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
        CertificateLoader cl = new CertificateLoader();
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            File[] filesList = dir.listFiles();
            for (File f : filesList) {
                if (f.isFile() && f.canRead()) {
                    ApolloCertificate vc = cl.loadCertificate(f.getAbsolutePath());
                    if (vc != null) {
                        put(vc.getApolloId(), vc);
                        if (loadPrivateKey) {
                            String parent = f.getParent();
                            String fn = f.getName();
                            PrivateKey pk = null;
                            pk = readPvtKey(parent + "/" + pvtKeyFileName(fn));
                            if (pk == null) {//no key in the same dir, try ../private
                                pk = readPvtKey(parent + PVT_SEARCH_PATH + pvtKeyFileName(fn));
                            }
                            if (pk != null) {
                               if(vc.checkKeys(pk)){
                                 vc.setPrivateKey(pk);
                               }else{
                                   log.error("Private key file does not correspond to certificate: {}"+f.getAbsolutePath());
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
        try {
            res = kr.readPrivateKeyPEM(new FileInputStream(filePath));
        } catch (IOException ex) {
            log.trace("Can not read private key: {}", filePath);
        }
        return res;
    }

    public static String rmSuffixes(String fn) {
        String name = new String(fn);
        String ext="";
        int last_dot = fn.lastIndexOf(".");
        if(last_dot>=0){
            ext=fn.substring(last_dot+1);
            name=fn.substring(0, last_dot);
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
}
