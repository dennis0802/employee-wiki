package com.development.hris.entities;

import java.util.Comparator;

public class PayrollDataComparator implements Comparator<PayrollData> {
    @Override
    public int compare(PayrollData data1, PayrollData data2) {
       return data1.getStartDate().compareTo(data2.getStartDate());
    }
}
