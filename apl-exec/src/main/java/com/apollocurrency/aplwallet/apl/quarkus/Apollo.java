/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.quarkus;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

import javax.inject.Inject;

/**
 * Quarkus Apollo startup class
 */
@QuarkusMain
public class Apollo implements QuarkusApplication {

    @Inject
    CommandLineParamMap paramMap;
    @Inject
    CommandLine.IFactory factory;

    public static void main(String... args) {
        System.out.println("Running Main.main(). Args:");
        for( String a: args){
            System.out.println(a);
        }
        // Can not start from IDE with real command line, Quarkus IDE launcher is craziy and buggy
        // Use terminal and say
        // mvn compile quarkus:dev -Dquarkus.args="-n=ZZZ"

        Quarkus.run(Apollo.class, args);
    }

    @Override
    public int run(String... args) {
        System.out.println("Running Main.run()");

        paramMap.getCliParams().put("Before Start", "blah-blah");

        CmdLineArgs cmdLine = new CmdLineArgs();
        int result = new CommandLine(cmdLine, factory).execute(args);

        System.out.println("Command line parsed. Result:"+result+" Entering Quarkus loop");
        System.out.println("Use URL http://localhost:8080/cli");

//        CmdLineArgs.printFields(cmdLine);
        paramMap.put("chainId", cmdLine.chainId);
        System.out.println("chainId" + cmdLine.chainId);

        if(!cmdLine.exit){
            Quarkus.waitForExit();
        }

        System.out.println("Quarkus loop exited");
        return 0;
    }
}
