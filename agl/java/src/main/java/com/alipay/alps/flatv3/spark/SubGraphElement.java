package com.alipay.alps.flatv3.spark;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class SubGraphElement implements Serializable {
    @Getter
    @Setter
    private String seed;
    @Getter
    @Setter
    private String entryType;
    @Getter
    @Setter
    private String node1;
    @Getter
    @Setter
    private String node2;
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String feature;
    @Getter
    @Setter
    private String type;
    @Getter
    @Setter
    private Long other1Long;
    @Getter
    @Setter
    private String other1String;
    @Getter
    @Setter
    private Float other1Float;
    @Getter
    @Setter
    private Long other2Long;
    @Getter
    @Setter
    private String other2String;
    @Getter
    @Setter
    private Float other2Float;
    public SubGraphElement() {}
    public SubGraphElement(String seed, String entryType, String node1, String node2, String id, Object other1, Object other2) {
        this.seed = seed;
        this.entryType = entryType;
        this.node1 = node1;
        this.node2 = node2;
        this.id = id;
        if (other1 instanceof String) {
            this.other1String = (String) other1;
        } else if (other1 instanceof Long) {
            this.other1Long = (Long) other1;
        } else if (other1 instanceof Float) {
            this.other1Float = (Float) other1;
        }
        if (other2 instanceof String) {
            this.other2String = (String) other2;
        } else if (other2 instanceof Long) {
            this.other2Long = (Long) other2;
        } else if (other2 instanceof Float) {
            this.other2Float = (Float) other2;
        }
    }

    public SubGraphElement(String seed, String entryType, String node1, String node2, String id, String feature, String type) {
        this.seed = seed;
        this.entryType = entryType;
        this.node1 = node1;
        this.node2 = node2;
        this.id = id;
        this.feature = feature;
        this.type = type;
    }

    public String getSeed() {
        return seed;
    }

    public String getEntryType() {
        return entryType;
    }

    public String getNode1() {
        return node1;
    }

    public String getNode2() {
        return node2;
    }

    public String getID() {
        return id;
    }

    public String getFeature() {
        return feature;
    }

    public String getType() {
        return type;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public void setNode1(String node1) {
        this.node1 = node1;
    }

    public void setNode2(String node2) {
        this.node2 = node2;
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOther1String() {
        return other1String;
    }

    public String getOther2String() {
        return other2String;
    }
    public void setOther1String(String other1) {
        this.other1String = other1;
    }

    public void setOther2String(String other2) {
        this.other2String = other2;
    }

    public Float getOther1Float() {
        return other1Float;
    }

    public Float getOther2Float() {
        return other2Float;
    }
    public void setOther1Float(Float other1) {
        this.other1Float = other1;
    }

    public void setOther2Float(Float other2) {
        this.other2Float = other2;
    }

    public Long getOther1Long() {
        return other1Long;
    }

    public Long getOther2Long() {
        return other2Long;
    }
    public void setOther1Long(Long other1) {
        this.other1Long = other1;
    }

    public void setOther2Long(Long other2) {
        this.other2Long = other2;
    }

    public static Object getOther1(SubGraphElement subGraphElement) {
        if (subGraphElement.other1String != null) {
            return subGraphElement.other1String;
        } else if (subGraphElement.other1Long != null) {
            return subGraphElement.other1Long;
        } else if (subGraphElement.other1Float != null) {
            return subGraphElement.other1Float;
        }
        return null;
    }

    public static Object getOther2(SubGraphElement subGraphElement) {
        if (subGraphElement.other2String != null) {
            return subGraphElement.other2String;
        } else if (subGraphElement.other2Long != null) {
            return subGraphElement.other2Long;
        } else if (subGraphElement.other2Float != null) {
            return subGraphElement.other2Float;
        }
        return null;
    }
    
    public String toString() {
        return "SubGraphElement(seed=" + this.getSeed() + ", entryType=" + this.getEntryType() + ", node1=" + this.getNode1() + ", node2=" + this.getNode2() + ", id=" + this.getID() + ", feature=" + this.getFeature() + ", type=" + this.getType() ;
    }
}
