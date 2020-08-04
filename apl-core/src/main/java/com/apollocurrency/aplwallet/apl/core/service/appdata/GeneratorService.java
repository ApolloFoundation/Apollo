package com.apollocurrency.aplwallet.apl.core.service.appdata;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.GeneratorMemoryEntity;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;

public interface GeneratorService {

    GeneratorMemoryEntity startForging(byte[] keySeed);

    GeneratorMemoryEntity stopForging(byte[] keySeed);

    int stopForging();

    GeneratorMemoryEntity getGenerator(long id);

    int getGeneratorCount();

    Map<Long, GeneratorMemoryEntity> getGeneratorsMap();

    Collection<GeneratorMemoryEntity> getAllGenerators();

    List<GeneratorMemoryEntity> getSortedForgers();

    long getNextHitTime(long lastBlockId, int curTime);

    void setDelay(int delay);

    boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, int timestamp);

    boolean verifyHit(GeneratorMemoryEntity generator, Block previousBlock, int timestamp);

    void setLastBlock(Block lastBlock, GeneratorMemoryEntity generator);

    BigInteger getHit(byte[] publicKey, Block block);

    long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block);

    void suspendForging();

    void resumeForging();

    boolean forge(Block lastBlock, int generationLimit, GeneratorMemoryEntity generator) throws BlockchainProcessor.BlockNotAcceptedException;

}
