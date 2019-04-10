/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.tools.impl.UpdaterUrlUtils;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class RSADecryptionTest {

    @Test
    public void testSingleDecryption() throws Exception {
        PrintStream systemOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 1024);
        try {
            PrintStream printStream = new PrintStream(out);
            System.setOut(printStream);
            String[] params = new String[] {
 "c4e77135292a1d973dfcbb9baa533c5d2377fd1fd8943fbf37a53ac499d672a5adb24956a2605897ffd59f93faed1c3d71b6601db82d942b039bda3c99b974fd947b49cda197768a5eed5a31378b3257d8416a4f25bda9aa7ce5a6f1d4f0541cf40d760e913f4f981f6345f0126db9248636f5bee66ca530be41f7767eeb6605539abb984fedde76ff0f29cdf71677b6406870b937b9165697cf4fbb17294400830671b40b374fcbc0f44ba79d3dd44174e9fcb89daea845c12dff7dfa99bf919cab61a2c690c227d9bc3f98c84e495c07f794b3b49e5def106dfbba5bc1e15f015246ba0fc73fa6a4e17c531f827af8663af238c28964f5fe847c1e2848013d97dc0eb31159c0e542b1d5530f64e2ba4c588e28c85f44aeffe51d21f30c2462fed37d6b86052e171e64431dce6e04aa5a05a938f9e3330f40dec90c0ab091d0549714bd3c72b9c801e07d91c355129518506a5cbd0b432691a409316c6e8419371d4e1f942f958b6f89269e5c49cfeb23ffd7b1df3725efce69436ebdc0fa503fc9053f874f9379e579dca1fa89b59549b9a9f5dc4b07947457c3933d8d3cc8bc48a9be600dcd35d41f4edb072d5643dd2d323cbf5f65395bf0d4fe5bcdd26aaa0b935d40454482ca90610f94c8a807008b4cc93acf8b3f61fa2cd0363c2ae2f5af3d4abe3935a9deb70657bf449181d2b6eca77dbd9255c7041816e401f9f4"                   
                    ,"certs/1_2.crt"
                    ,"false"
            };

            UpdaterUrlUtils.decrypt(params[1], params[0], false);
            printStream.close();
            String consoleOutput = new String(out.toByteArray());
            assertTrue(consoleOutput.substring(consoleOutput.lastIndexOf("format:")).contains(Convert.toHexString("Secret message here!"
                    .getBytes())));
        }
        finally {
            System.setOut(systemOut);
            System.out.println(new String(out.toByteArray()));
            out.close();
        }
    }


    @Test
    public void testSplittedDecryption() throws Exception {
        PrintStream systemOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 1024);
        try {
            PrintStream printStream = new PrintStream(out);
            System.setOut(printStream);
            String[] params = new String[] {
                    "36c028bb6bb88b7c1c77e028c879d85312411db9ae47a9b0918a2cf5a9ec87a56b465d77c34019c221923753f16aa7ca45a2711094a8240b1a2ea3c5a75ce15c66a9d9e5e546e9739d3ec37331daacb7c822d303fa5736aba97813cc54130dcb75d569ce09741c1d3717773d59442f534707ba299ccc67126f538ea0225601bbd5cc48dbce7dff7e2d003e80f8d464d9fe0de8e309bfe539ad8f2d279382b840a01389d3752b51617a68f76e5116087d9b457f66f4bd300f4a21e204d655848979e635b71e13d72fa30a6a5e7e99591a81ba16e697f110c95524b0197675c2206d239ee95ba312f81a5d60cbfb40246b50574688a4c8247ddfc21a155a8bf4f97b8b0f3f2387074f1a3b9c3376d63f81009ba89facc6902bd47f9121133e6ae655ebbcc68686de31f7fda9f34682821d9d3e27f9ef710acbf2e33fad153b5f0fa1061fe91ffce2f5bef939fa4a05f2d7c4aea77228aff168440a5aac8cc5731ef1b3092ea22bce144b7ff84f3e47bc5afb015f088abacc1e6575f049b4a3aaef6029557c05413bf094e1f610357ac2cbdf3f6c45fa67588060ea4f1c39cd7353542e7cd34c8791edf7676a1b690c00f6c559417a9996820ba1fd1e57c40fadd3748e659e38c3b48d0310134811ac2a0ad148a6f29e15f2e9d991b7f3c703a4bd99adb14a323014633c59f08291c550631c3feecf9af8ea0a2490e4a0cf3376c1"
                    + ";" +
"aa22973200037d2108ececa9850ed6f07c099fd4363bfb08467e26e38140ee7b8002181d53a2373c93fa52662f68bc180ce3061ee4db4ff543957ce25573ae75b2da47c1ecae15b5dfdca17d9417c3cd3294051b3626a0a3b8930a6cec51395557beb051c0a9b2cdd3e4872e25065d79f1482fbd144a0adbc7785ffece971281bc72412b17c7f1f01d31c08a28344bb3c82f9d242c71aa7d5e4ac898b69f558ce75bb3ab8e48dfd69895453cda7c819d32dec50aca1470bc7b9329ac3d46165801c0f7bd474151cd2e57b016dfe1e02c67001400ab64612686f7c7ddc57a38c963b220f96b3d92e03cc85e9b908b74e6224bc1f2159dbf046ce61579d00b8541624e7e0e87605223fbd4e976e9e6205348e66223fa2eff99b0dbb774133534121ace3555175503de2cff0799f77ea80d2466b189925342ac4afb22df59a1e9efcfa569d75aa36910dc8e9e5bc262baa3c4508fda4c7b0fc728b7a6a811af5c026392ca2c6d80f8a1cfc91ab6c7646b45f84666b02571fe328b411330e7e2567251d384873cfa00152750b03cf23ab722a65e2081af40e858a2f183977ed6d4b7514f2dbe5d607b6dcbfbd553e17472111a4ca3cc2fd6ade9e58e2ddac97a05fec9cac6bd4b9a42e28094738505681f329ef534d1963320792106bed85710d8fa6260e102f03e296f559202e72a34b7394c19d6726abf44836ca0389a4472dbf4"
                    , "certs/2_2.crt"
                    , "false"
            };

            UpdaterUrlUtils.decrypt(params[1], params[0], false);
            printStream.close();
            String consoleOutput = new String(out.toByteArray());
            assertTrue(consoleOutput.substring(consoleOutput.lastIndexOf("format:")).contains("c4e77135292a1d973dfcbb9baa533c5d2377fd1fd8943fbf37a53ac499d672a5adb24956a2605897ffd59f93faed1c3d71b6601db82d942b039bda3c99b974fd947b49cda197768a5eed5a31378b3257d8416a4f25bda9aa7ce5a6f1d4f0541cf40d760e913f4f981f6345f0126db9248636f5bee66ca530be41f7767eeb6605539abb984fedde76ff0f29cdf71677b6406870b937b9165697cf4fbb17294400830671b40b374fcbc0f44ba79d3dd44174e9fcb89daea845c12dff7dfa99bf919cab61a2c690c227d9bc3f98c84e495c07f794b3b49e5def106dfbba5bc1e15f015246ba0fc73fa6a4e17c531f827af8663af238c28964f5fe847c1e2848013d97dc0eb31159c0e542b1d5530f64e2ba4c588e28c85f44aeffe51d21f30c2462fed37d6b86052e171e64431dce6e04aa5a05a938f9e3330f40dec90c0ab091d0549714bd3c72b9c801e07d91c355129518506a5cbd0b432691a409316c6e8419371d4e1f942f958b6f89269e5c49cfeb23ffd7b1df3725efce69436ebdc0fa503fc9053f874f9379e579dca1fa89b59549b9a9f5dc4b07947457c3933d8d3cc8bc48a9be600dcd35d41f4edb072d5643dd2d323cbf5f65395bf0d4fe5bcdd26aaa0b935d40454482ca90610f94c8a807008b4cc93acf8b3f61fa2cd0363c2ae2f5af3d4abe3935a9deb70657bf449181d2b6eca77dbd9255c7041816e401f9f4"));
        }
        finally {
            System.setOut(systemOut);
            System.out.println(new String(out.toByteArray()));
            out.close();
        }
    }

    @Test
    public void testDoubleDecryption() throws Exception {
        PrintStream systemOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 1024);
        try {
            PrintStream printStream = new PrintStream(out);
            System.setOut(printStream);

            String[] params = new String[] {
                    "36c028bb6bb88b7c1c77e028c879d85312411db9ae47a9b0918a2cf5a9ec87a56b465d77c34019c221923753f16aa7ca45a2711094a8240b1a2ea3c5a75ce15c66a9d9e5e546e9739d3ec37331daacb7c822d303fa5736aba97813cc54130dcb75d569ce09741c1d3717773d59442f534707ba299ccc67126f538ea0225601bbd5cc48dbce7dff7e2d003e80f8d464d9fe0de8e309bfe539ad8f2d279382b840a01389d3752b51617a68f76e5116087d9b457f66f4bd300f4a21e204d655848979e635b71e13d72fa30a6a5e7e99591a81ba16e697f110c95524b0197675c2206d239ee95ba312f81a5d60cbfb40246b50574688a4c8247ddfc21a155a8bf4f97b8b0f3f2387074f1a3b9c3376d63f81009ba89facc6902bd47f9121133e6ae655ebbcc68686de31f7fda9f34682821d9d3e27f9ef710acbf2e33fad153b5f0fa1061fe91ffce2f5bef939fa4a05f2d7c4aea77228aff168440a5aac8cc5731ef1b3092ea22bce144b7ff84f3e47bc5afb015f088abacc1e6575f049b4a3aaef6029557c05413bf094e1f610357ac2cbdf3f6c45fa67588060ea4f1c39cd7353542e7cd34c8791edf7676a1b690c00f6c559417a9996820ba1fd1e57c40fadd3748e659e38c3b48d0310134811ac2a0ad148a6f29e15f2e9d991b7f3c703a4bd99adb14a323014633c59f08291c550631c3feecf9af8ea0a2490e4a0cf3376c1"
                            + ";" +
                            "aa22973200037d2108ececa9850ed6f07c099fd4363bfb08467e26e38140ee7b8002181d53a2373c93fa52662f68bc180ce3061ee4db4ff543957ce25573ae75b2da47c1ecae15b5dfdca17d9417c3cd3294051b3626a0a3b8930a6cec51395557beb051c0a9b2cdd3e4872e25065d79f1482fbd144a0adbc7785ffece971281bc72412b17c7f1f01d31c08a28344bb3c82f9d242c71aa7d5e4ac898b69f558ce75bb3ab8e48dfd69895453cda7c819d32dec50aca1470bc7b9329ac3d46165801c0f7bd474151cd2e57b016dfe1e02c67001400ab64612686f7c7ddc57a38c963b220f96b3d92e03cc85e9b908b74e6224bc1f2159dbf046ce61579d00b8541624e7e0e87605223fbd4e976e9e6205348e66223fa2eff99b0dbb774133534121ace3555175503de2cff0799f77ea80d2466b189925342ac4afb22df59a1e9efcfa569d75aa36910dc8e9e5bc262baa3c4508fda4c7b0fc728b7a6a811af5c026392ca2c6d80f8a1cfc91ab6c7646b45f84666b02571fe328b411330e7e2567251d384873cfa00152750b03cf23ab722a65e2081af40e858a2f183977ed6d4b7514f2dbe5d607b6dcbfbd553e17472111a4ca3cc2fd6ade9e58e2ddac97a05fec9cac6bd4b9a42e28094738505681f329ef534d1963320792106bed85710d8fa6260e102f03e296f559202e72a34b7394c19d6726abf44836ca0389a4472dbf4"
                    , "certs/2_2.crt;certs/1_2.crt"
                    , "false"
            };

            UpdaterUrlUtils.decrypt(params[1], params[0], false);
            printStream.close();
            String consoleOutput = new String(out.toByteArray());
            assertTrue(consoleOutput.substring(consoleOutput.lastIndexOf("format:")).contains(Convert.toHexString("Secret message here!"
                    .getBytes())));
        }
        finally {
            System.setOut(systemOut);
            System.out.println(new String(out.toByteArray()));
            out.close();
        }
    }
}
