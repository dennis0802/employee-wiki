package com.development.hris.entities;

import java.util.Comparator;

public class OpenJobComparator implements Comparator<OpenJob> {
    @Override
    public int compare(OpenJob o1, OpenJob o2) {
       return o1.getPostDate().compareTo(o2.getPostDate());
    }
}
