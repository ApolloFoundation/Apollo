/*
 *  Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Common code for tests
 *
 * @author alukin@gmail.com
 */
public class TestsCommons {

    protected static final String TST_IN_DIR = "testdata/input/";
    protected static final String TST_OUT_DIR = "testdata/out/";

    protected static final String PLAIN_FILE_TEXT = "lorem_ipsum.bin";
    protected static final byte[] nonce1 = new byte[32]; //(0-31)
    protected static final byte[] nonce2 = new byte[32]; //(32-63)
    protected static final String secretPhraseA = "Red fox jumps over the Lazy dog";
    protected static final String secretPhraseB = "Red dog jumps over the Lazy fox";
    protected static byte[] plain_data;

    static {
        String inFile = TST_IN_DIR + PLAIN_FILE_TEXT;

        try {
            ByteBuffer pd = readFromFile(inFile);
            plain_data = pd.array();
            File directory = new File(TST_OUT_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            writeToFile(pd, TST_OUT_DIR + PLAIN_FILE_TEXT);
            for (Integer i = 0; i < 32; i++) {
                nonce1[i] = i.byteValue();
                nonce2[i] = new Integer(i + 32).byteValue();
            }
        } catch (IOException ex) {
            fail("Can not read input data file: " + inFile);
        }
    }

    public static void writeToFile(ByteBuffer data, String fileName) throws IOException {
        FileChannel out = new FileOutputStream(fileName).getChannel();
        data.rewind();
        out.write(data);
        out.close();
    }

    public static ByteBuffer readFromFile(String fileName) throws IOException {

        FileChannel fChan;
        Long fSize;
        ByteBuffer mBuf;
        fChan = new FileInputStream(fileName).getChannel();
        fSize = fChan.size();
        mBuf = ByteBuffer.allocate(fSize.intValue());
        fChan.read(mBuf);
        fChan.close();
        mBuf.rewind();
        return mBuf;
    }
}
