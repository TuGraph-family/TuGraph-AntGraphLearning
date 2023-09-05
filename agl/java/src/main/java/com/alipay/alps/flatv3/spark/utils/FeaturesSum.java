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

package com.alipay.alps.flatv3.spark.utils;

import com.antfin.agl.proto.graph_feature.Features;
import com.antfin.agl.proto.graph_feature.Float64List;
import com.antfin.agl.proto.graph_feature.FloatList;
import com.antfin.agl.proto.graph_feature.Int64List;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.expressions.Aggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeaturesSum extends Aggregator<FeaturesWeight, String, String> {

  private static final Logger LOG = LoggerFactory.getLogger(FeaturesSum.class);

  @Override
  public Encoder<String> bufferEncoder() {
    return Encoders.STRING();
  }

  @Override
  public String finish(String reduction) {
    return reduction;
  }

  @Override
  public String merge(String b1, String b2) {
    String ret = "";
    try {
      ret = BaseEncoding.base64().encode(
          mergeFeature(Features.parseFrom(BaseEncoding.base64().decode(b1)),
              Features.parseFrom(BaseEncoding.base64().decode(b2))).toByteArray());
    } catch (InvalidProtocolBufferException e) {
      LOG.info("merge exception:" + Arrays.toString(e.getStackTrace()));
    }
    return ret;
  }

  @Override
  public Encoder<String> outputEncoder() {
    return Encoders.STRING();
  }

  @Override
  public String reduce(String b, FeaturesWeight a) {
    try {
      b = BaseEncoding.base64()
          .encode(mergeFeature(Features.parseFrom(BaseEncoding.base64().decode(b)),
              updateFeature(Features.parseFrom(BaseEncoding.base64().decode(a.getFeatureStr())),
                  a.getWeight())).toByteArray());
    } catch (InvalidProtocolBufferException e) {
      LOG.info("reduce exception:" + Arrays.toString(e.getStackTrace()));
    }
    return b;
  }

  @Override
  public String zero() {
    return "";
  }


  public static Features updateFeature(Features a, float w) {
    // check if w is close to 1.0
    if (Math.abs(w - 1.0f) < 1e-6) {
      return a;
    }
    Features.Builder builder = Features.newBuilder();
    for (Map.Entry<String, Features.DenseFeatures> entry : a.getDfsMap().entrySet()) {
      String key = entry.getKey();
      Features.DenseFeatures value = entry.getValue();
      Features.DenseFeatures.Builder dfBuilder = Features.DenseFeatures.newBuilder();
      dfBuilder.setDim(value.getDim());
      if (value.hasF32S()) {
        List<Float> f32s = new ArrayList<>();
        for (float f : value.getF32S().getValueList()) {
          f32s.add(f * w);
        }
        dfBuilder.setF32S(FloatList.newBuilder().addAllValue(f32s).build());
        builder.putDfs(key, dfBuilder.build());
      } else if (value.hasF64S()) {
        List<Double> f64s = new ArrayList<>();
        for (double f : value.getF64S().getValueList()) {
          f64s.add(f * w);
        }
        dfBuilder.setF64S(Float64List.newBuilder().addAllValue(f64s).build());
        builder.putDfs(key, dfBuilder.build());
      } else if (value.hasI64S()) {
        List<Long> i64s = new ArrayList<>();
        for (long f : value.getI64S().getValueList()) {
          i64s.add((long) (f * w));
        }
        dfBuilder.setI64S(Int64List.newBuilder().addAllValue(i64s).build());
        builder.putDfs(key, dfBuilder.build());
      } else {
        throw new RuntimeException(
            "not supported node feature type: " + value.getClass().getName());
      }
    }
    for (Map.Entry<String, Features.SparseKVFeatures> entry : a.getSpKvsMap().entrySet()) {
      String key = entry.getKey();
      Features.SparseKVFeatures value = entry.getValue();
      Features.SparseKVFeatures.Builder spBuilder = Features.SparseKVFeatures.newBuilder();
      spBuilder.setLens(value.getLens());
      spBuilder.setKeys(value.getKeys());
      if (value.hasF32S()) {
        List<Float> f32s = new ArrayList<>();
        for (float f : value.getF32S().getValueList()) {
          f32s.add(f * w);
        }
        spBuilder.setF32S(FloatList.newBuilder().addAllValue(f32s).build());
        builder.putSpKvs(key, spBuilder.build());
      } else if (value.hasF64S()) {
        List<Double> f64s = new ArrayList<>();
        for (double f : value.getF64S().getValueList()) {
          f64s.add(f * w);
        }
        spBuilder.setF64S(Float64List.newBuilder().addAllValue(f64s).build());
        builder.putSpKvs(key, spBuilder.build());
      } else if (value.hasI64S()) {
        List<Long> i64s = new ArrayList<>();
        for (long f : value.getI64S().getValueList()) {
          i64s.add((long) (f * w));
        }
        spBuilder.setI64S(Int64List.newBuilder().addAllValue(i64s).build());
        builder.putSpKvs(key, spBuilder.build());
      } else {
        throw new RuntimeException(
            "not supported node feature type: " + value.getClass().getName());
      }
    }
    return builder.build();
  }


  public static Features mergeFeature(Features a, Features b) {
    if (!a.isInitialized() || (a.getDfsMap().isEmpty() && a.getSpKvsMap().isEmpty())) {
      return b;
    }
    if (!b.isInitialized() || (b.getDfsMap().isEmpty() && b.getSpKvsMap().isEmpty())) {
      return a;
    }
    Features.Builder builder = Features.newBuilder();
    for (Map.Entry<String, Features.DenseFeatures> entry : a.getDfsMap().entrySet()) {
      String key = entry.getKey();
      Features.DenseFeatures value = entry.getValue();
      Features.DenseFeatures.Builder dfBuilder = Features.DenseFeatures.newBuilder();
      dfBuilder.setDim(value.getDim());
      if (value.hasF32S()) {
        List<Float> f32sFromA = a.getDfsMap().get(key).getF32S().getValueList();
        List<Float> f32sFromB = b.getDfsMap().get(key).getF32S().getValueList();
        List<Float> f32s = new ArrayList<>();
        for (int i = 0; i < value.getF32S().getValueCount(); i++) {
          f32s.add(f32sFromA.get(i) + f32sFromB.get(i));
        }
        dfBuilder.setF32S(FloatList.newBuilder().addAllValue(f32s).build());
        builder.putDfs(key, dfBuilder.build());
      } else if (value.hasF64S()) {
        List<Double> f64sFromA = a.getDfsMap().get(key).getF64S().getValueList();
        List<Double> f64sFromB = b.getDfsMap().get(key).getF64S().getValueList();
        List<Double> f64s = new ArrayList<>();
        for (int i = 0; i < value.getF64S().getValueCount(); i++) {
          f64s.add(f64sFromA.get(i) + f64sFromB.get(i));
        }
        dfBuilder.setF64S(Float64List.newBuilder().addAllValue(f64s).build());
        builder.putDfs(key, dfBuilder.build());
      } else if (value.hasI64S()) {
        List<Long> i64sFromA = a.getDfsMap().get(key).getI64S().getValueList();
        List<Long> i64sFromB = b.getDfsMap().get(key).getI64S().getValueList();
        List<Long> i64s = new ArrayList<>();
        for (int i = 0; i < value.getI64S().getValueCount(); i++) {
          i64s.add(i64sFromA.get(i) + i64sFromB.get(i));
        }
        dfBuilder.setI64S(Int64List.newBuilder().addAllValue(i64s).build());
        builder.putDfs(key, dfBuilder.build());
      } else {
        throw new RuntimeException(
            "not supported node feature type: " + value.getClass().getName());
      }
    }

    for (Map.Entry<String, Features.SparseKVFeatures> entry : a.getSpKvsMap().entrySet()) {
      String key = entry.getKey();
      Features.SparseKVFeatures value = entry.getValue();
      Features.SparseKVFeatures.Builder spBuilder = Features.SparseKVFeatures.newBuilder();
      spBuilder.setLens(value.getLens());
      List<Long> keysFromA = a.getSpKvsMap().get(key).getKeys().getValueList();
      List<Long> keysFromB = b.getSpKvsMap().get(key).getKeys().getValueList();
      List<Long> keysMerged = new ArrayList<>();
      if (value.hasF32S()) {
        List<Float> f32sFromA = a.getSpKvsMap().get(key).getF32S().getValueList();
        List<Float> f32sFromB = b.getSpKvsMap().get(key).getF32S().getValueList();
        List<Float> f32sMerged = mergeFloatList(keysMerged, keysFromA, f32sFromA, keysFromB,
            f32sFromB);
        spBuilder.setF32S(FloatList.newBuilder().addAllValue(f32sMerged).build());
        spBuilder.setKeys(Int64List.newBuilder().addAllValue(keysMerged).build());
        builder.putSpKvs(key, spBuilder.build());
      } else if (value.hasF64S()) {
        List<Double> f64sFromA = a.getSpKvsMap().get(key).getF64S().getValueList();
        List<Double> f64sFromB = b.getSpKvsMap().get(key).getF64S().getValueList();
        List<Double> f64sMerged = mergeFloatList(keysMerged, keysFromA, f64sFromA, keysFromB,
            f64sFromB);
        spBuilder.setF64S(Float64List.newBuilder().addAllValue(f64sMerged).build());
        spBuilder.setKeys(Int64List.newBuilder().addAllValue(keysMerged).build());
        builder.putSpKvs(key, spBuilder.build());
      } else if (value.hasI64S()) {
        List<Long> i64sFromA = a.getSpKvsMap().get(key).getI64S().getValueList();
        List<Long> i64sFromB = b.getSpKvsMap().get(key).getI64S().getValueList();
        List<Long> i64sMerged = mergeFloatList(keysMerged, keysFromA, i64sFromA, keysFromB,
            i64sFromB);
        spBuilder.setI64S(Int64List.newBuilder().addAllValue(i64sMerged).build());
        spBuilder.setKeys(Int64List.newBuilder().addAllValue(keysMerged).build());
        builder.putSpKvs(key, spBuilder.build());
      } else {
        throw new RuntimeException(
            "not supported node feature type: " + value.getClass().getName());
      }
    }
    return builder.build();
  }


  private static <T extends Number> List<T> mergeFloatList(List<Long> keysMerged,
      List<Long> keysFromA, List<T> valsFromA, List<Long> keysFromB, List<T> valsFromB) {
    List<T> f32sMerged = new ArrayList<>();
    int i = 0, j = 0;
    while (i < keysFromA.size() && j < keysFromB.size()) {
      if (keysFromA.get(i) < keysFromB.get(j)) {
        keysMerged.add(keysFromA.get(i));
        f32sMerged.add(valsFromA.get(i));
        i++;
      } else if (keysFromA.get(i) > keysFromB.get(j)) {
        keysMerged.add(keysFromB.get(j));
        f32sMerged.add(valsFromB.get(j));
        j++;
      } else {
        keysMerged.add(keysFromA.get(i));
        f32sMerged.add(add(valsFromA.get(i), valsFromB.get(j)));
        i++;
        j++;
      }
    }
    while (i < keysFromA.size()) {
      keysMerged.add(keysFromA.get(i));
      f32sMerged.add(valsFromA.get(i));
      i++;
    }
    while (j < keysFromB.size()) {
      keysMerged.add(keysFromB.get(j));
      f32sMerged.add(valsFromB.get(j));
      j++;
    }
    return f32sMerged;
  }

  private static <T extends Number> T add(T a, T b) {
    if (a instanceof Long && b instanceof Long) {
      return (T) Long.valueOf(a.longValue() + b.longValue());
    } else if (a instanceof Float && b instanceof Float) {
      return (T) Float.valueOf(a.floatValue() + b.floatValue());
    } else if (a instanceof Double && b instanceof Double) {
      return (T) Double.valueOf(a.doubleValue() + b.doubleValue());
    } else {
      throw new IllegalArgumentException("Unsupported number type");
    }
  }
}
