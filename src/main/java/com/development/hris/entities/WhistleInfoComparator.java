package com.development.hris.entities;

import java.util.Comparator;

public class WhistleInfoComparator implements Comparator<WhistleInfo> {
    @Override
    public int compare(WhistleInfo w1, WhistleInfo w2) {
       return w1.getPostDate().compareTo(w2.getPostDate());
    }
}
