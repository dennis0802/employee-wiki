package com.development.hris.entities;

import java.util.Comparator;

public class CustomWebAppElementComparator implements Comparator<CustomWebAppElement> {
    @Override
    public int compare(CustomWebAppElement c1, CustomWebAppElement c2) {
       return c1.getId().compareTo(c2.getId());
    }
}
