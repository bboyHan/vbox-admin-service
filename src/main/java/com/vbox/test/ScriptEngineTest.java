package com.vbox.test;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.ExpireQueue;
import com.vbox.common.util.CommonUtil;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.persistent.pojo.param.GeeProdCodeParam;
import com.vbox.persistent.pojo.param.OrderCreateParam;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jodd.util.StringUtil;
import org.lionsoul.ip2region.xdb.Searcher;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 直接调用js代码
 */
public class ScriptEngineTest {
    public static void main(String[] args) throws Exception {
//        String ck = "xoyokey=DdG6mzDsGH3gkZ%3D%26%26NkcjaQ%3D%26%26c8_au%3D%26%26jrW%3DGm1GHmm3sm%26_n8c6sGg%26%26u7_cGmGmm%26naG.1Hg1zD%26p%3Dma%3D3rG.sa%261m3ojkHozs.%26onujsG3Hm8%3Dua%26_63DGG3jHN%3D; expires=Mon, 08-May-2023 12:07:38 GMT; path=/; domain=.xoyo.com; httponly";
//
//
////        jx_ht(ck);
////        jd_h5();
////        HttpResponse execute = HttpRequest.post("https://wepay.jd.com/jdpay/payIndex?tradeNum=10016819598538488821&orderId=202304201104142018310975998184&key=2d152b72bc98b1739389182db9830ad89a11fd273122bba75c955472535b85646e438a9a82795ce298d0d23ba1b92e5e3962822ef0fec11fda7a7b97f3dcc0ac")
////        HttpResponse execute = HttpRequest.post("https://wepay.jd.com/jdpay/login?key=10016819598538488821_202304201104142018310975998184")
////                .execute();
//
//        String resp = HttpRequest.get("https://pf-api.xoyo.com/passport/user_api/get_info")
//                .cookie(ck)
//                .execute().body();
//        System.out.println(resp);
        test5();
    }

    private static void jd_h5() {
        String jdPay = "https://wepay.jd.com/jdpay/payIndex?tradeNum=10016819571809217184&orderId=202304201019412017180320234167&key=ccac8dd256b18ba75d7dac8ea768e385325c2391f79c08b47a62624460d5f90f0aa5ee605846ffc396e2d08390d85c9ec4bbdef6b8527f746a56f9bd72c66a32";
        URL urlPay = URLUtil.url(jdPay);
        Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
        Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);
        HttpResponse accept = HttpRequest.post("https://wepay.jd.com/jdpay/payIndex")
                .header("Accept", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                .setFollowRedirects(false)
                .form(urlPayMap)
                .execute();
        String location = accept.header("Location");
        System.out.println(location);

        jdPay = location;
        urlPay = URLUtil.url(jdPay);
        urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
        urlPayMap = new HashMap<>(urlPayStringMap);
        String qrHtml = HttpRequest.post("https://wepay.jd.com/jdpay/login")
                .header("Accept", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                        .header("User-Agent", userAgent)
                .form(urlPayMap)
                .setFollowRedirects(false)
                .execute().body();
        String subBox = qrHtml.substring(qrHtml.indexOf("qrUrl=") + 6);
        jdPay = subBox.substring(0, subBox.indexOf("\""));
        System.out.println(jdPay);
    }

    private static void jx_ht(String ck) {
        String body = HttpRequest.get("https://ws.xoyo.com/jx3/groupbuying221123/create_order")
                .form("zone_id", "z05")
                .form("order_type", "1")
                .form("zone_name", URLEncoder.encode("双线一区（点卡）"))
                .form("platform", "pc")
                .form("callback", "__xfe11")
                .cookie(ck)
                .execute().body();


        System.out.println(body);
        String json = parseGeeJson(body);
        System.out.println("=============================");

        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONObject data = jsonObject.getJSONObject("data");
        String orderNo = data.getString("order_no");
        String payUrl = data.getString("pay_url");

        URL url = URLUtil.url(payUrl);
        Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
        String dStr = stringMap.get("data");
        System.out.println(dStr);
        System.out.println("=============================");

        String body2 = HttpRequest.get("https://pay-pf-api.xoyo.com/pay/store_api/get_order_id")
                .cookie(ck)                .form("data",dStr)

                .form("callback", "jsonp_13f3a339b089d90")
                .execute()
                .body();

        System.out.println(body2);
        String json2 = parseGeeJson(body2);
        JSONObject jsonObject2 = JSONObject.parseObject(json2);
        JSONObject data2 = jsonObject2.getJSONObject("data");
        System.out.println("=============================");

        String orderId = data2.getString("order_id");
        System.out.println(orderId);
        System.out.println("=============================");

        String body3 = HttpRequest.get("https://pay-pf-api.xoyo.com/pay/store_api/recharge_information")
//                .cookie("xoyokey=%5EQwClfN%3D%26%21WS-%3DC7zCf77lcl7%26%26N5rxxWg_Cw7C7Cllu%265g%3Dzf77%26%26N5rxWfwl.C%26%26rum%3Df.CNWf%21Cp7ClS%267uU7%3DW.%21mCgj587cuEcC_l%26l%3Dl%21m_C; expires=Tue, 18-Apr-2023 13:17:05 GMT; Max-Age=3600; path=/; domain=.xoyo.com; httponly")
                .form("game", "jx3")
                .form("channel", "weixin")
                .form("order_id", URLEncoder.encode(orderId))
                .form("recharge_source", "12")
                .form("callback", "jsonp_24853f8c034b6a0")
                .execute()
                .body();

        System.out.println(body3);
        System.out.println("=============================");
//        String json3 = parseGeeJson(body3);
//        JSONObject jsonObject3 = JSONObject.parseObject(json3);
//        JSONObject data3 = jsonObject3.getJSONObject("data");
//
//        System.out.println(data3);
//        System.out.println("=============================");

//        String sec = HttpRequest.get("http://localhost:8080/api/test/test")
//                .execute().body();
//        System.out.println(sec);
////
//        JSONObject secObj = JSONObject.parseObject(sec);
//        JSONObject result = secObj.getJSONObject("result");
        Map<String, String> m = new HashMap<>();
        m.put("game", "jx3");
        m.put("channel", "alipay_qr");
        m.put("recharge_num", "1");
        m.put("order_id", orderId);
        m.put("recharge_source", "12");
//        m.put("captcha_id", "a7c9ab026dc4366066e4aaad573dce02");
//        m.put("lot_number", "69b75237c8bf4540a6b6149751bc2b8d");
//        m.put("pass_token", "a9f041fa8bb2ac1b1473e5a93116f0bf588e37ac23088d34ad753ac5bfcc35fa");
//        m.put("gen_time", "1681825596");
//        m.put("captcha_output", "662a-teKeUpl5FxeT6YiibfEhw2ENSLfnQMoUl5uCfGLR-JWKspOZSHq_9jyaR54mh0SlYyjWXL9-bUzUamqzpsqfEDlTn3kwGgP7K5PhygKXtzB-fou6KU4h17I19N4sGtFBbR6rbOSn8t1ZF0MPt_t1-px4xqv7FwgZVs1OG_yGYDETi3g85JK5dXhH4UeDLAaNvffc7ahYiZ2M5UNjyUkmnfCig5xchXd9IlRyB-FMwc-OMI4gYNQJPgnD1buHXVpHhdZidlaVWGApMEWZxzwfeyIgJNo8WAIjKW9BIB2A0PqW4Y8O24lOWZy7dl--WtmyNO10lhg05NPnQWj5YtlOCnq1-Ww2dWCjHt3aOndj7hqQv5HoPPWmW_sLj1SfgMRcZ1iyp6_So6BGy3Q2ftBY5gT9PymCpG6A6yxZdvHF0mYtqfEJD1P8kNTkuNyZdYfnbCDvGa9VBt3n16Au0lWAMXFmfNRTPqNiqvLkJYtTq91EoCcjJ-tuLMM7PCACvgZPkzQW7GRlA9XrT9kaZWWkpzwJHlV0kBaFAZvUXMeIbrI1e0zyJh1wpmn8ska3hOvTvW-obfACLESqf71NA%3D%3D");
        String s = JSON.toJSONString(m);
        System.out.println(s);

        String resp = HttpRequest.get("https://pay-pf-api.xoyo.com/pay/store_api/create_order")
                .body(s)
                .execute().body();
        System.out.println(resp);
//        JSONObject j = JSONObject.parseObject(resp);
//        JSONObject datass = j.getJSONObject("data");
//        String resource_url = datass.getString("resource_url");
//        HttpResponse execute = HttpRequest.get(resource_url)
//                .contentType("application/x-www-form-urlencoded")
//                .header("X-Requested-With", "com.seasun.gamemgr")
//                .header("Origin", "https://m.xoyo.com")
//                .header("Referer", "https://m.xoyo.com")
//                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                .timeout(5000)
//                .execute();
//        String aliGateway = execute.header("Location");
//        System.out.println("alipay url 一次修正: "+ aliGateway);
//        HttpResponse cashierExecute = HttpRequest.get(aliGateway)
//                .contentType("application/x-www-form-urlencoded")
//                .header("X-Requested-With", "com.seasun.gamemgr")
//                .header("Origin", "https://m.xoyo.com")
//                .header("Referer", "https://m.xoyo.com")
//                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                .timeout(5000)
//                .execute();
//        String cashier = cashierExecute.header("Location");
//        System.out.println("alipay url 修正 后 pay url: {}" + cashier);
    }

    public static String parseGeeJson(String resp) {
        int startIndex = resp.indexOf("(");
        int endIndex = resp.lastIndexOf(")");
        String json = resp.substring(startIndex + 1, endIndex);
        return json;
    }

    private static void queueTest() throws InterruptedException {
        ExpireQueue<String> queue = new ExpireQueue<>();
        for (int i = 0; i < 100; i++) {
            queue.add(i + "");
        }

        System.out.println(queue.size());
        System.out.println("======================");
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println("======================");

        System.out.println(queue.size());

        System.out.println("======================");
        for (int i = 100; i < 200; i++) {
            queue.add(i + "");
        }
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println("======================");
        System.out.println(queue.size());

        System.out.println("===========sleep===========");
        Thread.sleep(60000);
        System.out.println(queue.size());

        System.out.println("===========sleep end===========");
        Thread.sleep(3000);
        System.out.println(queue.size());
//        String url = "      if(is_postmsg==\"1\")\n" +
//                "                {\n" +
//                "                    parent.postMessage(JSON.stringify({\n" +
//                "                        action : \"send_deeplink\",\n" +
//                "                        data : {\n" +
//                "                            deeplink : \"weixin://wap/pay?prepayid%3Dwx092048332302565f1ed2cc24f9739b0000&package=3170794954&noncestr=1681044546&sign=9c0cab0fc5e04b306c6109d1a8fb0061\"\n" +
//                "                        }\n" +
//                "                    }), \"\");\n" +
//                "                }\n" +
//                "                else\n" +
//                "                {\n" +
//                "                    var url=\"weixin://wap/pay?prepayid%3Dwx092048332302565f1ed2cc24f9739b0000&package=3170794954&noncestr=1681044546&sign=9c0cab0fc5e04b306c6109d1a8fb0061\";\n" +
//                "                    var redirect_url=\"https://m.xoyo.com/#/order-status?vouch_code=90016810445130668682&way=weixin_mobile&order_id=98e8f8fcc8426678adb01736a82f94e5&item=jx3\";\n" +
//                "                    top.location.href=url;\n" +
//                "\n" +
//                "                    if(redirect_url)\n" +
//                "                    {\n" +
//                "                        setTimeout(";
//
//        if (url.contains("weixin://wap")) {
//            String substring = url.substring(url.indexOf("weixin://wap"), url.indexOf("&sign=") + 38);
//            System.out.println(substring);
//        }

//        String a = "100.00";
//        int i = NumberUtil.parseInt(a);
//        System.out.print(i);

//        String body = HttpRequest.get("https://mobile.huashengdaili.com/servers.php?session=U216f946c0315205246--3b78a97ba1bd30781f9e769942c562b8&time=1&count=1&type=text&only=1&pw=no&protocol=http&ip_type=direct&province=" + 610000)
//        String body = HttpRequest.get("http://v2.api.juliangip.com/dynamic/getips?area=三明&num=1&pt=1&result_type=text&split=1&trade_no=1102019502692322&sign=0dd65623a208cf5b1d35d3787cf1c017")
//                .setHttpProxy("119.96.108.78", 10249)
//                .basicProxyAuth("wj0217", "123123")
//                .header("Content-type", "application/json")
//                .execute().body();
        //http://v2.api.juliangip.com/dynamic/getips?area=天津&filter=1&num=1&pt=1&result_type=text&split=1&trade_no=1102019502692322&sign=9c26f7afb42d1a33fcd243818b143453
//        String s = "星际二省";
////        String s = "星际二区";
//        if (s.contains("省")) {
//            int a = s.indexOf("省");
//            String substring = s.substring(0, a);
//            System.out.println(substring);
//        }else if (s.contains("区")) {
//            String substring = s.substring(0, 2);
//            System.out.println(substring);
//        }

        /*SortedMap<String, String> map = new TreeMap<>();
        map.put("trade_no", "1102019502692322");
        map.put("area", "安康");
        map.put("filter", "1");
        map.put("pt", "1");
        map.put("num", "1");
        map.put("result_type", "text");
        String s = encodeSign(map, "60bfa08147244afca968b0360f02d067");
        map.put("sign", s);
        Map<String, Object> m = new HashMap<>(map);
        String body = HttpRequest.get("http://v2.api.juliangip.com/dynamic/getips")
                .form(m)
                .execute().body();
        System.out.print(body);*/

//        System.out.println(body);
//        String[] split = body.split(":");
//        int port = Integer.parseInt(split[1].trim());
//        String ipAddr = split[0];
//        System.out.println(body);
//        System.out.println(ipAddr + port );

        //https://aapi.51daili.com/getapi2?linePoolIndex=1&packid=2&unkey=&tid=&qty=1&time=2&port=1&format=json&ss=5&css=&pro=%E5%AE%81%E5%A4%8F&city=&dt=1&ct=0&service=1&usertype=17
//        String body = HttpRequest.get("https://aapi.51daili.com/getapi2?linePoolIndex=1&packid=2&unkey=&tid=&qty=1&time=2&port=1&format=json&ss=5&css=&city=&dt=1&ct=0&service=1&usertype=17&pro=陕西省")
//                .execute().body();
//        System.out.println(body);
//
//        JSONObject resp = JSONObject.parseObject(body);
//        JSONArray list = resp.getJSONArray("data");
//        JSONObject data = list.getJSONObject(0);
//        String ipAddr = data.getString("IP");
//        Integer port = data.getInteger("Port");
//
//        String location = ProxyUtil.ip2region(ipAddr);
//        System.out.println(location);
//        String[] split = location.split("\\|");
//        System.out.println(split[2]);

//        String body = HttpRequest.get("http://api.wandoudl.com/api/ip?app_key=335df332a886cbaf27698df5f42ff936&pack=228272&num=1&xy=1&type=1&lb=\\r\\n&nr=99&area_id=210400").execute().body();
//        System.out.println(body);
    }

    private static void ip() {
        String property = System.getProperty("user.dir");
        String dbPath = (property + File.separator + "ip2region.xdb");
        System.out.println(dbPath);
//        String dbPath = "ip2region.xdb";

        // 1、从 dbPath 加载整个 xdb 到内存。
        byte[] cBuff;
        try {
            cBuff = Searcher.loadContentFromFile(dbPath);
        } catch (Exception e) {
            System.out.printf("failed to load content from `%s`: %s\n", dbPath, e);
            return;
        }

        // 2、使用上述的 cBuff 创建一个完全基于内存的查询对象。
        Searcher searcher;
        try {
            searcher = Searcher.newWithBuffer(cBuff);
        } catch (Exception e) {
            System.out.printf("failed to create content cached searcher: %s\n", e);
            return;
        }

        // 3、查询
        try {
            String ip = "122.18.92.145";
            long sTime = System.nanoTime();
            String region = searcher.search(ip);
            long cost = TimeUnit.NANOSECONDS.toMicros((long) (System.nanoTime() - sTime));
            System.out.printf("{region: %s, ioCount: %d, took: %d μs}\n", region, searcher.getIOCount(), cost);
        } catch (Exception e) {
//            System.out.printf("failed to search(%s): %s\n", ip, e);
        }
    }

    private static void signTest() throws IllegalAccessException {
        //        TreeMap<String, String> map = new TreeMap<>();
//        map.put("a", "b");
//        map.put("c", "b");
//        map.put("d", "b");
//        map.put("e", "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCbe07q/b4UPfy9fl/wKz1h5lBXnCdZL94OSkugY/HCHAIhhOUnyc646SfqIeruv7Cp94rJk48wsJLPQn2Hnz+5hHLJSGHElSdaGkE3m9LcXbF4CFATf8BVNKYlQ7rgA43gjeyzSQpTFBWIm4h05bAHd9lJ1lHeC9mLW8MjbO67EwIDAQAB");
        String o = "{\n" +
                "    \"channel_id\":\"jx3_weixin\",\n" +
                "    \"money\":\"20\",\n" +
                "    \"notify_url\":\"http://192.168.0.188:8080/api/callback\",\n" +
                "    \"p_account\":\"161b54d4e70544bf8ec799dae34d207c\",\n" +
                "    \"p_order_id\":\"bac555aacffff18999999\",\n" +
                "}";
        //        String o = "{\n" +
//                "    \"p_account\": \"662477aac40542e2bd98d5d7adaed359\",\n" +
//                "    \"p_order_id\": \"na1bfa23a3ba44acaf55\",\n" +
//                "}";
        OrderCreateParam orderCreateParam = JSONObject.parseObject(o, OrderCreateParam.class);

        SortedMap<String, String> map = CommonUtil.objToTreeMap(orderCreateParam);
        String s = encodeSign(map, "e191aa33c9a74416b6ae6aa66d7195f0");
        System.out.println(s);
        System.out.println(JSONObject.toJSONString(orderCreateParam));

//        String ss = "{\n" +
//                "    \"p_account\": \"e191aa33c9a74416b6ae6aa66d7195f1\",\n" +
//                "    \"money\": 10,\n" +
//                "    \"p_order_id\": \"na1bfa23a3ba44acaf5b119ddyyvmm\",\n" +
//                "    \"channel_id\": \"jx3_weixin\",\n" +
//                "    \"notify_url\": \"http://127.0.0.1:8080/api/test/callback\",\n" +
//                "    \"attach\":\"lalala\",\n" +
//                "    \"sign\": \"\"\n" +
//                "}";
//        JSONObject rs = JSONObject.parseObject(ss);
//        System.out.println(Result.ok(rs));
    }

    /**
     * sign 签名 （参数名按ASCII码从小到大排序（字典序）+key+MD5+转大写签名）
     *
     * @param map
     * @return
     */
    public static String encodeSign(SortedMap<String, String> map, String key) {
        if (StringUtil.isEmpty(key)) {
            throw new RuntimeException("签名key不能为空");
        }
        Set<Map.Entry<String, String>> entries = map.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        List<String> values = new ArrayList<>();

        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String k = String.valueOf(entry.getKey());
            String v = String.valueOf(entry.getValue());
            if ((v != null) && entry.getValue() != null && !"sign".equals(k) && !"key".equals(k)) {
                values.add(k + "=" + v);
            }
        }
        values.add("key=" + key);
        String sign = StringUtil.join(values, "&");
        System.out.println(sign);
        return encodeByMD5(sign).toLowerCase();
    }

    /**
     * 通过MD5加密
     *
     * @param algorithmStr
     * @return String
     */
    public static String encodeByMD5(String algorithmStr) {
        if (algorithmStr == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(algorithmStr.getBytes(StandardCharsets.UTF_8));
            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static String getFormattedText(byte[] b) {
        int i;

        StringBuilder buf = new StringBuilder();
        for (byte value : b) {
            i = value;
            if (i < 0)
                i += 256;
            if (i < 16)
                buf.append("0");
            buf.append(Integer.toHexString(i));
        }
        // 将计算结果s转换为字符串
        return buf.toString();
    }

    private static void test5() throws IOException, NoSuchMethodException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
//        File file = ResourceUtils.getFile("classpath:d4.js");
        ClassPathResource classPathResource = new ClassPathResource("d4.js");
        InputStream is = classPathResource.getInputStream();
        File file = new File("tmp");
        inputStreamToFile(is, file);
        FileReader reader = new FileReader(file);
        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;    // 调用merge方法，并传入两个参数

                String pwd = "wx12345678";
                String payload = (String) invoke.invokeFunction("test", pwd);
//                Object c = invoke.invokeFunction("get_w", 1888,"[[1,1]]","xxx",188);
                System.out.println("c = " + payload);
                String encode = URLEncoder.encode(payload, "UTF-8");
                System.out.println("pwd = " + encode);

                String output = "aAUwkM5lynrJX5PBGNYPjTvyf_A3p_uxHrlc1B12sWnqTNb9SB6oRBAAk_GAAmjMNRV3Zhk5bgr5xCprg47rKBsI9I9OJiOk5WQzBikSchQvjim9bt7h_mO5erpZj7sDedF0N2DK4ME_mGMrx_eyZd46De_X4tSQPuJjyTDQGPt2uJNvr7S7HYRAQq462WgOHZwngZHl707pJ0TMX0RLc9vJMBRmCe-NNnmknTvqVyhjlnNZw81tbGTmJpd3BTEqd-t94PyF-sFtpL47pULctqQLEDz2u4tjBy40yg601J0=";
                String captcha_output = URLEncoder.encode(output, "UTF-8");

                HttpResponse resp = HttpRequest.get("https://pf-api.xoyo.com/passport/common_api/login")
                        .form("account", "18210889498")
                        .form("encrypt_method", "rsa")
                        .form("captcha_id", "a7c9ab026dc4366066e4aaad573dce02")
                        .form("lot_number", "98ead2fcd6cb463b9c27ecd111c55b91")
                        .form("pass_token", "c5e635d58f5d8e8f6c4e93c5488e898a59d461a342e4553214bacf3d7176d3ce")
                        .form("gen_time", "1687955434")
                        .form("captcha_output", captcha_output)
                        .form("password", encode)
                        .form("callback", "jsonp_ef2891abd4b000")
                        .execute();

                System.out.println(resp.body());

//                JSONObject obj = JSONObject.parseObject(resp.body());
//                JSONObject data = obj.getJSONObject("data");
//                System.out.println(obj);
//                System.out.println(data);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    /**
     * 输入流转文件
     *
     * @param ins
     * @param file
     */
    public static void inputStreamToFile(InputStream ins, File file) {
        BufferedOutputStream bos = null;
        BufferedInputStream bis = new BufferedInputStream(ins);
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = bis.read(buffer, 0, 8192)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new RuntimeException("上传文件压缩出错", e);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
                ins = null;
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
                bos = null;
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
                bis = null;
            }
        }
    }


    private static void redissionTest() {
        Config config = new Config();
        String redisUrl = String.format("redis://%s:%s", "127.0.0.1" + "", "6379");
        config.useSingleServer().setAddress(redisUrl);
        config.useSingleServer().setConnectionMinimumIdleSize(10);
        RedissonClient redissonClient = Redisson.create(config);

        for (int i = 0; i < 50; i++) {
            int finalI = i;
            new Thread(() -> {
                RLock lock = redissonClient.getLock("111");
                try {
                    boolean tryLock = lock.tryLock(1, 1, TimeUnit.SECONDS);
                    if (!tryLock) {
                        System.out.println("获取分布式锁失败，lockKey = " + tryLock);
                    }
                    if (tryLock) {
                        System.out.println(" 开始执行业务 :" + finalI + " --- " + tryLock);
                        Thread.sleep(5000);
                        System.out.println(" 完成执行业务 :" + finalI + " --- " + tryLock);
                    }
                } catch (InterruptedException | IllegalMonitorStateException ex) {
                    System.out.println(ex.getStackTrace()[0]);
                } finally {
                    try {
                        if (lock.isLocked()) {
                            lock.unlock();
                        }
                    } catch (IllegalMonitorStateException ex) {
                        System.out.println(" lala 锁过了 :" + finalI + " --- " + ex.getStackTrace()[0]);
                    }
                }

            }).start();
        }
    }

    public static boolean isUrl(String pInput) {
        if (pInput == null) {
            return false;
        }
        String regEx = "^(http|https|ftp)\\://([a-zA-Z0-9\\.\\-]+(\\:[a-zA-"
                + "Z0-9\\.&%\\$\\-]+)*@)?((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{"
                + "2}|[1-9]{1}[0-9]{1}|[1-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}"
                + "[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|"
                + "[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-"
                + "4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])|([a-zA-Z0"
                + "-9\\-]+\\.)*[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4})(\\:[0-9]+)?(/"
                + "[^/][a-zA-Z0-9\\.\\,\\?\\'\\\\/\\+&%\\$\\=~_\\-@]*)*$";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(pInput);
        return matcher.matches();
    }

    private static void test3() {
        String date = "2023-02-17 16:32:21";
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime parse = LocalDateTime.parse(date, format);
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-5);
        System.out.println(parse);
        System.out.println(localDateTime);
        System.out.println(localDateTime.isBefore(parse));
    }

    private static void urlTest() {
        String url = "https://xuy02.oss.pubstaging-north3.inspurcloudoss.com:99/ysp/%E8%B0%A2%E5%AE%89%E7%90%AA%20-%20%E7%8B%AC%E5%AE%B6%E6%9D%91.mp3";

        System.out.println(url.substring(0, url.lastIndexOf("/")));
        Pattern pattern = Pattern.compile("(\\w+):\\/\\/([^/:]+)(:\\d*)?([^# ]*)");
        Matcher matcher = pattern.matcher(url);
        matcher.find();
        String domain = matcher.group(2);
        String port = matcher.group(3);
        String buket = domain.substring(0, domain.indexOf("."));
        String d = domain.substring(domain.indexOf(".") + 1);
        System.out.println(buket);
        System.out.println(d);
        if (port != null) {
            System.out.println(d + port + "/" + buket + matcher.group(4));
        } else {
            System.out.println(d + "/" + buket + matcher.group(4));
        }
    }

    private static void test2() throws FileNotFoundException, NoSuchMethodException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        File file = ResourceUtils.getFile("classpath:d2.js");
        FileReader reader = new FileReader(file);   // 执行指定脚本
        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;    // 调用merge方法，并传入两个参数

                String captchaId = "a7c9ab026dc4366066e4aaad573dce02";
                String lotNum = "9fd5e9d295054375a2e6697cb384c71b";
                String dTime = "2023-02-11T00:00:17.053991+08:00";
                String payload = "39Gl86BQgpTxeuyf4iPX04J91RvP8jL3C7WWwr7MJznwkyvAUzfXb-TSPYNMj0rlxM0Hb7tC5hHUzNwFpZXXqKd57Mx_-e66VgAObBa2N2zASdiSSzpw50MXbylKCEdzj1fxbyVHATosuV_Dw7G0CmC1sQG58tYY-8WB1_4PpPl3qrsuvhkN_f4zePlzEKuY4lU2SfD00pQtlseYBqzeChYbeUX62uj7pi8tRxuHeiz5nNsUD3wtO37dbaq6uTk1Sp4jd3Jvt4HjdeH99a9gEh2YdBKSKB1sLfoO5jh-NMkmkhPEeRD8ETQXgIvjr0w9Qxde3H-urDMkWgfxtH9LO2DJpa3rAg-EMhInoU1EvP6HPxyjS4xsq6T88arVgtvoCfHFSeLlXCmxBxUGHRsVus-Gr3rPSA2vitK51Pjxkriw8QEG5ER6PaOw4lMUAwh3-snD5z0r4rOsIl5oGVJsp4PKV-ZYqOa0WPjl5B0DcmQ9K82f2MP6dfqcIKzWg_XcofyAInSbSjwYe3fel9hBT0vOVuTP05MWRMB3w1jFs_avBEjlKJlOmJFFUFKmLuLiX1185WLRo8vfqydkJlfZjnWnG1HbxA8-3plywR3jo4wrSFNSjqr1ZfVw0TM-ITrdWJ5Eu1pbu2VgrGNYj8q5HBAs0OWdy2GCYgZBRp34PZeogyWu7cpWC_dC2-EyV4QZ_EH3kRVzp32P9JAHKb9nEA==";
                String passToken = "974b788ec932e9363f3f4083ea13de262ec73d40a68b376f3f04b481a2c02d21";
                List<List<Integer>> l = new ArrayList<>();
                List<Integer> l1 = new ArrayList<>();
                l1.add(7236);
                l1.add(2945);
                List<Integer> l2 = new ArrayList<>();
                l2.add(3459);
                l2.add(2647);
                List<Integer> l3 = new ArrayList<>();
                l3.add(5447);
                l3.add(1405);

                l.add(l1);
                l.add(l2);
                l.add(l3);
                System.out.println(l);
                ScriptObjectMirror c = (ScriptObjectMirror) invoke.invokeFunction("get_param",
                        captchaId,
                        lotNum,
                        l.toString(),
                        dTime
                );
                System.out.println(c.get("user_resp"));
                System.out.println(c.get("lot_number"));
                System.out.println(c.get("d_time"));
//                Object w = invoke.invokeFunction("get_w", 1856, c.get("user_resp"), c.get("lot_number"), c.get("d_time"));
                Object w = invoke.invokeFunction("get_w", c);
                System.out.println("w: ---------\n" + w);

                String resp = HttpRequest.get("https://gcaptcha4.geetest.com/verify")
                        .form("captcha_id", captchaId)
                        .form("client_type", "web")
                        .form("lot_number", lotNum)
                        .form("payload", payload)
                        .form("process_token", passToken)
                        .form("payload_protocol", "1")
                        .form("pt", "1")
                        .form("w", w)
                        .form("callback", "geetest_1676042920288")
                        .execute().body();

                System.out.println(resp);

            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    public static void test() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
//        File file = ResourceUtils.getFile("classpath:d3.js");
        ClassPathResource classPathResource = new ClassPathResource("d3.js");
        InputStream is = classPathResource.getInputStream();
        File file = new File("tmp");
        CommonUtil.inputStreamToFile(is, file);
        FileReader reader = new FileReader(file);   // 执行指定脚本
        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;    // 调用merge方法，并传入两个参数

                ScriptObjectMirror c = (ScriptObjectMirror) invoke.invokeFunction("get_sec",
                        "a7c9ab026dc4366066e4aaad573dce02",
                        "5697d08cf1a44a5ba8e8818510f17696",
                        "e49dfcf47feeaeb960889f7117b0ae64bdd87550821c50cb8b1b2e3a6b3311db",
                        "1675952003",
                        "662a-teKeUpl5FxeT6YiiXIbumHOjcfmcwusUifSm2Fc7i496LG7OO_3RIbggNlHL32Cq9mQRj8yen75hZ3gWmbvtQ_18zOIJ_3WA1FaZGmzKdfKBoQLXj5wBkF66Xbmd6wvkfU4KwP1-vr8LBaXJW1dS-BYXeLVsVt3eMl8-XzqkpMZ0z1Sai7X_wXCx6oW66Vw4WJlmBJIUUPlcfKhywtx6majgf54Nb5dzDhYzdCkgbfXEWZznHPC4vsHIrFoxkBqI9ibhkyrOSx9jPjnT2EShbYKIBklxqBEs5QnLsWs-_LMNJekhhRmis0nkKhOXn43wq3e1oweJj0AAil3DQ=="
                );
                String payload = (String) invoke.invokeFunction("get_payload", c);
//                Object c = invoke.invokeFunction("get_w", 1888,"[[1,1]]","xxx",188);
                System.out.println("c = " + payload);
                GeeProdCodeParam param = new GeeProdCodeParam();

                String cookie = "xoyokey_=obZuB20s=&E91L=5HW5gHHUBUH&&s3xee90=5PuUugggscE302=WgHBUse95BBu.BU&&Eg.H5PZu&U3gB9UZ1g.ZxNx5=uPZ5&Hg&u5&9WE2=BH&f3K; expires=Thu, 09-Feb-2023 15:05:58 GMT; path=/; domain=.xoyo.com; secure; httponly; samesite=none\\n";
                param.setEncrypt_fields("payload");
                param.setEncrypt_version("v1");
                param.setEncrypt_method("xoyo_combine");
                param.setPayload(payload);
                param.setToken(cookie);
                String jp = JSON.toJSONString(param);

                String resp = HttpRequest.get("https://pay-pf-api.xoyo.com/pay/recharge_api/create_order")
                        .body(jp)
                        .cookie(param.getToken())
                        .execute().body();

                JSONObject obj = JSONObject.parseObject(resp);
                JSONObject data = obj.getJSONObject("data");
                System.out.println(obj);
                System.out.println(data);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }
}