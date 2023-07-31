package com.vbox.test;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQTest {

    public static void main(String[] args) throws UnsupportedEncodingException {

        String param = "<form name=\"punchout_form\" method=\"post\" action=\"https://openapi.alipay.com/gateway.do?sign=dX9pUOpNt5OnpBx%2BhZ3kkzixkegdELFJvuy%2BDRq707xqkX5oWjDmXp4wwp%2Bx7qUUgkdbDfthlqakjxK2POEU0DQ%2F2a0JpuowmGaP5ntVs5Hsk5dPj2UjlS4Qf9UN8W8Lb6ICUVXxu%2BdNZq7ZwsAJinYgfl%2FJPKqL6MUtu0N9xGcNr2UDA4qCc6Lz%2Fz3YPVIjvi5lCq0NT2OttAFCtAKmsHJKeT8ZRT%2BUq0Hdx5Gre47VYDkjMttbXiY97WSJMJdUODXG972gO6hISjbHO9hmirkvX1lhCgp9GgmM5BtoX%2BZCS7daGaR04lBP1WwpjAlwM95XMIpPjXIuUNhf%2BBhQ5A%3D%3D&timestamp=2023-07-30+23%3A22%3A09&sign_type=RSA2&notify_url=http%3A%2F%2Fpeak.changyou.com%2Fservlet%2FAliPayQrRsa2NotifyAction&charset=UTF-8&app_id=2018082461110402&method=alipay.trade.page.pay&version=1.0&alipay_sdk=alipay-sdk-java-3.0.52.ALL&format=json\">\n" +
                "    <input type=\"hidden\" name=\"biz_content\" value=\"{&quot;out_trade_no&quot;:&quot;19423616907305292246&quot;,&quot;product_code&quot;:&quot;FAST_INSTANT_TRADE_PAY&quot;,&quot;total_amount&quot;:&quot;10.00&quot;,&quot;subject&quot;:&quot;200游戏点数&quot;,&quot;body&quot;:&quot;200游戏点数&quot;,&quot;qr_pay_mode&quot;:&quot;4&quot;,&quot;qrcode_width&quot;:148,&quot;timeout_express&quot;:&quot;1d&quot;,&quot;business_params&quot;:{&quot;mcCreateTradeIp&quot;:&quot;42.57.251.225&quot;}}\">\n" +
                "    <input type=\"submit\" value=\"立即支付\" style=\"display:none\">\n" +
                "</form>\n" +
                "<script>\n" +
                "    document.forms[0].submit();\n" +
                "</script>";

        // 获取form标签中的action属性值
        Pattern actionPattern = Pattern.compile("<form[^>]*action=\"([^\"]*)\"");
        Matcher actionMatcher = actionPattern.matcher(param);
        String resource_url = "";
        String biz = "";
        if (actionMatcher.find()) {
            String actionValue = actionMatcher.group(1);
            System.out.println("Action Value: " + actionValue);

            resource_url = actionValue;
        }

        // 获取input标签中name为biz_content的value值
        Pattern inputPattern = Pattern.compile("<input[^>]*name=\"biz_content\"[^>]*value=\"([^\"]*)\"");
        Matcher inputMatcher = inputPattern.matcher(param);
        if (inputMatcher.find()) {
            String bizContentValue = inputMatcher.group(1);
            System.out.println("biz_content Value: " + bizContentValue);
            bizContentValue = bizContentValue.replaceAll("&quot;", "\"");
            System.out.println(bizContentValue);
//            JSONObject jsonObject = JSONObject.parseObject(bizContentValue);
//            jsonObject.remove("business_params");
//            bizContentValue = jsonObject.toJSONString();

            biz = URLEncoder.encode(bizContentValue, "UTF-8");
            System.out.println("biz_content --> Value: " + biz);
        }

//        String resource_url = "https://openapi.alipay.com/gateway.do?sign=LKDK%2FW8bpfP7y8Weht1RM4M%2Fj%2B%2B5XhwJXoY30U7LMXs8MqJgt3aEiWeNvc5LPGlxnRQc5YhRz46QRWN3ffd8R01jT8FW2MZ9ekB0%2FpeCPO9COA1g%2B8MLo%2FJXDnRYbJW5CE6PiCJp1Tb80dTvRL7LMx45Hevx3XXCcmiOeh%2FsPT8gmbbcq%2BdjwZZFTmoQ6joN0mtb%2FUc8XikJp%2BlJ5%2FH1QliAgykoRVIVpTm5yDkPfZQcUl%2FlifXPLquF9298tKa0FzXAZ9AfEZmdqnGvPx57ug9FhaznA2xZd8yOb1kvS8OhPs78x5xUi%2FZ2xJlMIRt%2FeT7LVw%2Br4UTqDeH7YowlEA%3D%3D&timestamp=2023-07-27+17%3A14%3A59&sign_type=RSA2&notify_url=http%3A%2F%2Fpeak.changyou.com%2Fservlet%2FAliPayQrRsa2NotifyAction&charset=UTF-8&app_id=2018082461110402&method=alipay.trade.page.pay&version=1.0&alipay_sdk=alipay-sdk-java-3.0.52.ALL&format=json";

//        URL url = URLUtil.url(resource_url);
//        Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
//        Map<String, Object> objectObjectSortedMap = new HashMap<>(stringMap);

//        String biz = "%7B%22out_trade_no%22%3A%22194236169045386851842%22%2C%22product_code%22%3A%22FAST_INSTANT_TRADE_PAY%22%2C%22total_amount%22%3A%22100.00%22%2C%22subject%22%3A%222000%E6%B8%B8%E6%88%8F%E7%82%B9%E6%95%B0%22%2C%22body%22%3A%222000%E6%B8%B8%E6%88%8F%E7%82%B9%E6%95%B0%22%2C%22qr_pay_mode%22%3A%224%22%2C%22qrcode_width%22%3A148%2C%22timeout_express%22%3A%221d%22%2C%22business_params%22%3A%7B%22mcCreateTradeIp%22%3A%22111.207.123.57%22%7D%7D";
//        String b = "{\"out_trade_no\":\"194236169044929987396\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\",\"total_amount\":\"100.00\",\"subject\":\"2000游戏点数\",\"body\":\"2000游戏点数\",\"qr_pay_mode\":\"4\",\"qrcode_width\":148,\"timeout_express\":\"1d\",\"business_params\":{\"mcCreateTradeIp\":\"111.207.123.85\"}}";
//        objectObjectSortedMap.put("biz_content", biz);
//        HttpResponse execute = HttpRequest.post("https://openapi.alipay.com/gateway.do?sign=HcLrpMauyHYdikXoODfpc8uVA%2B7YQpTCMZm1a94MuuI6NkEA9wpTNJPchh9ydv6DWefvn0tcZi6LDY%2FyVRUMgMJV6T1MpRwt86%2FaFE8izm6dCLsCn%2B4V1XCscavq4kQkB%2BC1yIumZmcJOjo%2BMv%2FtClgV19%2FzayE3CU45T7ipky21WNY53wsX%2F5g2TnMm39WP9hyTyP4SIhLnQbgyLibT46XjX1n%2FYexOkeJeMr5iBP3RzK5uPxHQuHlFcYMbhYjNBrsJ6GdmTVQuVwvD%2BCQwq8h9hhB5zTeS5%2B148nbLy4jAOJIYHNYNd2GaLHlWiEClnpzCqQzsPWY5EDiO8kWS%2Fw%3D%3D&timestamp=2023-07-27+18%3A31%3A08&sign_type=RSA2&notify_url=http%3A%2F%2Fpeak.changyou.com%2Fservlet%2FAliPayQrRsa2NotifyAction&charset=UTF-8&app_id=2018082461110402&method=alipay.trade.page.pay&version=1.0&alipay_sdk=alipay-sdk-java-3.0.52.ALL&format=json&biz_content=%7B%22out_trade_no%22%3A%22194236169045386851842%22%2C%22product_code%22%3A%22FAST_INSTANT_TRADE_PAY%22%2C%22total_amount%22%3A%22100.00%22%2C%22subject%22%3A%222000%E6%B8%B8%E6%88%8F%E7%82%B9%E6%95%B0%22%2C%22body%22%3A%222000%E6%B8%B8%E6%88%8F%E7%82%B9%E6%95%B0%22%2C%22qr_pay_mode%22%3A%224%22%2C%22qrcode_width%22%3A148%2C%22timeout_express%22%3A%221d%22%2C%22business_params%22%3A%7B%22mcCreateTradeIp%22%3A%22111.207.123.57%22%7D%7D")
//        HttpResponse execute = HttpRequest.post("https://openapi.alipay.com/gateway.do?sign=HcLrpMauyHYdikXoODfpc8uVA%2B7YQpTCMZm1a94MuuI6NkEA9wpTNJPchh9ydv6DWefvn0tcZi6LDY%2FyVRUMgMJV6T1MpRwt86%2FaFE8izm6dCLsCn%2B4V1XCscavq4kQkB%2BC1yIumZmcJOjo%2BMv%2FtClgV19%2FzayE3CU45T7ipky21WNY53wsX%2F5g2TnMm39WP9hyTyP4SIhLnQbgyLibT46XjX1n%2FYexOkeJeMr5iBP3RzK5uPxHQuHlFcYMbhYjNBrsJ6GdmTVQuVwvD%2BCQwq8h9hhB5zTeS5%2B148nbLy4jAOJIYHNYNd2GaLHlWiEClnpzCqQzsPWY5EDiO8kWS%2Fw%3D%3D&timestamp=2023-07-27+18%3A31%3A08&sign_type=RSA2&notify_url=http%3A%2F%2Fpeak.changyou.com%2Fservlet%2FAliPayQrRsa2NotifyAction&charset=UTF-8&app_id=2018082461110402&method=alipay.trade.page.pay&version=1.0&alipay_sdk=alipay-sdk-java-3.0.52.ALL&format=json&biz_content=%7B%22out_trade_no%22%3A%22194236169045386851842%22%2C%22product_code%22%3A%22FAST_INSTANT_TRADE_PAY%22%2C%22total_amount%22%3A%22100.00%22%2C%22subject%22%3A%222000%E6%B8%B8%E6%88%8F%E7%82%B9%E6%95%B0%22%2C%22body%22%3A%222000%E6%B8%B8%E6%88%8F%E7%82%B9%E6%95%B0%22%2C%22qr_pay_mode%22%3A%224%22%2C%22qrcode_width%22%3A148%2C%22timeout_express%22%3A%221d%22%2C%22business_params%22%3A%7B%22mcCreateTradeIp%22%3A%22111.207.123.57%22%7D%7D")
        String url = resource_url + "&biz_content=" + biz;
        System.out.println(url);
        HttpResponse execute = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute();

        String location = execute.header("Location");
        System.out.println(location);

        HttpResponse cash = HttpRequest.get(location)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute();

        String ca = cash.header("Location");
        System.out.println(ca);

    }
}
