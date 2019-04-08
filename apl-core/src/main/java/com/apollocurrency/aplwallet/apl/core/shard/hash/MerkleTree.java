/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.hash;

import org.apache.commons.collections4.list.TreeList;

import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>Merkle tree implementation based on {@link List}</p>
 * <ul>Use cases
 * <li>
 *     Create empty tree using constructor {@link MerkleTree#MerkleTree(MessageDigest)} and
 *     append new leaves using one of 'append' methods such as {@link MerkleTree#appendLeaf(byte[])}
 * </li>
 * <li>
 *     Create tree from the list of leaves using constructor {@link MerkleTree#MerkleTree(MessageDigest, List)},
 *     this method of tree creation will be significantly faster than ordinary 'appending' tree from scratch.
 *     After tree creation, you can append new leaves using any of 'append' methods
 * </li>
 * </ul>
 * <p>
 *     This implementation guarantee that trees, which were created using same leaves but in different ways (only append, from dataList + append and
 *     only from dataList) will be equals and fully compatible
 * </p>
 *
 */
public class MerkleTree {
    private List<Node> nodes;
    private MessageDigest digest;

    /**
     * Create empty tree
     * @param digest message digest for data hashing
     */
    public MerkleTree(MessageDigest digest) {
        this(digest, null);
    }
    /**
     * Build tree from the list of non-hashed data
     * @param dataList list of non-hashed data
     * @param digest message digest for data hashing
     */
    public MerkleTree(MessageDigest digest, List<byte[]> dataList) {
        Objects.requireNonNull(digest, "digest cannot be null");
        this.digest = digest;
        if (dataList != null && !dataList.isEmpty()) {
            this.nodes = new ArrayList<>(dataList.size() * 2);
            buildTreeFromScratch(dataList);
        } else {
            this.nodes = new ArrayList<>();
        }
    }

    private void buildTreeFromScratch(List<byte[]> dataList) {
        List<Node> treeNodes = dataList.stream().map(digest::digest).map(Node::new).collect(Collectors.toList());
        if (treeNodes.size() < 3) {
            for (Node node : treeNodes) {
                appendLeaf(node);
            }
        } else {
            List<Node> bottomNodes = buildBottomLevel(treeNodes);
            buildTree(bottomNodes);
            Collections.reverse(nodes);
        }
    }

    private void buildTree(List<Node> treeNodes) {
        if (treeNodes.size() <= 0) throw new InvalidParameterException("Node list not expected to be empty!");
        if (treeNodes.size() > 1) {
            List<Node> parents = new ArrayList<>();
            for (int i = 0; i < treeNodes.size(); i += 2) {
                Node right = treeNodes.get(i + 1);
                Node left = treeNodes.get(i);
                digest.update(left.getValue());
                digest.update(right.getValue());
                Node parent = new Node(digest.digest());
                parents.add(parent);
            }
            for (int i = parents.size() - 1; i >= 0; i--) {
                nodes.add(parents.get(i));
            }
            buildTree(parents);
        }
    }

    private List<Node> buildBottomLevel(List<Node> treeNodes) {
        int size = treeNodes.size();
        List<Node> bottomLevelNodes = new ArrayList<>();
        nodes = new ArrayList<>(treeNodes.size() * 2);
        if (!isPowerOfTwo(size)) {

            int n = nearestPowerOfTwo(size);
            int[] innerLevel = getOrderedLeavesIndexes(n);
            int[] outerLevel = getOrderedLeavesIndexes(n * 2);

            int nodesToAdd = size - n;
            // build outer level
            List<Node> outerLeaves = new ArrayList<>(nodesToAdd * 2);
            for (int i = 0; i < nodesToAdd; i++) {
                Node left = treeNodes.get(outerLevel[2 * i]);
                Node right = treeNodes.get(outerLevel[2 * i + 1]);
                digest.update(left.getValue());
                digest.update(right.getValue());
                Node parent = new Node(digest.digest());
                bottomLevelNodes.add(parent);
                outerLeaves.add(left);
                outerLeaves.add(right);
            }
            // add outer level leaves
            for (int i = outerLeaves.size() - 1; i >= 0; i--) {
                nodes.add(outerLeaves.get(i));
            }
            // add inner level leaves
            for (int i = n - 1; i >= nodesToAdd; i--) {
                nodes.add(treeNodes.get(innerLevel[i]));
            }
            // add outer level nodes
            for (int i = bottomLevelNodes.size() - 1; i >= 0; i--) {
                nodes.add(bottomLevelNodes.get(i));
            }
            // add inner level nodes as parent for next level
            for (int i = nodesToAdd; i < n; i++) {
                bottomLevelNodes.add(treeNodes.get(innerLevel[i]));
            }
        } else {
            int n = nearestPowerOfTwo(size);
            int[] outerLevel = getOrderedLeavesIndexes(n);

            // add inner level nodes
            for (int i = n - 1; i >= 0; i--) {
                nodes.add(treeNodes.get(outerLevel[i]));
            }
            // add inner level nodes as parent for next level
            for (int i = 0; i < n; i++) {
                bottomLevelNodes.add(treeNodes.get(outerLevel[i]));
            }
        }
        return bottomLevelNodes;
    }

    static boolean isPowerOfTwo(int number) {
        return number > 0 && ((number & (number - 1)) == 0);
    }

    private int[] getOrderedLeavesIndexes(int size) {
        int[][] nodeIndexes = getNodeIndexes(size);
        return orderNodeIndexes(nodeIndexes[0], nodeIndexes[1]);
    }

    private int nearestPowerOfTwo(int number) {
        return 1 << 32 - Integer.numberOfLeadingZeros(number) - 1;
    }

    static int[][] getNodeIndexes(int size) {
        if (size < 2) {
            throw new IllegalArgumentException("Cannot find indexes for size: " + size);
        }
        if (!isPowerOfTwo(size)) {
            throw new IllegalArgumentException("Size is not power of two: " + size);
        }
        int[][] res = new int[2][];
        int[] left = new int[size / 2];
        int[] right = new int[size / 2];
        res[0] = left;
        res[1] = right;
        left[0] = 0;
        right[0] = 1;
        int indexCounter = 1;
        int p = 1;
        int prev = 1 << p;
        int cur = 1 << p + 1;
        while (cur <= size) {
            int steps = (cur - prev) / 2;
            int mid = prev + steps;

            for (int i = 0; i < steps; i++) {
                left[indexCounter] = prev + i;
                right[indexCounter] = mid + i;
                indexCounter++;
            }

            p++;
            prev = 1 << p;
            cur = 1 << p + 1;
        }
        return res;
    }

    static int[] orderNodeIndexes(int[] left, int[] right) {
        Objects.requireNonNull(left, "left cannot be null");
        Objects.requireNonNull(right, "right cannot be null");
        if (left.length != right.length) {
            throw new IllegalArgumentException("Branch size mismatch");
        }
        if (!isPowerOfTwo(left.length)) {
            throw new IllegalArgumentException("Size of branch is not power of two");
        }

        int branchLength = left.length;
        int[] ordered = new int[2 * branchLength];
        int[] leftOrdered = orderNodes(left);
        int[] rightOrdered = orderNodes(right);
        System.arraycopy(leftOrdered, 0, ordered, 0, branchLength);
        System.arraycopy(rightOrdered, 0, ordered, branchLength, branchLength);
        return ordered;
    }

    static int[] orderNodes(int[] indexes) {
        if (indexes.length <= 2) {
            return indexes;
        }
        int[] res = new int[indexes.length];
        List<Integer> list = new TreeList<>();
        list.add(indexes[0]);
        list.add(indexes[1]);
        int position = 1;
        while (2 * position != indexes.length) {
            position <<= 1;
            int start = 1;
            for (int i = position; i < 2 * position; i++) {
                if (list.size() <= start) {
                    list.add(indexes[i]);
                } else {
                    list.add(start, indexes[i]);
                    start += 2;
                }
            }
        }
        for (int i = 0; i < indexes.length; i++) {
            res[i] = list.get(i);
        }
        return res;
    }

    /**
     * @return all tree nodes including root, intermediate nodes and leaves
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * @return list of leaves of the three or empty list, when tree has no leaves
     */
    public List<Node> getLeaves() {
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        } else {
            return nodes.subList(nodes.size() / 2, nodes.size());
        }
    }

    /**
     * @return merkle root or null when tree is empty
     */
    public Node getRoot() {
        if (nodes.isEmpty()) {
            return null;
        } else {
            return nodes.get(0);
        }
    }

    /**
     * <p>Add new leaf to the end of tree</p>
     * <p>This method guarantee, that no more than 'log2(numberOfLeaves) + 1' hash updates will be performed</p>
     * @param node leaf with hashed value
     */
    public void appendLeaf(Node node) {
        int size = nodes.size();
        if (size <= 2) {
            nodes.add(node);
            if (size == 0) {
                nodes.add(new Node(node.getValue()));
            } else {
                updateHash(0);
            }
        } else {
            int parentIndex = getParentIndex(size);
            Node nodeToReplace = nodes.get(parentIndex);
            Node emptyNode = new Node(null);
            nodes.set(parentIndex, emptyNode);
            nodes.add(nodeToReplace);
            nodes.add(node);
            updateHash(parentIndex);
        }
    }

    // assume that all children were updated
    // IMPORTANT: recursive update
    private void updateHash(int nodeIndex) {
        Node node = nodes.get(nodeIndex);
        int leftChildIndex = getLeftChildIndex(nodeIndex);
        int rightChildIndex = getRightChildIndex(nodeIndex);
        if (leftChildIndex < nodes.size()) {
            Node left = nodes.get(leftChildIndex);
            if (rightChildIndex >= nodes.size()) {
                node.setValue(left.getValue());
            } else {
                Node right = nodes.get(rightChildIndex);
                digest.update(left.getValue());
                digest.update(right.getValue());
                byte[] hash = digest.digest();
                node.setValue(hash);
            }
        }
        int parent = getParentIndex(nodeIndex);
        if (nodeIndex != 0) {
            updateHash(parent);
        }
    }


    static int getParentIndex(int childIndex) {
        return (childIndex - 1) / 2;
    }

    static int getLeftChildIndex(int parentIndex) {
        return parentIndex * 2 + 1;
    }

    static int getRightChildIndex(int parentIndex) {
        return parentIndex * 2 + 2;
    }
    /**
     * Add array of leaves to the end of tree
     * @param nodes array of leaves
     */
    public void appendLeaves(Node[] nodes) {
        for (Node node : nodes) {
            this.appendLeaf(node);
        }
    }

    /**
     * Add new non-hashed data to the end of tree
     * @param value non-hashed data to append
     */
    public void appendLeaf(byte[] value) {
        this.appendLeaf(new Node(digest.digest(value)));
    }

    /**
     * Add array of non-hashed elements to the end of tree
     * @param values array which consist of non-hashed data elements
     */
    public void appendLeaves(byte[][] values) {
        for (byte[] data : values) {
            appendLeaf(new Node(digest.digest(data)));
        }
    }

    /**
     * Add array of hashed elements to the end of tree
     * @param hashes array which consist of hashed data elements
     */
    public void appendHashedLeaves(byte[][] hashes) {
        for (byte[] hash : hashes) {
            appendLeaf(new Node(hash));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerkleTree)) return false;
        MerkleTree that = (MerkleTree) o;
        return nodes.equals(that.nodes);
    }
}
