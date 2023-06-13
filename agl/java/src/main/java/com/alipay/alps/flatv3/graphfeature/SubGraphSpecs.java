package com.alipay.alps.flatv3.graphfeature;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.antfin.agl.proto.graph_feature.BytesList;
import com.antfin.agl.proto.graph_feature.DType;
import com.antfin.agl.proto.graph_feature.Features;
import com.antfin.agl.proto.graph_feature.Float64List;
import com.antfin.agl.proto.graph_feature.FloatList;
import com.antfin.agl.proto.graph_feature.Int64List;
import com.antfin.agl.proto.graph_feature.SubGraphSpec;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SubGraphSpecs implements Serializable {
    private static final String DENSE = "dense";
    private static final String SPARSE_K = "sparse_k";
    private static final String SPARSE_KV = "sparse_kv";
    private static final String KV = "kv";

    private SubGraphSpec subGraphSpec;

    public SubGraphSpecs(String subgraphSpecJson) {
        JSONObject subgraphSpecJsonObj = JSON.parseObject(subgraphSpecJson);
        JSONArray nodeSpecJsonArray = subgraphSpecJsonObj.getJSONArray("node_spec");
        JSONArray edgeSpecJsonArray = subgraphSpecJsonObj.getJSONArray("edge_spec");

        SubGraphSpec.Builder subGraphSpecBuilder = SubGraphSpec.newBuilder();
        List<SubGraphSpec.NodeSpec> nodeSpecList = new ArrayList<>();
        for (int i = 0; i < nodeSpecJsonArray.size(); i++) {
            JSONObject nodeSpecJsonObj = nodeSpecJsonArray.getJSONObject(i);
            SubGraphSpec.NodeSpec.Builder nodeSpecBuilder = SubGraphSpec.NodeSpec.newBuilder();
            nodeSpecBuilder.setNodeName(nodeSpecJsonObj.getString("node_name"));
            nodeSpecBuilder.setIdType(getDtype(nodeSpecJsonObj.getString("id_type")));
            JSONArray nodeFeaturesJsonArray = nodeSpecJsonObj.getJSONArray("features");
            List<SubGraphSpec.FeatureSpec> featureSpecList = convertFeatureSpecList(nodeFeaturesJsonArray);
            nodeSpecBuilder.addAllFeatures(featureSpecList);
            nodeSpecList.add(nodeSpecBuilder.build());
        }
        subGraphSpecBuilder.addAllNodes(nodeSpecList);

        List<SubGraphSpec.EdgeSpec> edgeSpecList = new ArrayList<>();
        for (int i = 0; i < edgeSpecJsonArray.size(); i++) {
            JSONObject edgeSpecJsonObj = edgeSpecJsonArray.getJSONObject(i);
            SubGraphSpec.EdgeSpec.Builder edgeSpecBuilder = SubGraphSpec.EdgeSpec.newBuilder();
            edgeSpecBuilder.setEdgeName(edgeSpecJsonObj.getString("edge_name"));
            edgeSpecBuilder.setN1Name(edgeSpecJsonObj.getString("n1_name"));
            edgeSpecBuilder.setN2Name(edgeSpecJsonObj.getString("n2_name"));
            edgeSpecBuilder.setIdType(getDtype(edgeSpecJsonObj.getString("id_type")));
            JSONArray edgeFeaturesJsonArray = edgeSpecJsonObj.getJSONArray("features");
            List<SubGraphSpec.FeatureSpec> featureSpecList = convertFeatureSpecList(edgeFeaturesJsonArray);
            edgeSpecBuilder.addAllFeatures(featureSpecList);
            edgeSpecList.add(edgeSpecBuilder.build());
        }
        subGraphSpecBuilder.addAllEdges(edgeSpecList);
        subGraphSpec = subGraphSpecBuilder.build();
    }


    private List<SubGraphSpec.FeatureSpec> convertFeatureSpecList(JSONArray nodeFeaturesJsonArray) {
        List<SubGraphSpec.FeatureSpec> featureSpecList = new ArrayList<>();
        for (int j = 0; j < nodeFeaturesJsonArray.size(); j++) {
            JSONObject nodeFeatureJsonObj = nodeFeaturesJsonArray.getJSONObject(j);
            SubGraphSpec.FeatureSpec.Builder featureSpecBuilder = SubGraphSpec.FeatureSpec.newBuilder();
            featureSpecBuilder.setName(nodeFeatureJsonObj.getString("name"));
            featureSpecBuilder.setType(nodeFeatureJsonObj.getString("type"));
            featureSpecBuilder.setDim(nodeFeatureJsonObj.getInteger("dim"));
            if (nodeFeatureJsonObj.containsKey("key")) {
                featureSpecBuilder.setKeyDtype(getDtype((nodeFeatureJsonObj.getString("key"))));
            }
            featureSpecBuilder.setValueDtype(getDtype((nodeFeatureJsonObj.getString("value"))));
            featureSpecList.add(featureSpecBuilder.build());
        }
        return featureSpecList;
    }

    public List<String> getAllNodeTypes() {
        List<String> allNodeTypes = new ArrayList<>();
        for (SubGraphSpec.NodeSpec nodeSpec : subGraphSpec.getNodesList()) {
            allNodeTypes.add(nodeSpec.getNodeName());
        }
        return allNodeTypes;
    }

    public List<String> getAllEdgeTypes() {
        List<String> allEdgeTypes = new ArrayList<>();
        for (SubGraphSpec.EdgeSpec edgeSpec : subGraphSpec.getEdgesList()) {
            allEdgeTypes.add(edgeSpec.getEdgeName());
        }
        return allEdgeTypes;
    }

    public List<SubGraphSpec.EdgeSpec> getEdgeSpecList() {
        return subGraphSpec.getEdgesList();
    }

    public SubGraphSpec.EdgeSpec getEdgeSpec(String edgeType) {
        for (SubGraphSpec.EdgeSpec edgeSpec : subGraphSpec.getEdgesList()) {
            if (edgeSpec.getEdgeName().equals(edgeType)) {
                return edgeSpec;
            }
        }
        return null;
    }

    public SubGraphSpec.NodeSpec getNodeSpec(String nodeType) {
        for (SubGraphSpec.NodeSpec nodeSpec : subGraphSpec.getNodesList()) {
            if (nodeSpec.getNodeName().equals(nodeType)) {
                return nodeSpec;
            }
        }
        return null;
    }

    public String generateNodeFeaturesBase64(String featureStr, String nodeType) {
        SubGraphSpec.NodeSpec nodeSpec = getNodeSpec(nodeType);
        Features features = generateFeatures(featureStr, nodeSpec.getFeaturesList());
        return BaseEncoding.base64().encode(features.toByteArray());
    }

    public Features generateNodeFeatures(String featureStr, String nodeType) {
        SubGraphSpec.NodeSpec nodeSpec = getNodeSpec(nodeType);
        return generateFeatures(featureStr, nodeSpec.getFeaturesList());
    }

    public Features generateEdgeFeatures(String featureStr, String edgeType) {
        SubGraphSpec.EdgeSpec edgeSpec = getEdgeSpec(edgeType);
        return generateFeatures(featureStr, edgeSpec.getFeaturesList());
    }

    private Features generateFeatures(String rawFeatureList, List<SubGraphSpec.FeatureSpec> featureSpecList) {
        Features.Builder featuresBuilder = Features.newBuilder();
        String[] rawFeatureArray = rawFeatureList.split("\t");
        String featureSpliter = " ";
        if (rawFeatureList.contains(",")) {
            featureSpliter = ",";
        }
        for (int fi = 0; fi < featureSpecList.size(); fi++) {
            SubGraphSpec.FeatureSpec featureSpec = featureSpecList.get(fi);
            String rawFeature = rawFeatureArray[fi];
            rawFeature = rawFeature.trim();
            String values[] = rawFeature.split(featureSpliter);
            if (rawFeature.trim().isEmpty()) {
                values = new String[0];
            }

            String featureName = featureSpec.getName();
            String featureType = featureSpec.getType();
            switch (featureType.toLowerCase()) {
                case DENSE:
                    featuresBuilder.putDfs(featureName, generateDenseFeatures(values, featureSpec));
                    break;
                case SPARSE_KV:
                case KV:
                    featuresBuilder.putSpKvs(featureName, generateSpareKVFeatures(values, featureSpec));
                    break;
                case SPARSE_K:
                    featuresBuilder.putSpKs(featureName, generateSpareKFeatures(values, featureSpec));
                    break;
            }
        }
        return featuresBuilder.build();
    }

    private Features.DenseFeatures generateDenseFeatures(String values[], SubGraphSpec.FeatureSpec featureSpec) {
        int featureDim = featureSpec.getDim();
        DType valueDtype = featureSpec.getValueDtype();

        Features.DenseFeatures.Builder denseFeaturesBuilder = Features.DenseFeatures.newBuilder();
        denseFeaturesBuilder.setDim(featureDim);
        switch (valueDtype.getNumber()) {
            case DType.FLOAT_VALUE:
                FloatList.Builder f32sBuilder = FloatList.newBuilder();
                for (int i = 0; i < values.length; i++) {
                    f32sBuilder.addValue(Float.parseFloat(values[i]));
                }
                denseFeaturesBuilder.setF32S(f32sBuilder.build());
                break;
            case DType.DOUBLE_VALUE:
                Float64List.Builder f64sBuilder = Float64List.newBuilder();
                for (int i = 0; i < values.length; i++) {
                    f64sBuilder.addValue(Double.parseDouble(values[i]));
                }
                denseFeaturesBuilder.setF64S(f64sBuilder.build());
                break;
            case DType.INT64_VALUE:
                Int64List.Builder i64sBuilder = Int64List.newBuilder();
                for (int i = 0; i < values.length; i++) {
                    i64sBuilder.addValue(Long.parseLong(values[i]));
                }
                denseFeaturesBuilder.setI64S(i64sBuilder.build());
                break;
            case DType.STR_VALUE:
                BytesList.Builder rawsBuilder = BytesList.newBuilder();
                for (int i = 0; i < values.length; i++) {
                    rawsBuilder.addValue(ByteString.copyFrom(values[i].getBytes()));
                }
                denseFeaturesBuilder.setRaws(rawsBuilder.build());
                break;
            default:
                throw new RuntimeException("Unsupported value dtype: " + valueDtype);
        }
        return denseFeaturesBuilder.build();
    }

    private Features.SparseKVFeatures generateSpareKVFeatures(String values[], SubGraphSpec.FeatureSpec featureSpec) {
        DType valueDtype = featureSpec.getValueDtype();

        Features.SparseKVFeatures.Builder sparseKVFeaturesBuilder = Features.SparseKVFeatures.newBuilder();
        Int64List.Builder lensBuilder = Int64List.newBuilder();
        Int64List.Builder keysBuilder = Int64List.newBuilder();
        switch (valueDtype.getNumber()) {
            case DType.FLOAT_VALUE:
                FloatList.Builder f32sBuilder = FloatList.newBuilder();
                for (int i = 0; i < values.length; i++) {
                    String[] kv = values[i].split(":");
                    keysBuilder.addValue(Long.parseLong(kv[0]));
                    f32sBuilder.addValue(Float.parseFloat(kv[1]));
                }
                lensBuilder.addValue(values.length);
                sparseKVFeaturesBuilder.setLens(lensBuilder.build());
                sparseKVFeaturesBuilder.setKeys(keysBuilder.build());
                sparseKVFeaturesBuilder.setF32S(f32sBuilder.build());
                break;
            case DType.DOUBLE_VALUE:
                Float64List.Builder f64sBuilder = Float64List.newBuilder();
                for (int i = 0; i < values.length; i++) {
                    String[] kv = values[i].split(":");
                    keysBuilder.addValue(Long.parseLong(kv[0]));
                    f64sBuilder.addValue(Double.parseDouble(kv[1]));
                }
                lensBuilder.addValue(values.length);
                sparseKVFeaturesBuilder.setLens(lensBuilder.build());
                sparseKVFeaturesBuilder.setKeys(keysBuilder.build());
                sparseKVFeaturesBuilder.setF64S(f64sBuilder.build());
                break;
            case DType.INT64_VALUE:
                Int64List.Builder i64sBuilder = Int64List.newBuilder();
                for (int i = 0; i < values.length; i++) {
                    String[] kv = values[i].split(":");
                    keysBuilder.addValue(Long.parseLong(kv[0]));
                    i64sBuilder.addValue(Long.parseLong(kv[1]));
                }
                lensBuilder.addValue(values.length);
                sparseKVFeaturesBuilder.setLens(lensBuilder.build());
                sparseKVFeaturesBuilder.setKeys(keysBuilder.build());
                sparseKVFeaturesBuilder.setI64S(i64sBuilder.build());
                break;
            default:
                throw new RuntimeException("Unsupported value dtype: " + valueDtype);
        }
        return sparseKVFeaturesBuilder.build();
    }

    private Features.SparseKFeatures generateSpareKFeatures(String values[], SubGraphSpec.FeatureSpec featureSpec) {
        DType keyDtype = featureSpec.getKeyDtype();

        Features.SparseKFeatures.Builder sparseKFeaturesBuilder = Features.SparseKFeatures.newBuilder();
        Int64List.Builder lensBuilder = Int64List.newBuilder();
        Int64List.Builder keysBuilder = Int64List.newBuilder();
        switch (keyDtype.getNumber()) {
            case DType.INT64_VALUE:
                for (int i = 0; i < values.length; i++) {
                    keysBuilder.addValue(Long.parseLong(values[i]));
                }
                lensBuilder.addValue(values.length);
                sparseKFeaturesBuilder.setLens(lensBuilder.build());
                sparseKFeaturesBuilder.setI64S(keysBuilder.build());
                break;
            case DType.STR_VALUE:
                BytesList.Builder rawsBuilder = BytesList.newBuilder();
                for (int i = 0; i < values.length; i++) {
                    keysBuilder.addValue(Long.parseLong(values[i]));
                    rawsBuilder.addValue(ByteString.copyFrom(values[i].getBytes()));
                }
                lensBuilder.addValue(values.length);
                sparseKFeaturesBuilder.setLens(lensBuilder.build());
                sparseKFeaturesBuilder.setRaws(rawsBuilder.build());
                break;
            default:
                throw new RuntimeException("Unsupported key dtype: " + keyDtype);
        }
        return sparseKFeaturesBuilder.build();
    }

    private DType getDtype(String type) {
        switch (type.toLowerCase()) {
            case "int8":
                return DType.INT8;
            case "uint16":
                return DType.UINT16;
            case "int16":
                return DType.INT16;
            case "uint32":
                return DType.UINT32;
            case "int32":
                return DType.INT32;
            case "uint64":
                return DType.UINT64;
            case "int64":
                return DType.INT64;
            case "float":
            case "float32":
                return DType.FLOAT;
            case "double":
                return DType.DOUBLE;
            case "str":
            case "string":
                return DType.STR;
            case "uint8":
                return DType.UINT8;
            case "bf16":
                return DType.BF16;
            case "fp16":
                return DType.FP16;
            default:
                return DType.UNKNOWN;
        }
    }
}