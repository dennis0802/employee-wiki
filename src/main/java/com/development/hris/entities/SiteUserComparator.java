package com.development.hris.entities;

import java.util.Comparator;

public class SiteUserComparator implements Comparator<SiteUser> {

    @Override
    public int compare(SiteUser user1, SiteUser user2) {
       return Integer.compare(user1.getId().intValue(), user2.getId().intValue());
    }

}