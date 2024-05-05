package com.development.hris.entities;

import java.util.Comparator;

public class SiteUserComparator implements Comparator<SiteUser> {

    @Override
    public int compare(SiteUser user1, SiteUser user2) {
       return user1.getLastName().compareTo(user2.getLastName());
    }

}