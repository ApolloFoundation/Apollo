/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MerkleTreeTest {
    private MerkleNode merkleNode1 = new MerkleNode(new MerkleHash(Convert.parseHexString("A665A45920422F9D417E4867EFDC4FB8A04A1F3FFF1FA07E998E86F7F7A27AE3".toLowerCase())));
    private MerkleNode merkleNode2 = new MerkleNode(new MerkleHash(Convert.parseHexString(
            "03AC674216F3E15C761EE1A5E255F067953623C8B388B4459E13F978D7C846F4".toLowerCase())));
    private MerkleNode merkleNode3 = new MerkleNode(new MerkleHash(Convert.parseHexString(
            "5994471ABB01112AFCC18159F6CC74B4F511B99806DA59B3CAF5A9C173CACFC5".toLowerCase())));
    private MerkleNode left = new MerkleNode(merkleNode1, merkleNode2);
    private MerkleNode right = new MerkleNode(merkleNode3, null);
    private MerkleNode root = new MerkleNode(left, right);
    @Test
    public void testBuildMerkleTree() {
        MerkleTree merkleTree = new MerkleTree();
        merkleTree.appendLeaf(MerkleHash.create("123".getBytes()));
        merkleTree.appendLeaf(MerkleHash.create("1234".getBytes()));
        merkleTree.appendLeaf(MerkleHash.create("12345".getBytes()));
        merkleTree.buildTree();
        Assertions.assertEquals(root, merkleTree.getRoot());
    }
}
