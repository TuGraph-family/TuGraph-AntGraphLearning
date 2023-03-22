package com.alipay.alps.flatv3.sampler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

public class WeightedSelectionTreeTest {
    private List<Integer> elementIndices = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
    private List<Float> weights = new ArrayList<Float>(Arrays.asList(0.1F, 0.2F, 0.3F, 0.1F, 0.3F));
    @Test
    public void testBuildTree() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights);
        WeightedSelectionTree.Node root = tree.getRoot();
        assertEquals(2, root.element);
        assertEquals(0.3, root.leftBranchWeight, 0.001);
        assertEquals(0.4, root.rightBranchWeight, 0.001);
        assertEquals(0.3, root.elementWeight, 0.001);

        assertEquals(0, root.left.element);
        assertEquals(0.0, root.left.leftBranchWeight, 0.001);
        assertEquals(0.2, root.left.rightBranchWeight, 0.001);
        assertEquals(0.1, root.left.elementWeight, 0.001);

        assertEquals(3, root.right.element);
        assertEquals(0.0, root.right.leftBranchWeight, 0.001);
        assertEquals(0.3, root.right.rightBranchWeight, 0.001);
        assertEquals(0.1, root.right.elementWeight, 0.001);
    }

    @Test
    public void testSelectNode() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights);
        int removedNode = tree.selectNode(0.24);
        assertEquals(removedNode, 1);
    }

    @Test
    public void testSelectNode2() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights);
        int removedNode = tree.selectNode(0.34);
        assertEquals(removedNode, 2);
    }

    @Test
    public void testSelectNode3() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights);
        int removedNode = tree.selectNode(0.64);
        assertEquals(removedNode, 3);
    }

    @Test
    public void testSelectNode4() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights);
        int removedNode = tree.selectNode(0.78);
        assertEquals(removedNode, 4);
    }

    @Test
    public void testSelectNode5() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights);
        int removedNode = tree.selectNode(0.83);
        assertEquals(removedNode, 4);
    }

    @Test
    public void testRemoveNode() {
        // remove a leaf node
        // remove a node having a left child
        // remove a node having both children
    }
}

