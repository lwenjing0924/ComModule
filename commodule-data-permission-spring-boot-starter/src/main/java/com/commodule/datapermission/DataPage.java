package com.commodule.datapermission;

import java.util.List;

public record DataPage<T>(List<T> records, long total) {

    public DataPage {
        records = List.copyOf(records);
    }
}

