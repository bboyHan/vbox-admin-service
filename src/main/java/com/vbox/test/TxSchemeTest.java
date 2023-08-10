package com.vbox.test;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxSchemeTest {

    public static void main(String[] args) throws UnsupportedEncodingException {

        String payReqUrl = "https://m.tb.cn/h.5cYiNXF?tk=tFdpdvuoyMf";
        HttpResponse execute = HttpRequest.get(payReqUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();

        String html = execute.body();
//        m.taobao.com/i669010673510.htm
        // 使用正则表达式匹配包含"taobao.com"的那一行，并提取出相应的值
        Pattern pattern = Pattern.compile("var url = '([^']*)'");
        Matcher matcher = pattern.matcher(html);
        String tbTmpUrl = "";
        while (matcher.find()) {
            String match = matcher.group(0);
            if (match.contains("taobao.com")) {
                String value = matcher.group(1);
                System.out.println("匹配到的行：" + match);
                tbTmpUrl = value;
                System.out.println("提取的值：" + value);
            }
        }

        URL url = URLUtil.url(tbTmpUrl);
        Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
        String itemId = stringMap.get("id");
        System.out.println(itemId);
    }

    private static void jd() throws UnsupportedEncodingException {
        //        String resp = HttpRequest.get("http://101.89.120.162:555/api/pay?username=18210889498&amount=1000&cookie=sdo_dw_track=RmT7rL1euukxlRroUkngLw==; CAS_LOGIN_STATE=1; SECURE_CAS_LOGIN_STATE=1; nsessionid=3d6fdc99ce0d0ec4cc213df707f04a6e&proxy=49.69.228.201:40029")
//                .execute().body();

//        String payReqUrl = "https://m.tb.cn/h.50k8pfR?tk=j4hpdHAmPsE";
//        String payReqUrl = "https://m.tb.cn/h.50k8pfR?tk=j4hpdHAmPsE";
        String payReqUrl = "https://item.m.jd.com/product/10063946360171.html?&utm_source=iosapp&utm";

        HttpResponse execute = HttpRequest.get(payReqUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();

        String location = execute.header("Location");
        URL url = URLUtil.url(location);
        Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);

        String transUrl = stringMap.get("returnurl");

//        HttpResponse transExecute = HttpRequest.get(transUrl)
//                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
//                .execute();
//
//        String locationJD = transExecute.header("Location");
//
//        HttpResponse trans2Execute = HttpRequest.get(locationJD)
//                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
//                .execute();
//
//        System.out.println(trans2Execute);
        // 使用正则表达式提取skuid对应的值
//        Pattern pattern = Pattern.compile("\"skuId\":\"(\\d+)\"");
        Pattern pattern = Pattern.compile("/product/(\\d+)\\.html");
        Matcher matcher = pattern.matcher(transUrl);
        String skuId = "";
        if (matcher.find()) {
            skuId = matcher.group(1);
            System.out.println("skuId: " + skuId);
        } else {
            System.out.println("未找到skuId");
        }

        String schemaBody = "{\"sourceValue\":\"0_productDetail_97\",\"des\":\"productDetail\",\"skuId\":\" " + skuId + "\",\"category\":\"jump\",\"sourceType\":\"PCUBE_CHANNEL\"}";

        String schemaUrl = "openapp.jdmobile://virtual?params=" + URLEncoder.encode(schemaBody, "UTF-8");
        System.out.println(schemaUrl);
    }

    private static void douyin() {
        String payReqUrl = "https://v.douyin.com/iJr4qUsL/";

        HttpResponse execute = HttpRequest.get(payReqUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();

        System.out.println(execute);

        String location = execute.header("Location");

//        String dyDecodeUrl = URLDecoder.decode(location, "UTF-8");
        URL url = URLUtil.url(location);
        Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
        String detailSchema = stringMap.get("detail_schema");
        System.out.println(detailSchema);
        String replacedUrl = detailSchema.replace("sslocal://", "snssdk1128://");
        System.out.println(replacedUrl);
    }
}
