package com.apollocurrrency.aplwallet.inttest.helper.providers;

import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PollArgumentProvider implements ArgumentsProvider {
    private final int POLL_BY_ACCOUNT = 0;
    private final int POLL_BY_ACCOUNT_BALANCE = 1;
    private final int POLL_BY_ASSET = 2;
    private final int POLL_BY_CURRENCY = 3;

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        List<Integer> types = Arrays.asList(POLL_BY_ACCOUNT,POLL_BY_ACCOUNT_BALANCE,POLL_BY_ASSET,POLL_BY_CURRENCY);
        List<Wallet> wallets = Arrays.asList(
            TestConfiguration.getTestConfiguration().getStandartWallet(),
            TestConfiguration.getTestConfiguration().getVaultWallet());
        List<Arguments> arguments = new ArrayList<>();
        types.forEach(type-> wallets.forEach(wallet ->arguments.add(Arguments.of(type, wallet))));
        return arguments.stream();
    }
}
