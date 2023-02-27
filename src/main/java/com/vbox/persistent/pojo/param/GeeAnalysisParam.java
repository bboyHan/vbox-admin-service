package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.util.List;

@Data
public class GeeAnalysisParam {

    private String imgs;
    private List<String> ques;
    private String capType;

}
