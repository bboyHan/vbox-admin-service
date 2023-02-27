package com.vbox.persistent.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class TokenInfo {

    private String token;
    private Integer id;
    private String username;
    private List<String> mIds;

}
