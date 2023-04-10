package com.vbox;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.ProxyUtil;
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

        String url = "      if(is_postmsg==\"1\")\n" +
                "                {\n" +
                "                    parent.postMessage(JSON.stringify({\n" +
                "                        action : \"send_deeplink\",\n" +
                "                        data : {\n" +
                "                            deeplink : \"weixin://wap/pay?prepayid%3Dwx092048332302565f1ed2cc24f9739b0000&package=3170794954&noncestr=1681044546&sign=9c0cab0fc5e04b306c6109d1a8fb0061\"\n" +
                "                        }\n" +
                "                    }), \"\");\n" +
                "                }\n" +
                "                else\n" +
                "                {\n" +
                "                    var url=\"weixin://wap/pay?prepayid%3Dwx092048332302565f1ed2cc24f9739b0000&package=3170794954&noncestr=1681044546&sign=9c0cab0fc5e04b306c6109d1a8fb0061\";\n" +
                "                    var redirect_url=\"https://m.xoyo.com/#/order-status?vouch_code=90016810445130668682&way=weixin_mobile&order_id=98e8f8fcc8426678adb01736a82f94e5&item=jx3\";\n" +
                "                    top.location.href=url;\n" +
                "\n" +
                "                    if(redirect_url)\n" +
                "                    {\n" +
                "                        setTimeout(";

        if (url.contains("weixin://wap")) {
            String substring = url.substring(url.indexOf("weixin://wap"), url.indexOf("&sign=") + 38);
            System.out.println(substring);
        }

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
     * @param map
     * @return
     */
    public static String encodeSign(SortedMap<String,String> map, String key){
        if(StringUtil.isEmpty(key)){
            throw new RuntimeException("签名key不能为空");
        }
        Set<Map.Entry<String, String>> entries = map.entrySet();
        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        List<String> values = new ArrayList<>();

        while(iterator.hasNext()){
            Map.Entry<String, String> entry = iterator.next();
            String k = String.valueOf(entry.getKey());
            String v = String.valueOf(entry.getValue());
            if ((v!=null) && entry.getValue() !=null && !"sign".equals(k) && !"key".equals(k)) {
                values.add(k + "=" + v);
            }
        }
        values.add("key="+ key);
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
        if (algorithmStr==null) {
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

                HttpResponse resp = HttpRequest.get("https://pf-api.xoyo.com/passport/common_api/login")
                        .form("account", "18210889498")
                        .form("encrypt_method", "rsa")
                        .form("captcha_id", "a7c9ab026dc4366066e4aaad573dce02")
                        .form("lot_number", "a3731d67d8c9459ab05114e195ec8b6d")
                        .form("pass_token", "d6d7a94f1fdf1edaeabac9c9f34b2963a6c67b31ba3517dba9ac33788cb157b0")
                        .form("gen_time", "1676726917")
                        .form("captcha_output", "662a-teKeUpl5FxeT6YiiSbVmvtYPCxaHv_f8xvivNeivBvOUS5AiH1KD9M__RiIiXOdQ60fi7U3CFngJzHoAnyFbzhWYZE5LULy3LIskypH1aaGHLRuaLm-6U9eyiNt14Nnv0YzI9AMn9yvORwWNtyZpK_igE-ckrSOoCCaiheTb0xNm0TOu_rwRrxY6XgpCG9ryAwx4XB5Mn-NSRMm0o5uaXvzPOtDFhaOAXXmj_KHfQaId-aY2DEPEV4i7l2KNRrJjoaCFC646MykNSrCEg==")
                        .form("password", encode)
                        .form("callback", "jsonp_ef2891abd4b000")
                        .execute();

                JSONObject obj = JSONObject.parseObject(resp.body());
                JSONObject data = obj.getJSONObject("data");
                System.out.println(obj);
                System.out.println(data);
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