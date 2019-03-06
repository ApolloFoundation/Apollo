/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class MerkleTree {

    private MerkleNode root;
    private List<MerkleNode> nodes;
    private List<MerkleNode> leaves;

    public MerkleTree() {
        this.nodes = new ArrayList<>();
        this.leaves = new ArrayList<>();
    }

    public List<MerkleNode> getLeaves() {
        return leaves;
    }
    public List<MerkleNode> getNodes() {
        return nodes;
    }
    public MerkleNode getRoot() {
        return root;
    }

    public MerkleNode appendLeaf(MerkleNode node) {
        this.nodes.add(node);
        this.leaves.add(node);
        return node;
    }

    public void appendLeaves(MerkleNode[] nodes) {
        for (MerkleNode node : nodes) {
            this.appendLeaf(node);
        }
    }

    public MerkleNode appendLeaf(MerkleHash hash) {
        return this.appendLeaf(new MerkleNode(hash));
    }

    public List<MerkleNode> appendLeaves(MerkleHash[] hashes) {
        List<MerkleNode> nodes = new ArrayList<>();
        for (MerkleHash hash : hashes) {
            nodes.add(this.appendLeaf(hash));
        }
        return nodes;
    }

    public MerkleHash addTree(MerkleTree tree) {
        if (this.leaves.size() <= 0) throw new InvalidParameterException("Cannot add to a tree with no leaves!");
        tree.leaves.forEach(this::appendLeaf);
        return this.buildTree();
    }

    public MerkleHash buildTree() {
        if (this.leaves.size() <= 0) throw new InvalidParameterException("Cannot add to a tree with no leaves!");
        this.buildTree(this.leaves);
        return this.root.getHash();
    }

    public void buildTree(List<MerkleNode> nodes) {
        if (nodes.size() <= 0) throw new InvalidParameterException("Node list not expected to be empty!");

        if (nodes.size() == 1) {
            this.root = nodes.get(0);
        } else {
            List<MerkleNode> parents = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i += 2) {
                MerkleNode right = (i + 1 < nodes.size()) ? nodes.get(i + 1) : null;
                MerkleNode parent = new MerkleNode(nodes.get(i), right);
                parents.add(parent);
            }
            buildTree(parents);
        }
    }

    public List<MerkleProofHash> auditProof(MerkleHash leafHash) {
        List<MerkleProofHash> auditTrail = new ArrayList<>();

        MerkleNode leafNode = this.findLeaf(leafHash);

        if (leafNode != null) {
            if (leafNode.getParent() == null) throw new InvalidParameterException("Expected leaf to have a parent!");
            MerkleNode parent = leafNode.getParent();
            this.buildAuditTrail(auditTrail, parent, leafNode);
        }

        return auditTrail;
    }

    public static boolean verifyAudit(MerkleHash rootHash, MerkleHash leafHash, List<MerkleProofHash> auditTrail) {
        if (auditTrail.size() <= 0) throw new InvalidParameterException("Audit trail cannot be empty!");

        MerkleHash testHash = leafHash;

        for (MerkleProofHash auditHash : auditTrail) {
            testHash = auditHash.direction == MerkleProofHash.Branch.RIGHT
                    ? MerkleHash.create(testHash, auditHash.hash)
                    : MerkleHash.create(auditHash.hash, testHash);
        }

        return testHash.equals(rootHash);
    }

    private MerkleNode findLeaf(MerkleHash hash) {
        return this.leaves.stream()
                .filter((leaf) -> leaf.getHash() == hash)
                .findFirst()
                .orElse(null);
    }

    private void buildAuditTrail(List<MerkleProofHash> auditTrail, MerkleNode parent, MerkleNode child) {
        if (parent != null) {
            if (child.getParent() != parent) {
                throw new InvalidParameterException("Parent of child is not expected parent!");
            }

            MerkleNode nextChild = parent.getLeftNode() == child ? parent.getRightNode() : parent.getLeftNode();
            MerkleProofHash.Branch direction = parent.getLeftNode() == child
                    ? MerkleProofHash.Branch.RIGHT
                    : MerkleProofHash.Branch.LEFT;

            if (nextChild != null) auditTrail.add(new MerkleProofHash(nextChild.getHash(), direction));

            this.buildAuditTrail(auditTrail, parent.getParent(), child.getParent());
        }
    }
}
