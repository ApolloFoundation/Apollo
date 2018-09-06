/*
 * Copyright © 2018 Apollo Foundation
 */

package util;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TestClassLoader extends ClassLoader {

    @Test
    public void testLoadClass() throws Exception {
        Class<?> c1 = new TestClassLoader().loadClass("com.apollocurrency.aplwallet.apl.Apl");
        Class<?> c2 = new TestClassLoader().loadClass("com.apollocurrency.aplwallet.apl.Apl");
        System.out.println(c1);
        System.out.println(c2);
        Assert.assertFalse(c1 == c2);
    }

    private static final String rootDirName = "com.apollocurrency.aplwallet.apl";

    private static final Set<String> classNames = new HashSet<>();
    static {
        classNames.add(rootDirName + ".Apl");
        classNames.add(rootDirName + ".TransactionProcessor");
        classNames.add(rootDirName + ".CurrencyExchangeOffer");
        classNames.add(rootDirName + ".http.API");
        classNames.add(rootDirName + ".DebugTrace");
        classNames.add(rootDirName + ".http.APITag");
        classNames.add(rootDirName + ".http.EventWait");
        classNames.add(rootDirName + ".http.DGSListing");
        classNames.add(rootDirName + ".Constants");
        classNames.add(rootDirName + ".Currency");
        classNames.add(rootDirName + ".http.EventListener");
        classNames.add(rootDirName + ".Account");
        classNames.add(rootDirName + ".Genesis");
        classNames.add(rootDirName + ".peer.GetInfo");
        classNames.add(rootDirName + ".peer.PeerWebSocket");
        classNames.add(rootDirName + ".DigitalGoodsStore");
        classNames.add(rootDirName + ".http.GetConstants");
        classNames.add(rootDirName + ".http.SetLogging");
        classNames.add(rootDirName + ".peer.Peers");
        classNames.add(rootDirName + ".peer.GetNextBlocks");
        classNames.add(rootDirName + ".peer.PeerServlet");
        classNames.add(rootDirName + ".util.TrustAllSSLProvider");
        classNames.add(rootDirName + ".http.APITestServlet");
        classNames.add(rootDirName + ".util.Logger");
        classNames.add(rootDirName + ".http.JSONResponses");
        classNames.add(rootDirName + ".http.VerifyPrunableMessage");
        classNames.add(rootDirName + ".http.APIServlet");
        classNames.add(rootDirName + ".http.APIEnum");
        classNames.add(rootDirName + ".env.RuntimeEnvironment");
        classNames.add(rootDirName + ".http.APIProxy");
        classNames.add(rootDirName + ".addons.AddOns");
        classNames.add(rootDirName + ".TransactionScheduler");
        classNames.add(rootDirName + ".Poll");
        classNames.add(rootDirName + ".Shuffler");
        classNames.add(rootDirName + ".Shuffling");
        classNames.add(rootDirName + "db.TransactionalDb");

    }

    private boolean useCache;

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    private Map<String, Class> cachedClasses = new ConcurrentHashMap<>();
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!classNames.contains(name) && !name.startsWith(rootDirName)) {
            return super.loadClass(name);
        }
        if (useCache && cachedClasses.containsKey(name)) {
            return cachedClasses.get(name);
        }
        try(InputStream in = ClassLoader.getSystemResourceAsStream(name.replaceAll("\\.", "\\\\") + ".class")) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] byteArray = buffer.toByteArray();
            in.close();
            Class<?> loadedClass = defineClass(name, byteArray, 0, byteArray.length);
            cachedClasses.put(name, loadedClass);
            return loadedClass;
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot load class " + name, e);
        }
    }
}
