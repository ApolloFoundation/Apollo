/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import java.security.InvalidParameterException;
import java.util.Objects;

public class MerkleNode {
    private MerkleHash hash;
    private MerkleNode leftNode;
    private MerkleNode rightNode;
    private MerkleNode parent;

    public MerkleNode() {
    }

    public MerkleNode(MerkleHash hash) {
        this.hash = hash;
    }

    public MerkleNode(MerkleNode left, MerkleNode right) {
        this.leftNode = left;
        this.rightNode = right;
        this.leftNode.parent = this;
        if (this.rightNode != null) this.rightNode.parent = this;

        this.computeHash();
    }

    public boolean isLeaf() {
        return this.leftNode == null && this.rightNode == null;
    }

    @Override
    public String toString() {
        return hash.toString();
    }

    public void setLeftNode(MerkleNode node) {
        if (node.hash == null) {
            throw new InvalidParameterException("Node hash must be initialized!");
        }

        this.leftNode = node;
        this.leftNode.parent = this;

        this.computeHash();
    }

    public void setRightNode(MerkleNode node) {
        if (node.hash == null) {
            throw new InvalidParameterException("Node hash must be initialized!");
        }

        this.rightNode = node;
        this.rightNode.parent = this;

        if (this.leftNode != null) {
            this.computeHash();
        }
    }

    public boolean canVerifyHash() {
        return (this.leftNode != null && this.rightNode != null) || (this.leftNode != null);
    }

    public boolean verifyHash() {
        if (this.leftNode == null && this.rightNode == null) return true;
        if (this.rightNode == null) return hash.equals(leftNode.hash);

        if (this.leftNode == null) {
            throw new InvalidParameterException("Left branch must be a node if right branch is a node!");
        }

        MerkleHash leftRightHash = MerkleHash.create(this.leftNode.hash, this.rightNode.hash);
        return hash.equals(leftRightHash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MerkleNode)) return false;
        MerkleNode that = (MerkleNode) o;
        return this.hash.equals(that.hash);
    }

    public MerkleHash getHash() {
        return hash;
    }

    public MerkleNode getParent() {
        return parent;
    }

    public MerkleNode getLeftNode() {
        return leftNode;
    }

    public MerkleNode getRightNode() {
        return rightNode;
    }

    public void computeHash() {
        if (this.rightNode == null) {
            this.hash = this.leftNode.hash;
        } else {
            this.hash = MerkleHash.create(MerkleHash.concatenate(
                    this.leftNode.hash.getValue(), this.rightNode.hash.getValue()));
        }

        if (this.parent != null) {
            this.parent.computeHash();
        }
    }

    @Override
    public int hashCode() {

        return Objects.hash(hash, leftNode, rightNode, parent);
    }
}
