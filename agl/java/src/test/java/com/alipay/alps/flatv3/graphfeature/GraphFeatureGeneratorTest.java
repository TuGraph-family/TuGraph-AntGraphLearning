package com.alipay.alps.flatv3.graphfeature;

import com.google.common.io.BaseEncoding;
import org.junit.Test;

import com.antfin.agl.proto.graph_feature.Edges;
import com.antfin.agl.proto.graph_feature.GraphFeature;
import com.antfin.agl.proto.graph_feature.Nodes;
import com.google.protobuf.ByteString;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// add several nodes and edges into the graph, then check the result of the graph feature generator
public class GraphFeatureGeneratorTest {
     @Test
     public void testGenerateGraphFeature() throws Exception {
         String subgraphSpec = "{'node_spec':[{'node_name':'user','id_type':'string','features':[{'name':'dense','type':'dense','dim':5,'value':'float32'},{'name':'time','type':'dense','dim':1,'value':'int64'}]},{'node_name':'item','id_type':'string','features':[{'name':'dense','type':'dense','dim':2,'value':'float32'},{'name':'time','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'buy','n1_name':'user','n2_name':'item','id_type':'string','features':[{'name':'dense','type':'dense','dim':2,'value':'float32'},{'name':'time','type':'dense','dim':1,'value':'int64'}]},{'edge_name':'link','n1_name':'item','n2_name':'item','id_type':'string','features':[{'name':'dense','type':'dense','dim':1,'value':'float32'},{'name':'time','type':'dense','dim':1,'value':'int64'}]}]}";
         GraphFeatureGenerator graphFeatureGenerator = new GraphFeatureGenerator(subgraphSpec);
         graphFeatureGenerator.init();
         graphFeatureGenerator.addNodeInfo("1", "user", "0.2 1.1 2 3 4\t12");
         graphFeatureGenerator.addNodeInfo("2", "item", "0.2 1.1\t324");
         graphFeatureGenerator.addEdgeInfo("1", "2", "1-2", "buy", "2.2 3.4\t32435");
         graphFeatureGenerator.addNodeInfo("3", "item", "0.2 1.4\t324");
         graphFeatureGenerator.addEdgeInfo("2", "3", "2-3", "link", "2.2\t45");
         String rootId = "1";
         String graphFeatureBase64 = graphFeatureGenerator.getGraphFeature(rootId);
         System.out.println(graphFeatureBase64);
         // parse the graph feature to ProtoBuf, then check the result
         GraphFeature graphFeatureMessage = GraphFeature.parseFrom(BaseEncoding.base64().decode(graphFeatureBase64));
         // get 'user' nodes
         Nodes users = graphFeatureMessage.getNodesOrThrow("user");
         List<ByteString> userIDBytes = users.getNids().getStr().getValueList();
         Map<String, Long> userID2Indices = new HashMap<>();
         for (int i = 0; i < userIDBytes.size(); i++) {
             userID2Indices.put(userIDBytes.get(i).toStringUtf8(), (long) i);
         }
         assertEquals(1, userIDBytes.size());
         assertEquals("1", userIDBytes.get(0).toStringUtf8());
         // get 'item' nodes
         Nodes items = graphFeatureMessage.getNodesOrThrow("item");
         List<ByteString> itemIDBytes = items.getNids().getStr().getValueList();
         assertEquals(2, itemIDBytes.size());
         Map<String, Long> itemID2Indices = new HashMap<>();
         for (int i = 0; i < itemIDBytes.size(); i++) {
             itemID2Indices.put(itemIDBytes.get(i).toStringUtf8(), (long) i);
         }
         assertEquals(new HashSet<String>(Arrays.asList("2", "3")), itemID2Indices.keySet());
         Map<String, Map<String, Long>> nodeID2IndicesPerType = new HashMap<>();
         nodeID2IndicesPerType.put("user", userID2Indices);
         nodeID2IndicesPerType.put("item", itemID2Indices);

         // get 'buy' edges
         Edges buyEdges = graphFeatureMessage.getEdgesOrThrow("buy");
         String buyN1Name = buyEdges.getN1Name();
         String buyN2Name = buyEdges.getN2Name();
         assertEquals("user", buyN1Name);
         assertEquals("item", buyN2Name);
         List<ByteString> buyEdgeIDBytes = buyEdges.getEids().getStr().getValueList();
         assertEquals(1, buyEdgeIDBytes.size());
         assertEquals("1-2", buyEdgeIDBytes.get(0).toStringUtf8());

         // test csr indices
         List<Long> buyCsrIndptr = buyEdges.getCsr().getIndptr().getValueList();
         assertEquals(1, buyCsrIndptr.size());
         assertEquals(1, buyCsrIndptr.get(0).longValue());
         List<Long> buyNbrsIndices = buyEdges.getCsr().getNbrsIndices().getValueList();
         assertEquals(1, buyNbrsIndices.size());
         assertEquals(nodeID2IndicesPerType.get(buyN2Name).get("2").longValue(), buyNbrsIndices.get(0).longValue());


         // get 'link' edges
         Edges linkEdges = graphFeatureMessage.getEdgesOrThrow("link");
         String linkN1Name = linkEdges.getN1Name();
         String linkN2Name = linkEdges.getN2Name();
         assertEquals("item", linkN1Name);
         assertEquals("item", linkN2Name);
         List<ByteString> linkEdgeIDBytes = linkEdges.getEids().getStr().getValueList();
         assertEquals(1, linkEdgeIDBytes.size());
         assertEquals("2-3", linkEdgeIDBytes.get(0).toStringUtf8());

         // test csr indices
         List<Long> linkCsrIndptr = linkEdges.getCsr().getIndptr().getValueList();
         assertEquals(2, linkCsrIndptr.size());
         assertEquals(1, linkCsrIndptr.get(0).longValue());
         assertEquals(1, linkCsrIndptr.get(1).longValue());
         List<Long> linkNbrsIndices = linkEdges.getCsr().getNbrsIndices().getValueList();
         assertEquals(1, linkNbrsIndices.size());
         assertEquals(nodeID2IndicesPerType.get(linkN2Name).get("3").longValue(), linkNbrsIndices.get(0).longValue());


         // check the dense feature of user nodes
         List<Float> userDenseFeatures = users.getFeatures().getDfsOrThrow("dense").getF32S().getValueList();
         List<Float> expectedUserDenseFeatures = Arrays.asList(0.2f, 1.1f, 2f, 3f, 4f);
         assertEquals(expectedUserDenseFeatures, userDenseFeatures);
         // check the time feature of user nodes
         List<Long> userTimeFeatures = users.getFeatures().getDfsOrThrow("time").getI64S().getValueList();
         List<Long> expectedUserTimeFeatures = Arrays.asList(12L);
         assertEquals(expectedUserTimeFeatures, userTimeFeatures);

         // check the dense feature of the 'buy' edges
         List<Float> buyEdgeDenseFeatures = buyEdges.getFeatures().getDfsOrThrow("dense").getF32S().getValueList();
         List<Float> expectedBuyEdgeDenseFeatures = Arrays.asList(2.2f, 3.4f);
         assertEquals(expectedBuyEdgeDenseFeatures, buyEdgeDenseFeatures);
         // check the time feature of the 'buy' edges
         List<Long> buyEdgeTimeFeatures = buyEdges.getFeatures().getDfsOrThrow("time").getI64S().getValueList();
         List<Long> expectedBuyEdgeTimeFeatures = Arrays.asList(32435L);
         assertEquals(expectedBuyEdgeTimeFeatures, buyEdgeTimeFeatures);
     }

    @Test
    public void testGenerateGraphFeatureWithMultiRoots() throws Exception {
        String subgraphSpec = "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'dense','type':'dense','dim':3,'value':'float32'},{'name':'nf','type':'kv','dim':10,'key':'int64','value':'int64'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'ef','type':'kv','dim':10,'key':'int64','value':'float32'}]}]}";
        GraphFeatureGenerator graphFeatureGenerator = new GraphFeatureGenerator(subgraphSpec);
        graphFeatureGenerator.init();
        graphFeatureGenerator.addNodeInfo("1", "default", "0.1 1.1 1\t1:1 10:1");
        graphFeatureGenerator.addNodeInfo("2", "default", "0.2 2.2 2\t2:2");
        graphFeatureGenerator.addEdgeInfo("1", "2", "1-2", "default", "1:1.1 2:2.2 9:10.1");
        graphFeatureGenerator.addNodeInfo("3", "default", "0.3 3.3 3\t3:3 4:3 10:3");
        graphFeatureGenerator.addEdgeInfo("2", "3", "2-3", "default", "2:2.2 3:3.3 10:2.1");
        List<String> roots = Arrays.asList("1", "2");
        String graphFeatureBase64 = graphFeatureGenerator.getGraphFeature(roots);
        System.out.println(graphFeatureBase64);
        // parse the graph feature to ProtoBuf, then check the result
        GraphFeature graphFeatureMessage = GraphFeature.parseFrom(BaseEncoding.base64().decode(graphFeatureBase64));
        // check the nodes
        Nodes nodes = graphFeatureMessage.getNodesOrThrow("default");
        List<ByteString> idBytes = nodes.getNids().getStr().getValueList();
        assertEquals(3, idBytes.size());
        Map<String, Long> id2Indices = new HashMap<>();
        for (int i = 0; i < idBytes.size(); i++) {
            String userID = idBytes.get(i).toStringUtf8();
            id2Indices.put(userID, (long) i);
        }
        assertEquals(new HashSet<String>(Arrays.asList("1", "2", "3")), id2Indices.keySet());
        Map<String, Map<String, Long>> nodeID2IndicesPerType = new HashMap<>();
        nodeID2IndicesPerType.put("default", id2Indices);
        // check the edges
        Edges edges = graphFeatureMessage.getEdgesOrThrow("default");
        String n1Name = edges.getN1Name();
        String n2Name = edges.getN2Name();
        assertEquals("default", n1Name);
        assertEquals("default", n2Name);
        List<ByteString> edgeIDBytes = edges.getEids().getStr().getValueList();
        assertEquals(2, edgeIDBytes.size());
        assertEquals("1-2", edgeIDBytes.get(0).toStringUtf8());
        assertEquals("2-3", edgeIDBytes.get(1).toStringUtf8());

        // check the coo indices
//        List<Long> n1Indices = edges.getCoo().getN1Indices().getValueList();
//        assertEquals(2, n1Indices.size());
//        assertEquals(nodeID2IndicesPerType.get(n1Name).get("1").longValue(), n1Indices.get(0).longValue());
//        assertEquals(nodeID2IndicesPerType.get(n1Name).get("2").longValue(), n1Indices.get(1).longValue());
//        List<Long> n2Indices = edges.getCoo().getN2Indices().getValueList();
//        assertEquals(2, n2Indices.size());
//        assertEquals(nodeID2IndicesPerType.get(n2Name).get("2").longValue(), n2Indices.get(0).longValue());
//        assertEquals(nodeID2IndicesPerType.get(n2Name).get("3").longValue(), n2Indices.get(1).longValue());

        // check the csr indices
        List<Long> csrIndptr = edges.getCsr().getIndptr().getValueList();
        assertEquals(3, csrIndptr.size());
        assertEquals(1, csrIndptr.get(0).longValue());
        assertEquals(2, csrIndptr.get(1).longValue());
        assertEquals(2, csrIndptr.get(2).longValue());
        List<Long> csrNbrsIndices = edges.getCsr().getNbrsIndices().getValueList();
        assertEquals(2, csrNbrsIndices.size());
        assertEquals(nodeID2IndicesPerType.get(n2Name).get("2").longValue(), csrNbrsIndices.get(0).longValue());
        assertEquals(nodeID2IndicesPerType.get(n2Name).get("3").longValue(), csrNbrsIndices.get(1).longValue());

        // check the dense feature of default nodes
        List<Float> defaultDenseFeatures = nodes.getFeatures().getDfsOrThrow("dense").getF32S().getValueList();
        List<Float> expectedDefaultDenseFeatures = Arrays.asList(0.1f, 1.1f, 1f, 0.2f, 2.2f, 2f, 0.3f, 3.3f, 3f);
        assertEquals(expectedDefaultDenseFeatures, defaultDenseFeatures);
        // check the nf feature of default nodes
        List<Long> defaultNFKeys = nodes.getFeatures().getSpKvsOrThrow("nf").getKeys().getValueList();
        List<Long> expectedDefaultNFKeys = Arrays.asList(1L, 10L, 2L, 3L, 4L, 10L);
        assertEquals(expectedDefaultNFKeys, defaultNFKeys);
        List<Long> defaultNFValues = nodes.getFeatures().getSpKvsOrThrow("nf").getI64S().getValueList();
        List<Long> expectedDefaultNFValues = Arrays.asList(1L, 1L, 2L, 3L, 3L, 3L);
        assertEquals(expectedDefaultNFValues, defaultNFValues);
        List<Long> defaultNFLens = nodes.getFeatures().getSpKvsOrThrow("nf").getLens().getValueList();
        List<Long> expectedDefaultNFLens = Arrays.asList(2L, 3L, 6L);
        assertEquals(expectedDefaultNFLens, defaultNFLens);

        // check the ef feature of default edges
        List<Long> defaultEFKeys = edges.getFeatures().getSpKvsOrThrow("ef").getKeys().getValueList();
        List<Long> expectedDefaultEFKeys = Arrays.asList(1L, 2L, 9L, 2L, 3L, 10L);
        assertEquals(expectedDefaultEFKeys, defaultEFKeys);
        List<Float> defaultEFValues = edges.getFeatures().getSpKvsOrThrow("ef").getF32S().getValueList();
        List<Float> expectedDefaultEFValues = Arrays.asList(1.1f, 2.2f, 10.1f, 2.2f, 3.3f, 2.1f);
        assertEquals(expectedDefaultEFValues, defaultEFValues);
        List<Long> defaultEFLens = edges.getFeatures().getSpKvsOrThrow("ef").getLens().getValueList();
        List<Long> expectedDefaultEFLens = Arrays.asList(3L, 6L);
        assertEquals(expectedDefaultEFLens, defaultEFLens);
    }

    @Test
    public void testPpi2HopStructure() throws Exception {
        // load all ppi edges
        String edgePath = "/Users/lianxing/workplace/flat_v3/Alps/java/graph_embedding/ppi_edge_table.txt";
        Map<String, Set<String>> edgeSets = new HashMap<>();
        try (BufferedReader b = new BufferedReader(new FileReader(edgePath))) {
            String line;
            while ((line = b.readLine()) != null) {
                String[] cols = line.split(";");
                String node1 = cols[0];
                String node2 = cols[1];
                if (!edgeSets.containsKey(node1)) {
                    edgeSets.put(node1, new HashSet<>());
                }
                edgeSets.get(node1).add(node2);
            }
        }
        String filePath = "/Users/lianxing/workplace/spark-dev/AGL_OpenSource/agl/java/src/main/java/com/alipay/alps/flatv3/spark/graph_feature.txt";
        try (BufferedReader b = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = b.readLine()) != null) {
                String[] cols = line.split("\t");
                String nodeId = cols[0];
                String base64SerializedString = cols[1];
                Boolean isCompress = false;
                GraphFeature subGraph = null;
                try {
                    byte[] compressed = BaseEncoding.base64().decode(base64SerializedString);
                    if (isCompress) {
                        // GZIPInputStream gzipIn = null;
                        // byte[] a = null;
                        // try {
                        //     gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressed));
                        //     a = IOUtils.toByteArray(gzipIn);
                        // } catch (IOException e) {
                        // } finally {
                        //     IOUtils.closeQuietly(gzipIn);
                        // }
                        System.out.println("compressed.length:" + compressed.length);
                    }
                    subGraph = GraphFeature.parseFrom(compressed);
//                    System.out.println("subGraph:" + subGraph);
                } catch (Exception e) {
                    System.out.println("Exception:" + e.getMessage());
                    throw e;
                }
                Nodes nodes = subGraph.getNodesMap().get("default");
                int nodeCount = nodes.getNids().getStr().getValueCount();
                System.out.println("nodeId:"+nodeId + " subgraph:" + base64SerializedString.length() + " nodeCount:" + nodeCount);
                List<String> nodeStrs = new ArrayList<>();
                for (int i = 0; i < nodeCount; i++) {
                    String nodeID = new String(nodes.getNids().getStr().getValue(i).toByteArray());
                    nodeStrs.add(nodeID);
                }
                Edges edges = subGraph.getEdgesMap().get("default");
                List<Long> csrIndptr = edges.getCsr().getIndptr().getValueList();
                List<Long> nbrsIndices = edges.getCsr().getNbrsIndices().getValueList();
                Map<String, Set<String>> edgeStrs = new HashMap<>();
                long startIdx = 0;
                int maxlimit = -1;
                for (int i = 0; i < csrIndptr.size(); i++) {
                    if (startIdx < csrIndptr.get(i)) {
                        Set<String> node2Strs = new HashSet<>();
                        for (int j = (int)startIdx; j < csrIndptr.get(i); j++) {
                            String n2 = nodeStrs.get( nbrsIndices.get(j).intValue() );
                            node2Strs.add(n2);
                            // test that all edges appear in ppi edges
                            assertTrue(edgeSets.get(nodeStrs.get(i)).contains(n2));
                        }
                        maxlimit = Math.max(maxlimit, node2Strs.size());
                        edgeStrs.put(nodeStrs.get(i), node2Strs);
                    }
                    startIdx = csrIndptr.get(i);
                }
                Map<String, Set<String>> visitedEdgeStrs = new HashMap<>();
                assertTrue(maxlimit <= 50);
                Queue<String> queue = new LinkedList<>();
                Set<String> visitedNode1 = new HashSet<>();
                queue.add(nodeId);
                visitedNode1.add(nodeId);
                int depth = 0;
                while (!queue.isEmpty()) {
                    int layerSize = queue.size();
                    for (int i = 0; i < layerSize; i++) {
                        String n1 = queue.poll();
                        assertTrue(edgeStrs.containsKey(n1));
                        for (String n2 : edgeStrs.get(n1)) {
                            if (!visitedNode1.contains(n2)) {
                                queue.add(n2);
                                visitedNode1.add(n2);
                            }
                        }
                        visitedEdgeStrs.put(n1, edgeStrs.get(n1));
                    }
                    depth++;
                    if (depth >= 2) {
                        break;
                    }
                }
                // starting from root id, all nodes and edges from the subgraph can by reached using bfs within particular hops.
                assertEquals(visitedEdgeStrs, edgeStrs);
            }
        }
    }
}
