/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UpdatableMerkleTree {
    private List<Node> nodes;
    private MessageDigest digest;

    public UpdatableMerkleTree(MessageDigest digest) {
        this(digest, null);
    }

    public UpdatableMerkleTree(MessageDigest digest, List<byte[]> dataList) {
        this.digest = digest;
        this.nodes = new ArrayList<>();
        if (dataList != null && !dataList.isEmpty()) {
            buildTreeFromScratch(dataList);
        }
    }

    public void buildTreeFromScratch(List<byte[]> dataList) {
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

    public void buildTree(List<Node> treeNodes) {
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

    public List<Node> buildBottomLevel(List<Node> treeNodes) {
        int size = treeNodes.size();
        List<Node> bottomLevelNodes = new ArrayList<>();
        nodes = new ArrayList<>(treeNodes);
        Collections.reverse(nodes);
        if (!isPowerOfTwo(size)) {
            int n = nearestPowerOfTwo(size);
            int nodesToAdd = size - n;
            int rightStartCounter = size - nodesToAdd - 1;
            for (int i = 0; i < nodesToAdd; i++) {
                Node left = treeNodes.get(i);
                Node right = treeNodes.get(rightStartCounter + i);
                digest.update(left.getValue());
                digest.update(right.getValue());
                Node parent = new Node(digest.digest());
                bottomLevelNodes.add(parent);
            }
            for (int i = bottomLevelNodes.size() - 1; i >= 0; i--) {
                nodes.add(bottomLevelNodes.get(i));
            }
            bottomLevelNodes.addAll(treeNodes.subList(nodesToAdd, size - nodesToAdd));
        } else {
            bottomLevelNodes.addAll(treeNodes);
        }
        return bottomLevelNodes;
    }

    public boolean isPowerOfTwo(int number) {
        return number > 0 && ((number & (number - 1)) == 0);
    }

    public int nearestPowerOfTwo(int number) {
        return (int) Math.pow(2, 32 - Integer.numberOfLeadingZeros(number) - 1);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Node> getLeaves() {
        if (nodes.size() == 1) {
            return Collections.emptyList();
        } else {
            return nodes.subList(nodes.size() / 2, nodes.size());
        }
    }

    public Node getRoot() {
        if (nodes.isEmpty()) {
            return null;
        } else {
            return nodes.get(0);
        }
    }

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

    // assume that all childs were updated
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


    public int getParentIndex(int childIndex) {
        return (childIndex - 1) / 2;
    }

    public int getLeftChildIndex(int parentIndex) {
        return parentIndex * 2 + 1;
    }

    public int getRightChildIndex(int parentIndex) {
        return parentIndex * 2 + 2;
    }

    public void appendLeaves(Node[] nodes) {
        for (Node node : nodes) {
            this.appendLeaf(node);
        }
    }

    public void appendLeaf(byte[] value) {
        this.appendLeaf(new Node(digest.digest(value)));
    }

    public void appendLeaves(byte[][] values) {
        for (byte[] data : values) {
            appendLeaf(new Node(digest.digest(data)));
        }
    }

    public void appendHashedLeaves(byte[][] hashes) {
        for (byte[] hash : hashes) {
            appendLeaf(new Node(hash));
        }
    }

//    public MerkleHash addTree(MerkleTree tree) {
//        if (this.leaves.size() <= 0) throw new InvalidParameterException("Cannot add to a tree with no leaves!");
//        tree.leaves.forEach(this::appendLeaf);
//        return this.buildTree();
//    }
//
//    public MerkleHash buildTree() {
//        if (this.leaves.size() <= 0) throw new InvalidParameterException("Cannot add to a tree with no leaves!");
//        this.buildTree(this.leaves);
//        return this.root.getHash();
//    }
//
//    public void buildTree(List<MerkleNode> nodes) {
//        if (nodes.size() <= 0) throw new InvalidParameterException("Node list not expected to be empty!");
//
//        if (nodes.size() == 1) {
//            this.root = nodes.get(0);
//        } else {
//            List<MerkleNode> parents = new ArrayList<>();
//            for (int i = 0; i < nodes.size(); i += 2) {
//                MerkleNode right = (i + 1 < nodes.size()) ? nodes.get(i + 1) : null;
//                MerkleNode parent = new MerkleNode(nodes.get(i), right);
//                parents.add(parent);
//            }
//            buildTree(parents);
//        }
//    }
//
//    public List<MerkleProofHash> auditProof(MerkleHash leafHash) {
//        List<MerkleProofHash> auditTrail = new ArrayList<>();
//
//        MerkleNode leafNode = this.findLeaf(leafHash);
//
//        if (leafNode != null) {
//            if (leafNode.getParent() == null) throw new InvalidParameterException("Expected leaf to have a parent!");
//            MerkleNode parent = leafNode.getParent();
//            this.buildAuditTrail(auditTrail, parent, leafNode);
//        }
//
//        return auditTrail;
//    }
//
//    public static boolean verifyAudit(MerkleHash rootHash, MerkleHash leafHash, List<MerkleProofHash> auditTrail) {
//        if (auditTrail.size() <= 0) throw new InvalidParameterException("Audit trail cannot be empty!");
//
//        MerkleHash testHash = leafHash;
//
//        for (MerkleProofHash auditHash : auditTrail) {
//            testHash = auditHash.direction == MerkleProofHash.Branch.RIGHT
//                    ? MerkleHash.create(testHash, auditHash.hash)
//                    : MerkleHash.create(auditHash.hash, testHash);
//        }
//
//        return testHash.equals(rootHash);
//    }
//
//    private MerkleNode findLeaf(MerkleHash hash) {
//        return this.leaves.stream()
//                .filter((leaf) -> leaf.getHash() == hash)
//                .findFirst()
//                .orElse(null);
//    }
//
//    private void buildAuditTrail(List<MerkleProofHash> auditTrail, MerkleNode parent, MerkleNode child) {
//        if (parent != null) {
//            if (child.getParent() != parent) {
//                throw new InvalidParameterException("Parent of child is not expected parent!");
//            }
//
//            MerkleNode nextChild = parent.getLeftNode() == child ? parent.getRightNode() : parent.getLeftNode();
//            MerkleProofHash.Branch direction = parent.getLeftNode() == child
//                    ? MerkleProofHash.Branch.RIGHT
//                    : MerkleProofHash.Branch.LEFT;
//
//            if (nextChild != null) auditTrail.add(new MerkleProofHash(nextChild.getHash(), direction));
//
//            this.buildAuditTrail(auditTrail, parent.getParent(), child.getParent());
//        }
//    }
}
