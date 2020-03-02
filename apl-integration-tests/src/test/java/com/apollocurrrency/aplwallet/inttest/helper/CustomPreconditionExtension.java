package com.apollocurrrency.aplwallet.inttest.helper;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.ws.rs.NotFoundException;

public class CustomPreconditionExtension implements BeforeAllCallback{
    private final String DEX = "TestDex";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        switch (context.getRequiredTestClass().getSimpleName()){
            case DEX:
                //TODO: Implement import
                break;
             default: throw new NotFoundException("Please implement logic for class: "+context.getRequiredTestClass().getSimpleName());
        }
    }


}
