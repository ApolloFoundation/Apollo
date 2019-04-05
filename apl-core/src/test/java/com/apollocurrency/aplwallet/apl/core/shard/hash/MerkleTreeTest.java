/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.hash;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

class MerkleTreeTest {
    static final String data1 = "123";
    static final String data2 = "1234";
    static final String data3 = "12345";
    static final String data4 = "123456";
    static final String data5 = "1234567";
    static final String data6 = "12345678";
    static final String data7 = "123456789";
    static final String data8 = "1234567890";
    static final String data9 = "12345678901";
    static byte[] hash1 = Convert.parseHexString(
            "A665A45920422F9D417E4867EFDC4FB8A04A1F3FFF1FA07E998E86F7F7A27AE3".toLowerCase());
    static byte[] hash2 = Convert.parseHexString(
            "03AC674216F3E15C761EE1A5E255F067953623C8B388B4459E13F978D7C846F4".toLowerCase());
    static byte[] hash3 = Convert.parseHexString(
            "5994471ABB01112AFCC18159F6CC74B4F511B99806DA59B3CAF5A9C173CACFC5".toLowerCase());
    static final byte[] hash4 = Convert.parseHexString(
            "8D969EEF6ECAD3C29A3A629280E686CF0C3F5D5A86AFF3CA12020C923ADC6C92".toLowerCase());
    static final byte[] hash5 = Convert.parseHexString(
            "8BB0CF6EB9B17D0F7D22B456F121257DC1254E1F01665370476383EA776DF414".toLowerCase());
    static final byte[] hash6 = Convert.parseHexString(
            "EF797C8118F02DFB649607DD5D3F8C7623048C9C063D532CC95C5ED7A898A64F".toLowerCase());
    static final byte[] hash7 = Convert.parseHexString(
            "15E2B0D3C33891EBB0F1EF609EC419420C20E320CE94C65FBC8C3312448EB225".toLowerCase());
    static final byte[] hash8 = Convert.parseHexString(
            "C775E7B757EDE630CD0AA1113BD102661AB38829CA52A6422AB782862F268646".toLowerCase());
    static final byte[] hash9 = Convert.parseHexString(
            "254AA248ACB47DD654CA3EA53F48C2C26D641D23D7E2E93A1EC56258DF7674C4".toLowerCase());
    static final Node leaf1 = new Node(hash1);
    static final Node leaf2 = new Node(hash2);
    static final Node leaf3 = new Node(hash3);
    static final Node leaf4 = new Node(hash4);
    static final Node leaf5 = new Node(hash5);
    static final Node leaf6 = new Node(hash6);
    static final Node leaf7 = new Node(hash7);
    static final Node leaf8 = new Node(hash8);
    static final Node leaf9 = new Node(hash9);

    private byte[] getHash(byte[] hash1, byte[] hash2) {
        MessageDigest md = sha256();
        md.update(hash1);
        md.update(hash2);
        return md.digest();
    }

    @Test
    public void testCreateTreeWithTwoLeafs() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes());
        Node root = new Node(getHash(leaf1.getValue(), leaf2.getValue()));
        List<Node> expectedNodes = Arrays.asList(root, leaf1, leaf2);

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));
    }

    @Test
    public void testCreateEmptyTree() {
        MerkleTree tree = new MerkleTree(sha256());
        List<Node> nodes = tree.getNodes();
        Assertions.assertEquals(0, nodes.size());
    }

    @Test
    public void testCreateTreeWithOneLeaf() {
        List<byte[]> dataList = Collections.singletonList(data1.getBytes());
        Node root = new Node(leaf1.getValue());
        List<Node> expectedNodes = Arrays.asList(root, leaf1);

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));
    }

    @Test
    public void testCreateTreeWithThreeLeafs() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes());
        Node left = new Node(getHash(hash1, hash3));
        Node root = new Node(getHash(left.getValue(), leaf2.getValue()));
        List<Node> expectedNodes = Arrays.asList(root, left, leaf2, leaf1, leaf3);

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));
    }

    private void compareTrees(List<Node> expectedNodes, List<MerkleTree> trees) {
        Objects.requireNonNull(trees);
        if (trees.isEmpty()) {
            throw new RuntimeException("list of trees is empty");
        }
        for (MerkleTree tree : trees) {
            Assertions.assertEquals(expectedNodes.size(), tree.getNodes().size());
            Assertions.assertEquals(expectedNodes, tree.getNodes());
        }
        MerkleTree initialTree = trees.get(0);
        for (int i = 1; i < trees.size(); i++) {
            Assertions.assertEquals(initialTree, trees.get(i));
        }
    }

    @Test
    public void testCreateTreeWithFourLeafs() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(), data4.getBytes());
        Node left = new Node(getHash(hash1, hash3));
        Node right = new Node(getHash(hash2, hash4));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        List<Node> expectedNodes = Arrays.asList(root, left, right, leaf1, leaf3, leaf2, leaf4);

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));
    }

    @Test
    public void testCreateTreeWithFiveLeafs() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(), data4.getBytes(), data5.getBytes());
        Node left1 = new Node(getHash(hash1, hash5));
        Node left2 = new Node(getHash(left1.getValue(), hash3));
        Node right = new Node(getHash(hash2, hash4));
        Node root = new Node(getHash(left2.getValue(), right.getValue()));
        List<Node> expectedNodes = Arrays.asList(root, left2, right, left1, leaf3, leaf2, leaf4, leaf1, leaf5);

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));
    }

    @Test
    public void testCreateTreeWithSixLeafs() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(), data4.getBytes(), data5.getBytes(), data6.getBytes());
        Node leftLeft = new Node(getHash(hash1, hash5));
        Node leftRight = new Node(getHash(hash3, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node right = new Node(getHash(hash2, hash4));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        List<Node> expectedNodes = Arrays.asList(root, left, right, leftLeft, leftRight, leaf2, leaf4, leaf1, leaf5, leaf3, leaf6);

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));
    }

    @Test
    public void testCreateTreeWithSevenLeafs() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(), data4.getBytes(), data5.getBytes(),
                data6.getBytes(), data7.getBytes());
        Node leftLeft = new Node(getHash(hash1, hash5));
        Node leftRight = new Node(getHash(hash3, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node rightLeft = new Node(getHash(hash2, hash7));
        Node right = new Node(getHash(rightLeft.getValue(), hash4));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        List<Node> expectedNodes = Arrays.asList(root, left, right, leftLeft, leftRight, rightLeft, leaf4, leaf1, leaf5, leaf3, leaf6, leaf2, leaf7);

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));
    }

    @Test
    public void testCreateTreeWithEightLeafs() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(), data4.getBytes(), data5.getBytes(),
                data6.getBytes(), data7.getBytes(), data8.getBytes());
        Node leftLeft = new Node(getHash(hash1, hash5));
        Node leftRight = new Node(getHash(hash3, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node rightLeft = new Node(getHash(hash2, hash7));
        Node rightRight = new Node(getHash(hash4, hash8));
        Node right = new Node(getHash(rightLeft.getValue(), rightRight.getValue()));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        List<Node> expectedNodes = Arrays.asList(root, left, right, leftLeft, leftRight, rightLeft, rightRight, leaf1, leaf5, leaf3, leaf6, leaf2,
                leaf7, leaf4, leaf8);

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));
    }

    @Test
    public void testCreateTreeWithNineLeafs() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(), data4.getBytes(), data5.getBytes(),
                data6.getBytes(), data7.getBytes(), data8.getBytes(), data9.getBytes());
        List<Node> expectedNodes = getExpectedNodesForTreeWithNineLeaves();

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        compareTrees(expectedNodes, Arrays.asList(appendTree, buildTree));

    }
    @Test
    public void testCreateDifferentPartiallyBuildTrees() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(), data4.getBytes(), data5.getBytes(),
                data6.getBytes(), data7.getBytes(), data8.getBytes(), data9.getBytes());
        List<Node> expectedNodes = getExpectedNodesForTreeWithNineLeaves();

        MerkleTree appendTree = new MerkleTree(sha256());
        dataList.forEach(appendTree::appendLeaf);
        MerkleTree buildTree = new MerkleTree(sha256(), dataList);
        List<MerkleTree> treesToCompare = new ArrayList<>();
        treesToCompare.add(appendTree);
        treesToCompare.add(buildTree);
        treesToCompare.addAll(createPartiallyBuiltTrees(dataList));
        compareTrees(expectedNodes, treesToCompare);

    }

    private List<MerkleTree> createPartiallyBuiltTrees(List<byte[]> dataList) {
        List<MerkleTree> trees = new ArrayList<>();
        for (int i = 1; i < dataList.size(); i++) {
            trees.add(new MerkleTree(sha256(), dataList.subList(0, i)));
            for (int j = 0; j < trees.size(); j++) {
                MerkleTree updatableMerkleTree = trees.get(j);
                updatableMerkleTree.appendLeaf(dataList.get(i));
            }
        }
        return trees;
    }

    private List<Node> getExpectedNodesForTreeWithNineLeaves() {
        Node leftLeftLeft = new Node(getHash(hash1, hash9));
        Node leftLeft = new Node(getHash(leftLeftLeft.getValue(), hash5));
        Node leftRight = new Node(getHash(hash3, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node rightLeft = new Node(getHash(hash2, hash7));
        Node rightRight = new Node(getHash(hash4, hash8));
        Node right = new Node(getHash(rightLeft.getValue(), rightRight.getValue()));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        List<Node> expectedNodes = Arrays.asList(root, left, right, leftLeft, leftRight, rightLeft, rightRight, leftLeftLeft, leaf5, leaf3,
                leaf6, leaf2,
                leaf7, leaf4, leaf8, leaf1, leaf9);
        return expectedNodes;
    }

    @Test
    public void testGetNodeIndexes() {
        int[][] nodeIndexes = MerkleTree.getNodeIndexes(8);
        Assertions.assertArrayEquals(new int[][] {{0, 2, 4, 5}, {1, 3, 6, 7}}, nodeIndexes);
        nodeIndexes = MerkleTree.getNodeIndexes(16);
        Assertions.assertArrayEquals(new int[][] {{0, 2, 4, 5, 8, 9, 10, 11}, {1, 3, 6, 7, 12, 13, 14, 15}}, nodeIndexes);
    }

    @Test
    public void testGetNodeOrderedNodeIndexes() {
        int[] ordered = MerkleTree.orderNodeIndexes(new int[] {0, 2, 4, 5}, new int[] {1, 3, 6, 7});
        Assertions.assertArrayEquals(new int[] {0, 4, 2, 5, 1, 6, 3, 7}, ordered);
        ordered = MerkleTree.orderNodeIndexes(new int[] {0, 2, 4, 5, 8, 9, 10, 11}, new int[] {1, 3, 6, 7, 12, 13, 14, 15});
        Assertions.assertArrayEquals(new int[] {0, 8, 4, 9, 2, 10, 5, 11, 1, 12, 6, 13, 3, 14, 7, 15}, ordered);
        ordered = MerkleTree.orderNodeIndexes(new int[] {0, 2, 4, 5, 8, 9, 10, 11, 16, 17, 18, 19, 20, 21, 22, 23}, new int[] {
                1, 3, 6, 7, 12, 13, 14,
                15, 24, 25, 26, 27, 28, 29, 30, 31});
        Assertions.assertArrayEquals(new int[] {
                0, 16, 8, 17, 4, 18, 9, 19, 2, 20, 10, 21, 5, 22, 11, 23, 1, 24, 12, 25, 6, 26, 13, 27, 3, 28,
                14, 29, 7, 30, 15, 31}, ordered);

    }

    @Test
    public void testOrderNodes() {
        int[] ordered = MerkleTree.orderNodes(new int[] {0, 2, 4, 5, 8, 9, 10, 11});
        Assertions.assertArrayEquals(new int[] {0, 8, 4, 9, 2, 10, 5, 11}, ordered);
        ordered = MerkleTree.orderNodes(new int[] {0, 2, 4, 5, 8, 9, 10, 11, 16, 17, 18, 19, 20, 21, 22, 23});
        Assertions.assertArrayEquals(new int[] {0, 16, 8, 17, 4, 18, 9, 19, 2, 20, 10, 21, 5, 22, 11, 23}, ordered);
    }

    @Test
    public void testAppendAndBuildTreesEquals() {
        int counter = 1_000;
        List<byte[]> bytesToAdd = new ArrayList<>();
        MerkleTree appendTree = new MerkleTree(sha256());
        Random random = new Random();
        while (counter-- > 0) {
            byte[] bytes = new byte[256];
            random.nextBytes(bytes);
            bytesToAdd.add(bytes);
            appendTree.appendLeaf(bytes);
        }
        MerkleTree builtTree = new MerkleTree(sha256(), bytesToAdd);
        Assertions.assertEquals(appendTree, builtTree);
    }

    @Test
    @Disabled
    public void testTreeAppendPerformance() {
        int counter = 1_000_000;
        long start = System.currentTimeMillis();
        MerkleTree tree = new MerkleTree(sha256());
        while (counter-- > 0) {
            tree.appendLeaf(data1.getBytes());
        }
        System.out.println("Time: " + (System.currentTimeMillis() - start) / 1000);
        System.out.println(tree.getRoot());
    }

    @Test
    @Disabled
    public void testTreeBuildPerformance() {
        int counter = 1_000_000;
        long start = System.currentTimeMillis();
        List<byte[]> bytesToAdd = new ArrayList<>();
        while (counter-- > 0) {
            bytesToAdd.add(data1.getBytes());
        }
        MerkleTree tree = new MerkleTree(sha256(), bytesToAdd);
        System.out.println("Time: " + (System.currentTimeMillis() - start) / 1000);
        System.out.println(tree.getRoot());
        System.out.println(tree.getNodes().size());
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
