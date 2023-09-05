/**
 * Copyright 2023 AntGroup CO., Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.alipay.alps.flatv3.sampler.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;

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

