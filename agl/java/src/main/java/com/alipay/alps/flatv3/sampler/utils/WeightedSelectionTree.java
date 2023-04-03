package com.alipay.alps.flatv3.sampler.utils;

import java.util.List;
import java.util.Random;

/**
 * A class that implements a Weighted Selection Tree, used to select a random element from a given set of elements with different weights.
 */
public class WeightedSelectionTree {
     /**
     * A class that represents a single node in the Weighted Selection Tree.
     */
    public static class Node {
        private int element;
        private float elementWeight;
        private float leftBranchWeight;
        private float rightBranchWeight;
        private Node left;
        private Node right;

         public int getElement() {
             return element;
         }

         public float getElementWeight() {
             return elementWeight;
         }

         public float getLeftBranchWeight() {
             return leftBranchWeight;
         }

         public float getRightBranchWeight() {
             return rightBranchWeight;
         }

         public Node getLeft() {
             return left;
         }

         public Node getRight() {
             return right;
         }
     }

    private Node root = null;
    private Integer removedNode = null;
    private Random rand;

    public WeightedSelectionTree(List<Integer> elementIndices, List<Float> weights, Random rand) {
        this.rand = rand;
        root = buildTree(elementIndices, 0, elementIndices.size() - 1, weights);
    }

    private float getTotalWeight() {
        return root.element + root.leftBranchWeight + root.rightBranchWeight;
    }

    /**
     * Selects a node from the Weighted Selection Tree using a random selection process.
     *
     * @return The selected node.
     */
    public int nextSample() {
        // the selected node's leftBranchWeight is less than randomNum, it's leftBranchWeight + elementWeight is larger than randomNum.
        double randomNum = rand.nextDouble() * getTotalWeight();
        root = this.selectNode(root, randomNum);
        return removedNode;
    }

    /**
     * Selects a node from the Weighted Selection Tree using a random selection process.
     *
     * @param randomNum The random number used to select the node.
     * @return The selected node.
     */
    public int getSampleByRandomNum(float randomNum) {
        root = this.selectNode(root, randomNum);
        return removedNode;
    }

    public WeightedSelectionTree.Node getRoot() {
        return root;
    }
    
    /**
     * Builds the Weighted Selection Tree from the given list of elements and weights.
     *
     * @param elementIndices The indices of the elements to be included in the tree.
     * @param left The left index of the element indices.
     * @param right The right index of the element indices.
     * @param weights The weights of the elements.
     * @return The root node of the Weighted Selection Tree.
     */
    private Node buildTree(List<Integer> elementIndices, int left, int right, List<Float> weights) {
        if (left > right) {
            return null;
        }
        int mid = left + (right - left) / 2;
        Node node = new Node();
        node.element = elementIndices.get(mid);
        node.elementWeight = weights.get(node.element);
        node.left = buildTree(elementIndices, left, mid-1, weights);
        node.right = buildTree(elementIndices, mid+1, right, weights);
        node.leftBranchWeight = node.left == null ? 0.0F : (node.left.leftBranchWeight +  node.left.rightBranchWeight + node.left.elementWeight);
        node.rightBranchWeight = node.right == null ? 0.0F : (node.right.leftBranchWeight +  node.right.rightBranchWeight + node.right.elementWeight);
        return node;
    }

    private Node selectNode(Node node, double randomNum) {
        if (node == null) {
            return null;
        }
        if (randomNum >= node.leftBranchWeight && randomNum <= node.elementWeight + node.leftBranchWeight) {
            removedNode = node.element;
            removeNode(node);
            return node;
        } else if (randomNum < node.leftBranchWeight) {
            node.left = selectNode(node.left, randomNum);
            node.leftBranchWeight = (node.left != null) ? node.left.leftBranchWeight + node.left.elementWeight + node.left.rightBranchWeight : 0;
        } else {
            node.right = selectNode(node.right, randomNum - node.elementWeight - node.leftBranchWeight);
            node.rightBranchWeight = (node.right != null) ? node.right.leftBranchWeight + node.right.elementWeight + node.right.rightBranchWeight : 0;
        }
        return node;
    }

    /**
     * Removes a node from the Weighted Selection Tree.
     *
     * @param node The node to be removed.
     */
    private void removeNode(Node node) {
        if (node.left == null && node.right == null) {
            node.elementWeight = 0;
            return;
        }
        if (node.left == null) {
            node.element = node.right.element;
            node.elementWeight = node.right.elementWeight;
            node.leftBranchWeight = node.right.leftBranchWeight;
            node.rightBranchWeight = node.right.rightBranchWeight;
            node.left = node.right.left;
            node.right = node.right.right;
        } else if (node.right == null) {
            node.element = node.left.element;
            node.elementWeight = node.left.elementWeight;
            node.leftBranchWeight = node.left.leftBranchWeight;
            node.rightBranchWeight = node.left.rightBranchWeight;
            node.right = node.left.right;
            node.left = node.left.left;
        } else {
            Node pred = node.left;
            while (pred.right != null) {
                pred = pred.right;
            }
            Node predx = node.left;
            while (predx.right != null) {
                predx.rightBranchWeight -= pred.elementWeight;
                predx = predx.right;
            }
            node.element = pred.element;
            node.elementWeight = pred.elementWeight;
            node.leftBranchWeight -= pred.elementWeight;
            removeNode(pred);
        }
    }
}