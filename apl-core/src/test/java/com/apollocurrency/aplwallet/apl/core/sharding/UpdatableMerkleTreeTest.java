/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UpdatableMerkleTreeTest {
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
    static final  byte[] hash7 = Convert.parseHexString(
            "15E2B0D3C33891EBB0F1EF609EC419420C20E320CE94C65FBC8C3312448EB225".toLowerCase());
    static final  byte[] hash8 = Convert.parseHexString(
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
    public void testCreateMerkleTreeWithTwoLeafs() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        tree.appendLeaf(data2.getBytes());
        List<Node> nodes = tree.getNodes();
        Assertions.assertEquals(3, nodes.size());
        byte[] root = getHash(hash1, hash2);
        Assertions.assertEquals(Arrays.asList(new Node(root), leaf1, leaf2), nodes);
    }

    @Test
    public void testCreateEmptyTree() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        List<Node> nodes = tree.getNodes();
        Assertions.assertEquals(0, nodes.size());
    }
    @Test
    public void testCreateTreeWithOneLeaf() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        List<Node> nodes = tree.getNodes();
        Assertions.assertEquals(2, nodes.size());
        Assertions.assertEquals(Arrays.asList(leaf1, leaf1), nodes);
    }
    @Test
    public void testCreateTreeWithThreeLeafs() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        tree.appendLeaf(data2.getBytes());
        tree.appendLeaf(data3.getBytes());
        List<Node> nodes = tree.getNodes();
        Node left = new Node(getHash(hash1, hash3));
        Node root = new Node(getHash(left.getValue(), leaf2.getValue()));
        Assertions.assertEquals( 5, nodes.size());
        Assertions.assertEquals(Arrays.asList(root, left, leaf2, leaf1, leaf3), nodes);
    }

    @Test
    public void testCreateTreeWithFourLeafs() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        tree.appendLeaf(data2.getBytes());
        tree.appendLeaf(data3.getBytes());
        tree.appendLeaf(data4.getBytes());
        List<Node> nodes = tree.getNodes();
        Node left = new Node(getHash(hash1, hash3));
        Node right = new Node(getHash(hash2, hash4));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(7, nodes.size());
        Assertions.assertEquals(Arrays.asList(root, left, right, leaf1, leaf3, leaf2, leaf4), nodes);
    }
    @Test
    public void testCreateTreeWithFiveLeafs() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        tree.appendLeaf(data2.getBytes());
        tree.appendLeaf(data3.getBytes());
        tree.appendLeaf(data4.getBytes());
        tree.appendLeaf(data5.getBytes());
        List<Node> nodes = tree.getNodes();
        Node left1 = new Node(getHash(hash1, hash5));
        Node left2 = new Node(getHash(left1.getValue(), hash3));
        Node right = new Node(getHash(hash2, hash4));
        Node root = new Node(getHash(left2.getValue(), right.getValue()));
        Assertions.assertEquals(9, nodes.size());
        Assertions.assertEquals(Arrays.asList(root, left2, right, left1, leaf3, leaf2, leaf4, leaf1, leaf5), nodes);
    }
    @Test
    public void testCreateTreeWithSixLeafs() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        tree.appendLeaf(data2.getBytes());
        tree.appendLeaf(data3.getBytes());
        tree.appendLeaf(data4.getBytes());
        tree.appendLeaf(data5.getBytes());
        tree.appendLeaf(data6.getBytes());
        List<Node> nodes = tree.getNodes();
        Node leftLeft = new Node(getHash(hash1, hash5));
        Node leftRight = new Node(getHash(hash3, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node right = new Node(getHash(hash2, hash4));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(11, nodes.size());
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leftRight,  leaf2, leaf4, leaf1, leaf5, leaf3, leaf6), nodes);
    }
    @Test
    public void testCreateTreeWithSevenLeafs() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        tree.appendLeaf(data2.getBytes());
        tree.appendLeaf(data3.getBytes());
        tree.appendLeaf(data4.getBytes());
        tree.appendLeaf(data5.getBytes());
        tree.appendLeaf(data6.getBytes());
        tree.appendLeaf(data7.getBytes());
        List<Node> nodes = tree.getNodes();
        Node leftLeft = new Node(getHash(hash1, hash5));
        Node leftRight = new Node(getHash(hash3, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node rightLeft = new Node(getHash(hash2, hash7));
        Node right = new Node(getHash(rightLeft.getValue(), hash4));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(13, nodes.size());
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leftRight,rightLeft,  leaf4, leaf1, leaf5, leaf3, leaf6, leaf2, leaf7),
                nodes);
    }
    @Test
    public void testCreateTreeWithEightLeafs() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        tree.appendLeaf(data2.getBytes());
        tree.appendLeaf(data3.getBytes());
        tree.appendLeaf(data4.getBytes());
        tree.appendLeaf(data5.getBytes());
        tree.appendLeaf(data6.getBytes());
        tree.appendLeaf(data7.getBytes());
        tree.appendLeaf(data8.getBytes());
        List<Node> nodes = tree.getNodes();
        Node leftLeft = new Node(getHash(hash1, hash5));
        Node leftRight = new Node(getHash(hash3, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node rightLeft = new Node(getHash(hash2, hash7));
        Node rightRight = new Node(getHash(hash4, hash8));
        Node right = new Node(getHash(rightLeft.getValue(), rightRight.getValue()));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(15, nodes.size());
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leftRight,rightLeft, rightRight,  leaf1, leaf5, leaf3, leaf6, leaf2,
                leaf7, leaf4, leaf8),
                nodes);
    }
    @Test
    public void testCreateTreeWithNineLeafs() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256());
        tree.appendLeaf(data1.getBytes());
        tree.appendLeaf(data2.getBytes());
        tree.appendLeaf(data3.getBytes());
        tree.appendLeaf(data4.getBytes());
        tree.appendLeaf(data5.getBytes());
        tree.appendLeaf(data6.getBytes());
        tree.appendLeaf(data7.getBytes());
        tree.appendLeaf(data8.getBytes());
        tree.appendLeaf(data9.getBytes());
        List<Node> nodes = tree.getNodes();
        Node leftLeftLeft = new Node(getHash(hash1, hash9));
        Node leftLeft = new Node(getHash(leftLeftLeft.getValue(), hash5));
        Node leftRight = new Node(getHash(hash3, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node rightLeft = new Node(getHash(hash2, hash7));
        Node rightRight = new Node(getHash(hash4, hash8));
        Node right = new Node(getHash(rightLeft.getValue(), rightRight.getValue()));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(17, nodes.size());
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leftRight,rightLeft, rightRight,  leftLeftLeft, leaf5, leaf3,
                leaf6, leaf2,
                leaf7, leaf4, leaf8, leaf1, leaf9),
                nodes);
    }


    @Test
    public void testBuildEmptyTree() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), null);
        Assertions.assertEquals(0, tree.getNodes().size());
        tree = new UpdatableMerkleTree(sha256(), Collections.emptyList());
        Assertions.assertEquals(0, tree.getNodes().size());
    }

    @Test
    public void testBuildTreeFromOneNode() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), Collections.singletonList(data1.getBytes()));
        List<Node> nodes = tree.getNodes();
        Assertions.assertEquals(Arrays.asList(leaf1, leaf1), nodes);
    }
    @Test
    public void testBuildTreeFromTwoNodes() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), Arrays.asList(data1.getBytes(), data2.getBytes()));
        List<Node> nodes = tree.getNodes();
        Node root = new Node(getHash(hash1, hash2));
        Assertions.assertEquals(Arrays.asList(root, leaf1, leaf2), nodes);
    }
    @Test
    public void testBuildTreeFromThreeNodes() {
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes()));
        List<Node> nodes = tree.getNodes();
        Node left = new Node(getHash(hash2, hash3));
        Node root = new Node(getHash(left.getValue(), hash1));
        Assertions.assertEquals(Arrays.asList(root, left, leaf1, leaf2, leaf3), nodes);
    }
    @Test
    public void testBuildTreeFromFourNodes() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(),
                data4.getBytes());
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), dataList);
        List<Node> nodes = tree.getNodes();
        Node left = new Node(getHash(hash1, hash2));
        Node right = new Node(getHash(hash3, hash4));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(Arrays.asList(root, left, right, leaf1, leaf2, leaf3, leaf4), nodes);
    }
    @Test
    public void testBuildTreeFromFiveNodes() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(),
                data4.getBytes(), data5.getBytes());
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), dataList);
        List<Node> nodes = tree.getNodes();
        Node leftLeft = new Node(getHash(hash4, hash5));
        Node left = new Node(getHash(leftLeft.getValue(), hash1));
        Node right = new Node(getHash(hash2, hash3));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leaf1, leaf2, leaf3, leaf4, leaf5), nodes);
    }
    @Test
    public void testBuildTreeFromSixNodes() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(),
                data4.getBytes(), data5.getBytes(), data6.getBytes());
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), dataList);
        List<Node> nodes = tree.getNodes();
        Node leftLeft = new Node(getHash(hash3, hash4));
        Node leftRight = new Node(getHash(hash5, hash6));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node right = new Node(getHash(hash1, hash2));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leftRight, leaf1, leaf2, leaf3, leaf4, leaf5, leaf6), nodes);
    }
    @Test
    public void testBuildTreeFromSevenNodes() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(),
                data4.getBytes(), data5.getBytes(), data6.getBytes(), data7.getBytes());
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), dataList);
        List<Node> nodes = tree.getNodes();
        Node leftLeft = new Node(getHash(hash2, hash3));
        Node leftRight = new Node(getHash(hash4, hash5));
        Node rightLeft = new Node(getHash(hash6, hash7));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node right = new Node(getHash(rightLeft.getValue(), hash1));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leftRight, rightLeft, leaf1, leaf2, leaf3, leaf4, leaf5, leaf6, leaf7),
                nodes);
    }
    @Test
    public void testBuildTreeFromEightNodes() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(),
                data4.getBytes(), data5.getBytes(), data6.getBytes(), data7.getBytes(), data8.getBytes());
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), dataList);
        List<Node> nodes = tree.getNodes();
        Node leftLeft = new Node(getHash(hash1, hash2));
        Node leftRight = new Node(getHash(hash3, hash4));
        Node rightLeft = new Node(getHash(hash5, hash6));
        Node rightRight = new Node(getHash(hash7, hash8));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node right = new Node(getHash(rightLeft.getValue(), rightRight.getValue()));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leftRight, rightLeft, rightRight, leaf1, leaf2, leaf3, leaf4, leaf5,
                leaf6, leaf7, leaf8),
                nodes);
    }

    @Test
    public void testBuildAndAppendEqualTrees() {

    }
    @Test
    public void testBuildTreeFromNineNodes() {
        List<byte[]> dataList = Arrays.asList(data1.getBytes(), data2.getBytes(), data3.getBytes(),
                data4.getBytes(), data5.getBytes(), data6.getBytes(), data7.getBytes(), data8.getBytes(), data9.getBytes());
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), dataList);
        List<Node> nodes = tree.getNodes();
        Node leftLeftLeft = new Node(getHash(hash8, hash9));
        Node leftLeft = new Node(getHash(leftLeftLeft.getValue(), hash1));
        Node leftRight = new Node(getHash(hash2, hash3));
        Node rightLeft = new Node(getHash(hash4, hash5));
        Node rightRight = new Node(getHash(hash6, hash7));
        Node left = new Node(getHash(leftLeft.getValue(), leftRight.getValue()));
        Node right = new Node(getHash(rightLeft.getValue(), rightRight.getValue()));
        Node root = new Node(getHash(left.getValue(), right.getValue()));
        Assertions.assertEquals(Arrays.asList(root, left, right, leftLeft, leftRight, rightLeft, rightRight, leftLeftLeft, leaf1, leaf2, leaf3, leaf4, leaf5,
                leaf6, leaf7, leaf8, leaf9),
                nodes);
    }

    @Test
    public void testTreeAppendPerformance() {
        int counter = 1_000_000;
        long start = System.currentTimeMillis();
        UpdatableMerkleTree tree = new UpdatableMerkleTree(Crypto.sha256());
        byte[] bytes = getHash(hash1, hash2);
        while (counter-- > 0) {
            if (counter % 1000 == 0) {
                System.out.println(counter);
            }
            tree.appendLeaf(new Node(bytes));
        }
        System.out.println("Time: " + (System.currentTimeMillis() - start) / 1000);
        System.out.println(tree.getRoot());
    }
    @Test
    public void testTreeBuildPerformance() {
        int counter = 1_000_000;
        long start = System.currentTimeMillis();
        byte[] bytes = getHash(hash1, hash2);
        List<byte[]> bytesToAdd = new ArrayList<>();
        while (counter-- > 0) {
            bytesToAdd.add(bytes);
        }
        UpdatableMerkleTree tree = new UpdatableMerkleTree(sha256(), bytesToAdd);
        System.out.println("Time: " + (System.currentTimeMillis() - start) / 1000);
        System.out.println(tree.getRoot());
    }
    


    private List<Node> fill(int i, Node node) {
        ArrayList<Node> nodes = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            nodes.add(node);
        }
        return nodes;
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
