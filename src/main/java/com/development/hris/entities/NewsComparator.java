package com.development.hris.entities;

import java.util.Comparator;

public class NewsComparator implements Comparator<News> {
    @Override
    public int compare(News n1, News n2) {
       return n1.getPostDate().compareTo(n2.getPostDate());
    }
}
