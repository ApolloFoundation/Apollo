/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.ApolloX509Info;
import com.apollocurrency.aplwallet.api.dto.GeneratorInfo;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alukin@gmail.com
 */
@Singleton
public class ServerInfoService {
    public ApolloX509Info getX509Info(){
        ApolloX509Info res = new ApolloX509Info();
        res.id = "No ID yet available";
        return res;
    }

    public List<GeneratorInfo> getActiveForgers(boolean showBallances) {
        List<GeneratorInfo> res = new ArrayList<>();
        List<Generator> forgers = Generator.getSortedForgers();
        for (Generator g : forgers) {
            GeneratorInfo gi = new GeneratorInfo();
            gi.setAccount(Convert.defaultRsAccount(g.getAccountId()));
            gi.setDeadline(g.getDeadline());
            gi.setHitTime(g.getHitTime());
            if (showBallances) {
                gi.setEffectiveBalanceAPL(0L);
            }
        }
        return res;
    }
}
