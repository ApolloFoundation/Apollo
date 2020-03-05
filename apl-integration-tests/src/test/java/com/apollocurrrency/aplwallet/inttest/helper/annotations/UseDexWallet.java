package com.apollocurrrency.aplwallet.inttest.helper.annotations;

import com.apollocurrrency.aplwallet.inttest.helper.providers.DexWalletProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ArgumentsSource(DexWalletProvider.class)
@ParameterizedTest
@Retention(RetentionPolicy.RUNTIME)
public @interface UseDexWallet  {
}
