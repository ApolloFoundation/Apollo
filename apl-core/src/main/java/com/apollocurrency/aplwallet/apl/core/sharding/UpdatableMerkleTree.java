/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpdatableMerkleTree {
    private List<Node> nodes;
    private MessageDigest messageDigest;

    public UpdatableMerkleTree(MessageDigest digest) {
        this.messageDigest = digest;
        this.nodes = new ArrayList<>();
        this.nodes.add(new Node(null));
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
        if (nodes.size() == 0) {
            return null;
        } else {
            return nodes.get(0);
        }
    }

    public void appendLeaf(Node node) {
        if (nodes.size() <= 2) {
            nodes.add(node);
            updateHash(0);
        } else {
            int pos = nodes.size();
            int parentIndex = getParentIndex(pos);
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
    private void updateHash(int index) {
        Node node = nodes.get(index);
        int leftChildIndex = getLeftChildIndex(index);
        int rightChildIndex = getRightChildIndex(index);
        if (leftChildIndex < nodes.size()) {
            Node left = nodes.get(leftChildIndex);
            if (rightChildIndex >= nodes.size()) {
                node.setValue(left.getValue());
            } else {
                Node right = nodes.get(rightChildIndex);
                messageDigest.update(left.getValue());
                messageDigest.update(right.getValue());
                byte[] hash = messageDigest.digest();
                node.setValue(hash);
            }
        }
        int parent = getParentIndex(index);
        if (index != 0) {
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
        this.appendLeaf(new Node(messageDigest.digest(value)));
    }
    public void appendLeaves(byte[][] values) {
        for (byte[] data : values) {
            appendLeaf(new Node(messageDigest.digest(data)));
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
