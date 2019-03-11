/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpdatableMerkleTree {
    private List<Node> nodes;
    private int leafs;
    private int height;
    private Node root;
    private MessageDigest messageDigest;

    public UpdatableMerkleTree(MessageDigest digest) {
        this.messageDigest = digest;
        this.nodes = new ArrayList<>();
        this.nodes.add(new Node(null));
        this.root = nodes.get(0);
    }

    public void buildTree(List<Node> treeNodes) {
        if (treeNodes.size() <= 0) throw new InvalidParameterException("Node list not expected to be empty!");

        if (treeNodes.size() > 1) {
            List<Node> parents = new ArrayList<>();
            for (int i = 0; i < treeNodes.size(); i += 2) {
                Node right = (i + 1 < treeNodes.size()) ? treeNodes.get(i + 1) : null;
                Node left = treeNodes.get(i);
                messageDigest.update(left.getValue());
                if (right != null) {
                    messageDigest.update(right.getValue());
                }
                Node parent = new Node(messageDigest.digest());
                parents.add(parent);
            }
            buildTree(parents);
        } else {
            updateHash(0);
        }
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

    public void appendNewBalancedLeaf(Node node) {
        if (leafs >= 2) {
            if (leafs == Math.pow(2, height + 1)) {
                Node right = nodes.get(2);
                Node left = nodes.get(1);

                messageDigest.update(left.getValue());
                messageDigest.update(right.getValue());
                Node upperNode = new Node(messageDigest.digest());
                upperNode.setLeft(left);
                upperNode.setRight(right);
                upperNode.setParent(root);
                nodes.add(1, upperNode);
                nodes.add(2, new Node(null));
                int newNodes = 0;
                int offset = 3;
                while (newNodes < height) {
                    offset += Math.pow(2, newNodes + 1);
                    nodes.add(offset, new Node(null));
                    newNodes++;
                }
                nodes.add(node);
                leafs++;
                int drift = getDrift(leafs);
                int parentIndex = getParentIndex(nodes.size() - 1 + drift);
                updateHash(parentIndex, Math.max(drift, 0));
                height++;
            } else if (leafs % 2 == 0) {
                createNodes(leafs, nodes);
                leafs++;
                int drift = getDrift(leafs);
                nodes.add(node);
                int parentIndex = getParentIndex(nodes.size() - 1 + drift);
                updateHash(parentIndex, Math.max(drift, 0));
            } else {
                leafs++;
                int drift = getDrift(leafs);
                nodes.add(node);
                int parentIndex = getParentIndex(nodes.size() - 1 + drift);
                updateHash(parentIndex, Math.max(drift, 0));
            }
        } else {
            nodes.add(node);
            height = 0;
            leafs++;
            updateHash(0);
        }

    }

    static void createNodes(int leafs, List<Node> nodes) {
        int n = leafs + 1;
        int in = leafs;
        int total = leafs;
        Node mandatory = new Node(null);
        int size = nodes.size();
        nodes.add(size - total, mandatory);
        n = n / 2 + n % 2;
        in = in / 2 + in % 2;
        total += in;
        while (n != 1) {
            n = n / 2 + n % 2;
            in = in / 2 + in % 2;
            total += in;
            if (n % 2 == 0) {
                break;
            }
            if (n % 2 != 0) {
                Node node = new Node(null);
                nodes.add(size - total, node);
            }
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
                messageDigest.update(left.getValue());
                messageDigest.update(right.getValue());
                byte[] hash = messageDigest.digest();
                node.setValue(hash);
            }
        }
        int parent = getParentIndex(nodeIndex);
        if (nodeIndex != 0) {
            updateHash(parent);
        }
    }
    // assume that all childs were updated
    // IMPORTANT: recursive update
    private void updateHash(int nodeIndex, int drift) {
        Node node = nodes.get(nodeIndex);
        int leftChildIndex = getLeftChildIndex(nodeIndex, drift);
        int rightChildIndex = getRightChildIndex(nodeIndex, drift);
        if (leftChildIndex < nodes.size()) {
            Node left = nodes.get(leftChildIndex);
            if (rightChildIndex >= nodes.size() || invalidChild(leafs, nodeIndex, rightChildIndex)) {
                node.setValue(left.getValue());
            } else {
                Node right = nodes.get(rightChildIndex);
                messageDigest.update(left.getValue());
                messageDigest.update(right.getValue());
                byte[] hash = messageDigest.digest();
                node.setValue(hash);
            }
        }
        int parent = getParentIndex(nodeIndex);
        if (nodeIndex != 0) {
            updateHash(parent, Math.max(--drift, 0));
        }
    }

    private boolean invalidChild(int leafs, int nodeIndex, int rightChildIndex) {
        int s = nodes.size();
        int n = leafs;
        int total = leafs;
        while (n != 1) {
            n = n / 2 + n % 2;
            total += n;
            if (rightChildIndex >= s - total) {
                if (n % 2 != 0 && n != 1) {
                    return true;
                }
            }

        }
        return false;
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
    public int getLeftChildIndex(int parentIndex, int drift) {
        return parentIndex * 2 + 1 - drift;
    }

    public int getRightChildIndex(int parentIndex, int drift) {
        return parentIndex * 2 + 2 - drift;
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

    public int getDrift(int leafs) {
        int n = leafs;
        int drift = 0;
        while (n != 1) {
            n = n / 2 + n % 2;
            if (n != 1 && n % 2 != 0) {
                drift++;
            }
        }
        return drift;
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
