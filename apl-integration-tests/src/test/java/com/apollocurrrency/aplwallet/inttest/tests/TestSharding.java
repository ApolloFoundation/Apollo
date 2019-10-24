package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class TestSharding extends TestBaseNew {


    @Test
    void verifyShards(){
     List<String> peers = TestConfiguration.getTestConfiguration().getHosts();
        HashMap<String, List<ShardDTO>> shards = new HashMap<>();
        for (String ip: peers) {
                shards.put(ip, getShards(ip));
        }
        System.out.println(shards);
    }


}
