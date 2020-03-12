package com.apollocurrrency.aplwallet.inttest.model.steps;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class Steps {
    public final AccountSteps ACCOUNT_STEPS = new AccountSteps();
    public final AliasSteps ALIAS_STEPS = new AliasSteps();
    public final AssertSteps ASSERT_STEPS = new AssertSteps();
    public final AssetSteps ASSET_STEPS = new AssetSteps();
    public final BlockSteps BLOCK_STEPS = new BlockSteps();
    public final CurrenciesSteps CURRENCIES_STEPS = new CurrenciesSteps();
    public final DexSteps DEX_STEPS = new DexSteps();
    public final MarketplaceSteps MARKETPLACE_STEPS = new MarketplaceSteps();
    public final MessagesSteps MESSAGES_STEPS = new MessagesSteps();
    public final NetworkingSteps NETWORKING_STEPS = new NetworkingSteps();
    public final PollSteps POLL_STEPS = new PollSteps();
    public final ShardingSteps SHARDING_STEPS = new ShardingSteps();
    public final ShufflingSteps SHUFFLING_STEPS = new ShufflingSteps();
    public final TaggedDataSteps TAGGED_DATA_STEPS = new TaggedDataSteps();

    private Map<String,Object> stepsContext = new HashMap<>();

}
