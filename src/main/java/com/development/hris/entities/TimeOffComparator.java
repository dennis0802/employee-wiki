package com.development.hris.entities;

import java.util.Comparator;

public class TimeOffComparator implements Comparator<TimeOffRequest> {

    @Override
    public int compare(TimeOffRequest r1, TimeOffRequest r2) {
       return r1.getStartDate().compareTo(r2.getStartDate());
    }

}