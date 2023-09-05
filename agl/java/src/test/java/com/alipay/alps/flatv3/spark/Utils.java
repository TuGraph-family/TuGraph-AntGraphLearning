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

package com.alipay.alps.flatv3.spark;

import com.antfin.agl.proto.graph_feature.GraphFeature;
import com.antfin.agl.proto.graph_feature.IDs;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;

public class Utils {

  public static GraphFeature parseGraphFeature(String base64SerializedString, boolean isCompress)
      throws InvalidProtocolBufferException {
    GraphFeature subGraph = null;
    try {
      byte[] compressed = BaseEncoding.base64().decode(base64SerializedString);
      if (isCompress) {
        GZIPInputStream gzipIn = null;
        try {
          gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressed));
          compressed = IOUtils.toByteArray(gzipIn);
        } catch (IOException e) {
        } finally {
          IOUtils.closeQuietly(gzipIn);
        }
      }
      subGraph = GraphFeature.parseFrom(compressed);
    } catch (Exception e) {
      throw e;
    }
    return subGraph;
  }

  public static List<String> getIdStrs(IDs ids) {
    List<ByteString> idBytes = ids.getStr().getValueList();
    List<String> idStrs = new ArrayList<>();
    for (int i = 0; i < idBytes.size(); i++) {
      idStrs.add(idBytes.get(i).toStringUtf8());
    }
    return idStrs;
  }

  public static List<String> getRootIdStrs(List<String> nodeIds, GraphFeature graphFeatureMessage) {
    List<String> rootNodes = new ArrayList<>();
    for (Long rootIdx : graphFeatureMessage.getRoot().getSubgraph().getIndicesMap().get("default")
        .getValueList()) {
      rootNodes.add(nodeIds.get(Math.toIntExact(rootIdx)));
    }
    return rootNodes;
  }

  public static ArrayList<String[]> readCSVGraphFeatureOutput(String outputPrefix,
      Map<String, Integer> columnIndex) {
    if (outputPrefix.startsWith("file:///")) {
      outputPrefix = outputPrefix.substring("file:///".length());
    }
    String pattern = outputPrefix + "/part-*";
    ArrayList<String[]> content = new ArrayList<>();
    try {
      // Get a list of files that match the specified pattern
      PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
      Path file = Files.walk(Paths.get(outputPrefix)).filter(pathMatcher::matches).findFirst()
          .get();

      // Read the file line by line
      try (BufferedReader reader = Files.newBufferedReader(file)) {
        String line;
        boolean skipFirstLine = true;
        while ((line = reader.readLine()) != null) {
          if (skipFirstLine) {
            skipFirstLine = false;
            String columns[] = line.trim().split(",");
            for (int i = 0; i < columns.length; i++) {
              columnIndex.put(columns[i], i);
            }
            continue; // Skip the first line
          }
          // Process the line as needed
          content.add(line.trim().split(","));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content;
  }
}
