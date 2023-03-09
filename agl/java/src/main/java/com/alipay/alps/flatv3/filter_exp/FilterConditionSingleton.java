package com.alipay.alps.flatv3.filter_exp;

import java.util.ArrayList;

// FilterConditionSingleton is a singleton class used to store an ArrayList of ArrayList of Expression objects
public class FilterConditionSingleton {
    // static instance of the class
    private static FilterConditionSingleton instance = null;
    // ArrayList of ArrayList of Expression objects
    public ArrayList<ArrayList<Expression>> unionJoinFilters = new ArrayList<ArrayList<Expression>>();
    private FilterConditionSingleton(){}

    public static FilterConditionSingleton getInstance(){
        if(instance == null){
            instance = new FilterConditionSingleton();
        }
        return instance;
    }

    public ArrayList<ArrayList<Expression>> getUnionJoinFilters() {
        return this.unionJoinFilters;
    }

    public void setUnionJoinFilters(ArrayList<ArrayList<Expression>> unionJoinFilters) {
        this.unionJoinFilters = unionJoinFilters;
    }
}
