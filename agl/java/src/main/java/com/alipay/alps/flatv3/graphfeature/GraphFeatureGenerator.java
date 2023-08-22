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

package com.alipay.alps.flatv3.graphfeature;

import com.antfin.agl.proto.graph_feature.BytesList;
import com.antfin.agl.proto.graph_feature.Edges;
import com.antfin.agl.proto.graph_feature.Features;
import com.antfin.agl.proto.graph_feature.Features.DenseFeatures;
import com.antfin.agl.proto.graph_feature.Features.SparseKFeatures;
import com.antfin.agl.proto.graph_feature.Features.SparseKVFeatures;
import com.antfin.agl.proto.graph_feature.Float64List;
import com.antfin.agl.proto.graph_feature.FloatList;
import com.antfin.agl.proto.graph_feature.GraphFeature;
import com.antfin.agl.proto.graph_feature.IDs;
import com.antfin.agl.proto.graph_feature.Int64List;
import com.antfin.agl.proto.graph_feature.Nodes;
import com.antfin.agl.proto.sampler.SubGraphSpec;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * String subgraphSpec = "{'node_spec':[{'node_name':'user','id_type':'string','features':[{'name':'dense','type':'dense','dim':5,'value':'float32'},{'name':'time','type':'dense','dim':1,'value':'int64'}]},{'node_name':'item','id_type':'string','features':[{'name':'dense','type':'dense','dim':2,'value':'float32'},{'name':'time','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'buy','n1_name':'user','n2_name':'item','id_type':'string','features':[{'name':'dense','type':'dense','dim':2,'value':'float32'},{'name':'time','type':'dense','dim':1,'value':'int64'}]},{'edge_name':'link','n1_name':'item','n2_name':'item','id_type':'string','features':[{'name':'dense','type':'dense','dim':1,'value':'float32'},{'name':'time','type':'dense','dim':1,'value':'int64'}]}]}";
 * GraphFeatureV2Generator graphFeatureGenerator = new GraphFeatureGenerator(subgraphSpec);
 * graphFeatureV2Generator.init(); graphFeatureV2Generator.addNodeInfo("1", "user", "0.2 1.1 2 3
 * 4\t12"); graphFeatureV2Generator.addNodeInfo("2", "item", "0.2 1.1\t324");
 * graphFeatureV2Generator.addEdgeInfo("1", "2", "1-2", "buy", "2.2 3.4\t32435");
 * graphFeatureV2Generator.addNodeInfo("3", "item", "0.2 1.4\t324");
 * graphFeatureV2Generator.addEdgeInfo("2", "3", "2-3", "link", "2.2\t45"); String ret =
 * graphFeatureGenerator.getGraphFeature(preId);
 */
public class GraphFeatureGenerator {

  private final String ADJ_FORMAT = "CSR";
  private SubGraphSpecs subGraphSpecs;
  public Map<String, Integer> nodeIndices = new HashMap<>();
  public Map<String, Map<String, Integer>> nodeIndicesPerType = new HashMap<>();
  public List<String> nodeTypes = new ArrayList<>();
  public Map<String, List<String>> type2NodeIds = new HashMap<>();
  public List<HashSet<String>> node1Edges = new ArrayList<>();
  private List<Features> nodeFeatures = new ArrayList<>();


  public Map<String, Integer> edgeIndices = new HashMap<>();
  public List<String> edgeTypes = new ArrayList<>();
  public List<Integer> edgeNode1IndicesPerType = new ArrayList<>();
  public List<Integer> edgeNode2IndicesPerType = new ArrayList<>();
  private List<Features> edgeFeatures = new ArrayList<>();

  public GraphFeatureGenerator(SubGraphSpecs subGraphSpecs) {
    this.subGraphSpecs = subGraphSpecs;
  }

  public GraphFeatureGenerator(String subgraphSpecJson) {
    subGraphSpecs = new SubGraphSpecs(subgraphSpecJson);
  }

  public void init() {
    nodeIndices.clear();
    nodeIndicesPerType.clear();
    nodeTypes.clear();
    type2NodeIds.clear();
    node1Edges.clear();
    nodeFeatures.clear();
    edgeIndices.clear();
    edgeTypes.clear();
    edgeNode1IndicesPerType.clear();
    edgeNode2IndicesPerType.clear();
    edgeFeatures.clear();
  }

  public void addNodeInfo(String nodeId, String nodeType, String features) {
    addNodeInfo(nodeId, nodeType, subGraphSpecs.generateNodeFeatures(features, nodeType));
  }

  public void addNodeFeaturesPB(String nodeId, String nodeType, Features featuresPb) {
    addNodeInfo(nodeId, nodeType, featuresPb);
  }

  public void addNodeInfo(String nodeId, String nodeType, Features features) {
    if (!nodeIndices.containsKey(nodeId)) {
      nodeIndices.put(nodeId, nodeIndices.size());
      if (nodeIndicesPerType.containsKey(nodeType)) {
        nodeIndicesPerType.get(nodeType).put(nodeId, nodeIndicesPerType.get(nodeType).size());
      } else {
        nodeIndicesPerType.put(nodeType, new HashMap<>());
        nodeIndicesPerType.get(nodeType).put(nodeId, 0);
      }
      nodeTypes.add(nodeType);
      if (!type2NodeIds.containsKey(nodeType)) {
        type2NodeIds.put(nodeType, new ArrayList<>());
      }
      type2NodeIds.get(nodeType).add(nodeId);
      node1Edges.add(new HashSet<>());
      nodeFeatures.add(features);
    }
  }

  public void addEdgeInfo(String node1Id, String node2Id, String edgeId, String edgeType,
      String features) {
    addEdgeInfo(node1Id, node2Id, edgeId, edgeType,
        subGraphSpecs.generateEdgeFeatures(features, edgeType));
  }

  public void addEdgeInfo(String node1Id, String node2Id, String edgeId, String edgeType,
      Features features) {
    try {
      if (!edgeIndices.containsKey(edgeId)) {
        edgeIndices.put(edgeId, edgeIndices.size());
        edgeTypes.add(edgeType);
        edgeNode1IndicesPerType
            .add(nodeIndicesPerType.get(nodeTypes.get(nodeIndices.get(node1Id))).get(node1Id));
        edgeNode2IndicesPerType
            .add(nodeIndicesPerType.get(nodeTypes.get(nodeIndices.get(node2Id))).get(node2Id));
        edgeFeatures.add(features);
        node1Edges.get(nodeIndices.get(node1Id)).add(edgeId);
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "missing edge:" + edgeId + " node1Id:" + node1Id + " node2Id:" + node2Id, e);
    }
  }

  public String getGraphFeature(List<String> roots, boolean compress, boolean needIDs) {
    Map<String, List<Integer>> rootTypeIndices = new HashMap<>();
    GraphFeature.Builder graphFeatureBuilder = generateGraphFeature(roots, rootTypeIndices,
        needIDs);
    GraphFeature.Root.Builder rootBuilder = GraphFeature.Root.newBuilder();
    GraphFeature.Root.MultiItems.Builder multiItemsBuilder = GraphFeature.Root.MultiItems
        .newBuilder();
    multiItemsBuilder.setIsNode(true);
    for (String nodeType : rootTypeIndices.keySet()) {
      Int64List.Builder indicesBuilder = Int64List.newBuilder();
      for (Integer index : rootTypeIndices.get(nodeType)) {
        indicesBuilder.addValue(index);
      }
      multiItemsBuilder.putIndices(nodeType, indicesBuilder.build());
    }
    rootBuilder.setSubgraph(multiItemsBuilder);
    graphFeatureBuilder.setRoot(rootBuilder);

    return serializeGraphFeature(graphFeatureBuilder.build(), compress);
  }

  public String getGraphFeature(List<String> roots, boolean needIDs) {
    return getGraphFeature(roots, true, needIDs);
  }

  private GraphFeature.Builder generateGraphFeature(List<String> rootIds,
      Map<String, List<Integer>> rootTypeIndices, boolean needIDs) {
    // iterate all node types and generate Nodes for each type.
    Map<String, Nodes.Builder> nodesMap = new HashMap<>();
    List<String> allNodeTypes = subGraphSpecs.getAllNodeTypes();
    for (String nodeType : allNodeTypes) {
      // build nodes for current nodeType
      Nodes.Builder mergedNodesBuilder = Nodes.newBuilder();
      List<ByteString> mergedNodeIdBytes = new ArrayList<>();
      Features.Builder mergedNodeFeatureBuilder = Features.newBuilder();
      List<String> nodesOfType = type2NodeIds.get(nodeType);
      mergeIdsAndFeatures(mergedNodeIdBytes, mergedNodeFeatureBuilder, nodesOfType, nodeIndices,
          nodeFeatures);
      for (String root : rootIds) {
        int rootIndex = nodeIndices.get(root);
        String rootType = nodeTypes.get(rootIndex);
        if (rootType.equals(nodeType)) {
          if (!rootTypeIndices.containsKey(nodeType)) {
            rootTypeIndices.put(nodeType, new ArrayList<>());
          }
          rootTypeIndices.get(nodeType).add(nodesOfType.indexOf(root));
        }
      }
      if (needIDs) {
        // add merged ids to mergedNodesBuilder
        mergedNodesBuilder.setNids(
            IDs.newBuilder().setStr(BytesList.newBuilder().addAllValue(mergedNodeIdBytes).build()));
      } else {
        mergedNodesBuilder.setNids(IDs.newBuilder().setInum(mergedNodeIdBytes.size()).build());
      }
      // add merged features to mergedNodesBuilder
      mergedNodesBuilder.setFeatures(mergedNodeFeatureBuilder.build());
      nodesMap.put(nodeType, mergedNodesBuilder);
    }

    // iterate all edge types and generate Edges for each type.
    Map<String, Edges.Builder> edgesMap = new HashMap<>();
    List<SubGraphSpec.EdgeSpec> allEdgeSpecList = subGraphSpecs.getEdgeSpecList();
    for (SubGraphSpec.EdgeSpec edgeSpec : allEdgeSpecList) {
      String edgeType = edgeSpec.getEdgeName();
      String n1Name = edgeSpec.getN1Name();
      String n2Name = edgeSpec.getN2Name();
      List<String> edgeIdsOfType = new ArrayList<>();

      List<Long> indptr = new ArrayList<>();
      List<Long> nbrsIndices = new ArrayList<>();
      for (String nodeId : type2NodeIds.get(n1Name)) {
        for (String edgeId : node1Edges.get(nodeIndices.get(nodeId))) {
          edgeIdsOfType.add(edgeId);
          int edgeIndex = edgeIndices.get(edgeId);
          if (edgeTypes.get(edgeIndex).equals(edgeType)) {
            nbrsIndices.add((long) edgeNode2IndicesPerType.get(edgeIndex));
          }
        }
        indptr.add((long) nbrsIndices.size());
      }

      // build edges for current edgeType
      Edges.Builder mergedEdgesBuilder = Edges.newBuilder();
      List<ByteString> mergedEdgeIdBytes = new ArrayList<>();
      Features.Builder mergedEdgeFeatureBuilder = Features.newBuilder();
      mergeIdsAndFeatures(mergedEdgeIdBytes, mergedEdgeFeatureBuilder, edgeIdsOfType, edgeIndices,
          edgeFeatures);
      if (needIDs) {
        // add merged ids to mergedEdgesBuilder
        mergedEdgesBuilder.setEids(IDs.newBuilder()
            .setStr(BytesList.newBuilder().addAllValue(mergedEdgeIdBytes).build()));  ///?????
      }

      if (ADJ_FORMAT.compareToIgnoreCase("CSR") == 0) {
        // add CSR n1_indices and n2_indices to mergedEdgesBuilder
        Edges.CSR.Builder csrBuilder = Edges.CSR.newBuilder();
        csrBuilder.setIndptr(Int64List.newBuilder().addAllValue(indptr));
        csrBuilder.setNbrsIndices(Int64List.newBuilder().addAllValue(nbrsIndices));
        mergedEdgesBuilder.setCsr(csrBuilder);
      } else {
        // add COO n1_indices and n2_indices to mergedEdgesBuilder
        List<Long> n1IndicesCOO = new ArrayList<>();
        List<Long> n2IndicesCOO = new ArrayList<>();
        for (String edgeId : edgeIdsOfType) {
          int edgeIndex = edgeIndices.get(edgeId);
          n1IndicesCOO.add((long) edgeNode1IndicesPerType.get(edgeIndex));
          n2IndicesCOO.add((long) edgeNode2IndicesPerType.get(edgeIndex));
        }
        Edges.COO.Builder cooBuilder = Edges.COO.newBuilder();
        cooBuilder.setN1Indices(Int64List.newBuilder().addAllValue(n1IndicesCOO));
        cooBuilder.setN2Indices(Int64List.newBuilder().addAllValue(n2IndicesCOO));
        mergedEdgesBuilder.setCoo(cooBuilder);
      }

      // add merged features to mergedEdgesBuilder
      mergedEdgesBuilder.setFeatures(mergedEdgeFeatureBuilder.build());
      // add n1_name and n2_name to mergedEdgesBuilder
      mergedEdgesBuilder.setN1Name(n1Name);
      mergedEdgesBuilder.setN2Name(n2Name);
      edgesMap.put(edgeType, mergedEdgesBuilder);
    }

    // build GraphFeature
    GraphFeature.Builder graphFeatureBuilder = GraphFeature.newBuilder();
    // add nodes to graphFeatureBuilder
    for (String nodeType : allNodeTypes) {
      graphFeatureBuilder.putNodes(nodeType, nodesMap.get(nodeType).build());
    }
    // add edges to graphFeatureBuilder
    for (SubGraphSpec.EdgeSpec edgeSpec : allEdgeSpecList) {
      String edgeType = edgeSpec.getEdgeName();
      graphFeatureBuilder.putEdges(edgeType, edgesMap.get(edgeType).build());
    }
    return graphFeatureBuilder;
  }

  private String serializeGraphFeature(GraphFeature graphFeaturex, boolean compress) {
    String graphFeatureStr = "";
    if (compress) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      GZIPOutputStream gzip = null;
      try {
        gzip = new GZIPOutputStream(out);
        gzip.write(graphFeaturex.toByteArray());
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(gzip);
      }
      graphFeatureStr = BaseEncoding.base64().encode(out.toByteArray());
    } else {
      graphFeatureStr = BaseEncoding.base64().encode(graphFeaturex.toByteArray());
    }
    return graphFeatureStr;
  }

  private void mergeIdsAndFeatures(List<ByteString> mergedIdBytes,
      Features.Builder mergedFeatureBuilder,
      List<String> idsOfOneType, Map<String, Integer> id2Index, List<Features> features) {
    try {
      mergedIdBytes.clear();
      mergedFeatureBuilder.clear();
      // merge featureList into mergedFeatureBuilder
      Map<String, DenseFeatures.Builder> mergedDenseFeatureBuilderMap = new HashMap<>();
      Map<String, SparseKVFeatures.Builder> mergedSparseKVFeaturesBuilderMap = new HashMap<>();
      Map<String, SparseKFeatures.Builder> mergedSparseKFeaturesBuilderMap = new HashMap<>();

      for (String id : idsOfOneType) {
        int index = id2Index.get(id);
        mergedIdBytes.add(ByteString.copyFrom(id.getBytes()));
        // merge features
        Features oneUnitFeatures = features.get(index);
        Map<String, DenseFeatures> denseFeatures = oneUnitFeatures.getDfsMap();
        for (String denseKey : denseFeatures.keySet()) {
          if (!mergedDenseFeatureBuilderMap.containsKey(denseKey)) {
            mergedDenseFeatureBuilderMap.put(denseKey, DenseFeatures.newBuilder());
          }
          DenseFeatures.Builder denseFeaturesBuilder = mergedDenseFeatureBuilderMap.get(denseKey);
          appendDenseFeatures(denseFeaturesBuilder, denseFeatures.get(denseKey));
        }
        Map<String, SparseKVFeatures> sparseKVFeatures = oneUnitFeatures.getSpKvsMap();
        for (String sparseKVKey : sparseKVFeatures.keySet()) {
          if (!mergedSparseKVFeaturesBuilderMap.containsKey(sparseKVKey)) {
            mergedSparseKVFeaturesBuilderMap.put(sparseKVKey, SparseKVFeatures.newBuilder());
          }
          SparseKVFeatures.Builder sparseKVFeaturesBuilder = mergedSparseKVFeaturesBuilderMap
              .get(sparseKVKey);
          appendSparseKVFeatures(sparseKVFeaturesBuilder, sparseKVFeatures.get(sparseKVKey));
        }
        Map<String, SparseKFeatures> sparseKFeatures = oneUnitFeatures.getSpKsMap();
        for (String sparseKKey : sparseKFeatures.keySet()) {
          if (!mergedSparseKFeaturesBuilderMap.containsKey(sparseKKey)) {
            mergedSparseKFeaturesBuilderMap.put(sparseKKey, SparseKFeatures.newBuilder());
          }
          SparseKFeatures.Builder sparseKFeaturesBuilder = mergedSparseKFeaturesBuilderMap
              .get(sparseKKey);
          appendSparseKFeatures(sparseKFeaturesBuilder, sparseKFeatures.get(sparseKKey));
        }
      }
      Map<String, DenseFeatures> mergedDenseFeatureMap = new HashMap<>();
      for (String denseKey : mergedDenseFeatureBuilderMap.keySet()) {
        mergedDenseFeatureMap.put(denseKey, mergedDenseFeatureBuilderMap.get(denseKey).build());
      }
      mergedFeatureBuilder.putAllDfs(mergedDenseFeatureMap);
      Map<String, SparseKVFeatures> mergedSparseKVFeatureMap = new HashMap<>();
      for (String sparseKVKey : mergedSparseKVFeaturesBuilderMap.keySet()) {
        mergedSparseKVFeatureMap
            .put(sparseKVKey, mergedSparseKVFeaturesBuilderMap.get(sparseKVKey).build());
      }
      mergedFeatureBuilder.putAllSpKvs(mergedSparseKVFeatureMap);
      Map<String, SparseKFeatures> mergedSparseKFeatureMap = new HashMap<>();
      for (String sparseKKey : mergedSparseKFeaturesBuilderMap.keySet()) {
        mergedSparseKFeatureMap
            .put(sparseKKey, mergedSparseKFeaturesBuilderMap.get(sparseKKey).build());
      }
      mergedFeatureBuilder.putAllSpKs(mergedSparseKFeatureMap);
    } catch (Exception e) {
      throw new RuntimeException("error:" + Arrays.toString(e.getStackTrace())
          + "\n input idsOfOneType:" + Arrays.toString(idsOfOneType.toArray()) + " id2Index:"
          + Arrays.toString(id2Index.entrySet().toArray()));
    }
  }

  private void appendDenseFeatures(DenseFeatures.Builder dstDenseFeaturesBuilder,
      DenseFeatures srcDenseFeatures) {
    if (dstDenseFeaturesBuilder.getDim() == 0) {
      dstDenseFeaturesBuilder.mergeFrom(srcDenseFeatures);
      return;
    }
    if (dstDenseFeaturesBuilder.getDim() != srcDenseFeatures.getDim()) {
      throw new RuntimeException("DenseFeatures dim not match");
    }
    if (dstDenseFeaturesBuilder.hasF32S() && srcDenseFeatures.hasF32S()) {
      FloatList.Builder dstFloatListBuilder = dstDenseFeaturesBuilder.getF32SBuilder();
      FloatList floatList = srcDenseFeatures.getF32S();
      dstFloatListBuilder.addAllValue(floatList.getValueList());
    } else if (dstDenseFeaturesBuilder.hasF64S() && srcDenseFeatures.hasF64S()) {
      Float64List.Builder dstFloat64ListBuilder = dstDenseFeaturesBuilder.getF64SBuilder();
      Float64List float64List = srcDenseFeatures.getF64S();
      dstFloat64ListBuilder.addAllValue(float64List.getValueList());
    } else if (dstDenseFeaturesBuilder.hasI64S() && srcDenseFeatures.hasI64S()) {
      Int64List.Builder dstInt64ListBuilder = dstDenseFeaturesBuilder.getI64SBuilder();
      Int64List int64List = srcDenseFeatures.getI64S();
      dstInt64ListBuilder.addAllValue(int64List.getValueList());
    } else if (dstDenseFeaturesBuilder.hasRaws() && srcDenseFeatures.hasRaws()) {
      BytesList.Builder dstBytesListBuilder = dstDenseFeaturesBuilder.getRawsBuilder();
      BytesList bytesList = srcDenseFeatures.getRaws();
      dstBytesListBuilder.addAllValue(bytesList.getValueList());
    } else {
      throw new RuntimeException("DenseFeatures type not match");
    }
  }

  private void appendSparseKFeatures(SparseKFeatures.Builder dstSparseKFeaturesBuilder,
      SparseKFeatures srcSparseKFeatures) {
    if (!dstSparseKFeaturesBuilder.hasLens()) {
      dstSparseKFeaturesBuilder.mergeFrom(srcSparseKFeatures);
      return;
    }
    if (dstSparseKFeaturesBuilder.hasLens() && srcSparseKFeatures.hasLens()) {
      Int64List.Builder dstLensBuilder = dstSparseKFeaturesBuilder.getLensBuilder();
      Int64List int64List = srcSparseKFeatures.getLens();
      long lastLen = dstLensBuilder.getValue(dstLensBuilder.getValueCount() - 1);
      for (long value : int64List.getValueList()) {
        dstLensBuilder.addValue(value + lastLen);
        lastLen += value;
      }
    } else {
      throw new RuntimeException("SparseKFeatures lens not match");
    }
    if (dstSparseKFeaturesBuilder.hasI64S() && srcSparseKFeatures.hasI64S()) {
      Int64List.Builder dstInt64ListBuilder = dstSparseKFeaturesBuilder.getI64SBuilder();
      Int64List int64List = srcSparseKFeatures.getI64S();
      dstInt64ListBuilder.addAllValue(int64List.getValueList());
    } else if (dstSparseKFeaturesBuilder.hasRaws() && srcSparseKFeatures.hasRaws()) {
      BytesList.Builder dstBytesListBuilder = dstSparseKFeaturesBuilder.getRawsBuilder();
      BytesList bytesList = srcSparseKFeatures.getRaws();
      dstBytesListBuilder.addAllValue(bytesList.getValueList());
    } else {
      throw new RuntimeException("SparseKFeatures keys not match");
    }
  }

  private void appendSparseKVFeatures(SparseKVFeatures.Builder dstSparseKVFeaturesBuilder,
      SparseKVFeatures srcSparseKVFeatures) {
    if (!dstSparseKVFeaturesBuilder.hasLens()) {
      dstSparseKVFeaturesBuilder.mergeFrom(srcSparseKVFeatures);
      return;
    }
    if (dstSparseKVFeaturesBuilder.hasLens() && srcSparseKVFeatures.hasLens()) {
      Int64List.Builder dstInt64ListBuilder = dstSparseKVFeaturesBuilder.getLensBuilder();
      Int64List int64List = srcSparseKVFeatures.getLens();
      long lastLen = dstInt64ListBuilder.getValue(dstInt64ListBuilder.getValueCount() - 1);
      for (long value : int64List.getValueList()) {
        dstInt64ListBuilder.addValue(value + lastLen);
        lastLen += value;
      }
    } else {
      throw new RuntimeException("SparseKVFeatures lens not match");
    }
    if (dstSparseKVFeaturesBuilder.hasKeys() && srcSparseKVFeatures.hasKeys()) {
      Int64List.Builder dstInt64ListBuilder = dstSparseKVFeaturesBuilder.getKeysBuilder();
      Int64List int64List = srcSparseKVFeatures.getKeys();
      dstInt64ListBuilder.addAllValue(int64List.getValueList());
    } else {
      throw new RuntimeException("SparseKVFeatures keys not match");
    }
    if (dstSparseKVFeaturesBuilder.hasF32S() && srcSparseKVFeatures.hasF32S()) {
      FloatList.Builder dstFloatListBuilder = dstSparseKVFeaturesBuilder.getF32SBuilder();
      FloatList floatList = srcSparseKVFeatures.getF32S();
      dstFloatListBuilder.addAllValue(floatList.getValueList());
    } else if (dstSparseKVFeaturesBuilder.hasF64S() && srcSparseKVFeatures.hasF64S()) {
      Float64List.Builder dstFloat64ListBuilder = dstSparseKVFeaturesBuilder.getF64SBuilder();
      Float64List float64List = srcSparseKVFeatures.getF64S();
      dstFloat64ListBuilder.addAllValue(float64List.getValueList());
    } else if (dstSparseKVFeaturesBuilder.hasI64S() && srcSparseKVFeatures.hasI64S()) {
      Int64List.Builder dstInt64ListBuilder = dstSparseKVFeaturesBuilder.getI64SBuilder();
      Int64List int64List = srcSparseKVFeatures.getI64S();
      dstInt64ListBuilder.addAllValue(int64List.getValueList());
    } else {
      throw new RuntimeException("SparseKVFeatures values not match");
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
  }
}
