package com.alipay.alps.flatv3.sampler.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class WeightedSelectionTreeTest {
    private List<Integer> elementIndices = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4));
    private List<Float> weights = new ArrayList<Float>(Arrays.asList(0.1F, 0.2F, 0.3F, 0.1F, 0.3F));
    private Random random = new Random();

    @Test
    public void testBuildTree() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights, new Random());
        WeightedSelectionTree.Node root = tree.getRoot();
        assertEquals(2, root.getElement());
        assertEquals(0.3, root.getLeftBranchWeight(), 0.001);
        assertEquals(0.4, root.getRightBranchWeight(), 0.001);
        assertEquals(0.3, root.getElementWeight(), 0.001);

        assertEquals(0, root.getLeft().getElement());
        assertEquals(0.0, root.getLeft().getLeftBranchWeight(), 0.001);
        assertEquals(0.2, root.getLeft().getRightBranchWeight(), 0.001);
        assertEquals(0.1, root.getLeft().getElementWeight(), 0.001);

        assertEquals(3, root.getRight().getElement());
        assertEquals(0.0, root.getRight().getLeftBranchWeight(), 0.001);
        assertEquals(0.3, root.getRight().getRightBranchWeight(), 0.001);
        assertEquals(0.1, root.getRight().getElementWeight(), 0.001);
    }

    @Test
    public void testSelectNode() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights, random);
        int removedNode = tree.getSampleByRandomNum(0.24F);
        assertEquals(removedNode, 1);
    }

    @Test
    public void testSelectNode2() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights, random);
        int removedNode = tree.getSampleByRandomNum(0.34F);
        assertEquals(removedNode, 2);
        // check the tree after remove a node
        WeightedSelectionTree.Node root = tree.getRoot();
        assertEquals(1, root.getElement());
        assertEquals(0.1, root.getLeftBranchWeight(), 0.001);
        assertEquals(0.4, root.getRightBranchWeight(), 0.001);
        assertEquals(0.2, root.getElementWeight(), 0.001);
    }

    @Test
    public void testSelectNode3() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights, random);
        int removedNode = tree.getSampleByRandomNum(0.64F);
        assertEquals(removedNode, 3);
    }

    @Test
    public void testSelectNode4() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights, random);
        int removedNode = tree.getSampleByRandomNum(0.78F);
        assertEquals(removedNode, 4);
    }

    @Test
    public void testSelectNode5() {
        WeightedSelectionTree tree = new WeightedSelectionTree(elementIndices, weights, random);
        int removedNode = tree.getSampleByRandomNum(0.83F);
        assertEquals(removedNode, 4);
    }
}

