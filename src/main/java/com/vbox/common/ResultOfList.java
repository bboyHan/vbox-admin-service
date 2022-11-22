package com.vbox.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResultOfList<R> {

    /**
     * data
     */
    private R items;

    /**
     * total
     */
    private int total;

    public ResultOfList() {
    }

    public ResultOfList(R items, int total) {
        this.items = items;
        this.total = total;
    }

    public static ResultOfList okList(Object items) {
        if (items instanceof List) {
            ResultOfList rs = new ResultOfList(items, ((List<?>)items).size());
            return rs;
        }
        return new ResultOfList(new ArrayList<>(), 0);
    }
}
