package com.apollocurrrency.aplwallet.inttest.helper.providers;

import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class WalletProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(TestConfiguration.getTestConfiguration().getStandartWallet(), TestConfiguration.getTestConfiguration().getVaultWallet()).map(Arguments::of);
    }
}
