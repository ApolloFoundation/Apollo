/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.hash;

import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.Arrays;

public class Node {
    private byte[] value;

    public Node(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Arrays.equals(value, node.value);
    }

    @Override
    public String toString() {
        return "Node{" +
                "value=" + Convert.toHexString(value) +
                '}';
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
