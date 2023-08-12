package com.vbox.test;

import com.alibaba.fastjson2.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: lianxiaobao
 * Date: 2023/8/12
 * Time: 12:20
 */
public class JsonTest {
    public static void main(String[] args) {
        String jsonStr = "{\"shopRemark\":\"小a\",\"channel\":\"tx_tb\",\"money-0\":\"200\",\"address-0\":\"地址1\",\"money-1\":\"50\",\"address-1\":\"地址2\"}";

        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        String keys = "money";
        JSONObject result = new JSONObject();
        int cnt = 0;
        for (String key : jsonObject.keySet()) {
            if (key.contains(keys)) {
                String index = key.substring(6); // 提取索引
                String moneyKey = "money-" + index;
                String addressKey = "address-" + index;
                String addressValue = jsonObject.getString(addressKey);
                result.put(moneyKey, jsonObject.get(moneyKey));
                result.put(addressKey, addressValue);
                cnt ++;
            }
        }

        System.out.println(result.toJSONString());
        System.out.println(cnt);
    }

}
