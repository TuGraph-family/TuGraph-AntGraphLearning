package com.alipay.alps.flatv3.spark;

import java.io.Serializable;


public class FeaturesWeight implements Serializable {
    private String featureStr;
    private float weight;

    public FeaturesWeight() {
    }

    public String getFeatureStr() {
        return featureStr;
    }

    public void setFeatureStr(String featureStr) {
        this.featureStr = featureStr;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
