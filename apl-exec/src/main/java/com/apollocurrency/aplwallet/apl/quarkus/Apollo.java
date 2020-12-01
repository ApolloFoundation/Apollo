/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.quarkus;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

/**
 * Quarkus Apollo startup class
 */
@QuarkusMain
public class Apollo implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        if (args.length == 0) {
            Quarkus.waitForExit();
            return 0;
        }
        return new CommandLine(new ApolloCommandLine(args))
            .execute(args);
    }
    public static void main(String... args) {
        Quarkus.run(Apollo.class, args);
    }

}
