package com.vbox.persistent.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayerInfo {

    private String pub;
    private String account;

    public static boolean valid(PayerInfo source, PayerInfo target) {
        return source.getAccount().equals(target.getAccount())
                && source.getPub().equals(target.getPub());
    }

}
