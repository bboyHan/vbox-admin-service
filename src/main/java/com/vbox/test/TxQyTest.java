package com.vbox.test;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.util.CommonUtil;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.persistent.pojo.dto.TxWaterList;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 直接调用js代码
 */
public class TxQyTest {
    public static void addElement(Map<Integer, List<String>> map, Integer key, String value) {
        // 检查是否已经存在对应的列表
        List<String> accountList = map.computeIfAbsent(key, k -> new ArrayList<>());
        // 如果不存在，则创建一个新的列表并将其添加到map中

        // 向账户列表中添加新的账户
        accountList.add(value);
    }

    public static void main(String[] args) {
        // 设置 ChromeDriver 路径
        WebDriverManager.chromedriver().setup();
        // 创建 ChromeOptions 对象并设置请求头信息
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // 启用无界面模式
        options.addArguments("--no-sandbox");  // 防止在 Linux 下遇到的一些问题
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");  // 禁用 GPU 加速

        // 添加请求头信息
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");
        options.addArguments("accept-language=en-US,en;q=0.9");
        // 设置代理服务器（可选）
         options.addArguments("--proxy-server=http://202.107.22.100:40021");

        // 创建 ChromeDriver 实例
        WebDriver driver = new ChromeDriver(options);

        // 导航到指定网页
        driver.get("https://pay.qq.com/h5/trade-record/trade-record.php?appid=1450000186&_wv=1024&pf=2199&sessionid=hy_gameid&sessiontype=st_dummy&openkey=openkey&openid=446794914#/");

        // 系统等待页面加载完成
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 获取网页内容
        String html = driver.getPageSource();

        // 关闭浏览器驱动
        driver.quit();

        // 使用 Jsoup 解析 HTML
        try {
            // 解析HTML内容
            Document doc = Jsoup.parse(html);

            // 获取class为list-record的元素
            Elements elements = doc.getElementsByClass("list-record");

            // 遍历元素并提取所需数据
            for (Element element : elements) {
                // 获取detail-title元素
                Element titleElement = element.getElementsByClass("detail-title").first();

                // 提取Q币数量
                String qCoin = titleElement.getElementsByTag("h3").first().text();

                // 提取支付时间和方式
                Element pElement = titleElement.getElementsByTag("p").first();
                String time = pElement.getElementsByTag("span").first().text();
                String paymentMethod = pElement.getElementsByTag("span").last().text();

                // 打印结果
                System.out.println("Q币数量: " + qCoin);
                System.out.println("支付时间: " + time);
                System.out.println("支付方式: " + paymentMethod);
                System.out.println("------------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void jsoup() {
        String url = "https://pay.qq.com/h5/trade-record/trade-record.php?appid=1450000186&_wv=1024&pf=2199&sessionid=hy_gameid&sessiontype=st_dummy&openkey=openkey&openid=446794914#/";

        try {
            Document doc = Jsoup.connect(url).get();
            Elements detailTitles = doc.select(".detail-title");

            for (Element title : detailTitles) {
                String qbiQuantity = title.selectFirst("h3").text();
                String time = title.selectFirst("p span:first-child").text();
                String paymentMethod = title.selectFirst("p span:nth-child(2)").text();

                System.out.println("Q币数量：" + qbiQuantity);
                System.out.println("支付时间：" + time);
                System.out.println("支付方式：" + paymentMethod);
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void test4() {
        Map<Integer, List<String>> map = new HashMap<>();

        // 添加元素
        addElement(map, 100, "Account1");
        addElement(map, 200, "Account2");
        addElement(map, 100, "Account3");
        addElement(map, 300, "Account4");

        // 打印结果
        for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
    }

    private static void test3() {
//        String address = "https://openapi.alipay.com/gateway.do?app_id=2017082908437947&biz_content=%7B%22business_params%22%3A%7B%22mc_create_trade_ip%22%3A%22180.102.115.133%22%2C%22mcCreateTradeIp%22%3A%22180.102.115.133%22%7D%2C%22timeout_express%22%3A%225m%22%2C%22product_code%22%3A%22FAST_INSTANT_TRADE_PAY%22%2C%22total_amount%22%3A%22199.92%22%2C%22out_trade_no%22%3A%22C1010127026167230724133740000001%22%2C%22quit_url%22%3A%22https%3A%2F%2Fpay.sdo.com%2F%22%2C%22goods_detail%22%3A%5B%7B%22price%22%3A%22199.92%22%2C%22goods_name%22%3A%22%E6%B8%B8%E6%88%8F%E5%85%85%E5%80%BC%7C%E8%B0%A8%E9%98%B2%E8%AF%88%E9%AA%97%22%2C%22quantity%22%3A1%2C%22goods_id%22%3A%221_39__1459%22%7D%5D%2C%22subject%22%3A%22%E6%B8%B8%E6%88%8F%E5%85%85%E5%80%BC%7C%E8%B0%A8%E9%98%B2%E8%AF%88%E9%AA%97%7C%E5%AE%A2%E6%9C%8D%E7%83%AD%E7%BA%BF021-50504724%22%7D&charset=utf-8&method=alipay.trade.page.pay&notify_url=https%3A%2F%2Fpaycallback.sdo.com%2Fcallback%2Falipaydirect&return_url=https%3A%2F%2Fpay.sdo.com%2Fcashier%2Fresult%2FresultAlipaydirect&sign=RrJNWzWAkFwMxc9cFNevbUcoJK%2BuC%2FoWm0%2F0p06XExjn3%2FEb3f1QbYH5Xw1hVEwKjRm1qKukvXEFk0dPirsbMhdgnylv9IeR3EKKK7VKu28ZwM%2BplXjlt7p6Nm3gv4bGkqP3HPIhaiiyi8Vu%2BbeelNFRRP7L5m73TDCJWfJWxPs%3D&sign_type=RSA&timestamp=2023-07-24+13%3A37%3A40&version=1.0";
        String address = "https://openapi.alipay.com/gateway.do?charset=utf-8&method=alipay.trade.page.pay&sign=LyYlXZVGkcoMIdtgO5P%2FBiUZ6FBmCda7uQn%2Bld1l%2Bv2YGZiqtzpXRMpqxEH%2FjxclXovVslBdbK0a9%2BpGW3iCw%2FdwV5zqxaHCzRr9Gv0UKvqsqxU5Jdq0sGEKkixEOid7qc2FlyM4OeEs3Yr8eZ9wlc8329BELGs%2BajKY6Q0KpVJztBYQpDgBeKTj%2Fhh65Y0ymay1FR8IiLZPKlta8ozrq%2FHmsQNoubdIMkm0E%2FdB%2FZl1d97iLZ59P45ISBX%2B4W%2FLsICBnOjWXb%2FdCAnuMplOx6Xl5szs4bpDSxHIlkA3QVWUUojqj3Api5MX%2F3bLKtShwKI80QOofARKT7bjpOkHRA%3D%3D&notify_url=https%3A%2F%2Fpay-pf-api.xoyo.com%2Fpay%2Frecharge_api%2Fnotify_do_fill%2Falipay_qr%2Fjx3%2Fc3VuODAwMzc2Mg%3D%3D%2F5cf7d2f2e7da3d3f9c63b5f8b320ae9d%2F0&version=1.0&app_id=2017120500398486&sign_type=RSA2&timestamp=2023-08-01+21%3A27%3A57&alipay_sdk=alipay-sdk-java-4.35.154.ALL&format=json&biz_content={\"body\":\"西山居[剑网3]充值订单号:35016908964777879068-(仅限金山西山居旗下游戏充值)\",\"subject\":\"游戏充值|谨防诈骗|西山居[剑网3]\",\"out_trade_no\":\"35016908964777879068\",\"timeout_express\":\"2m\",\"goods_type\":\"0\",\"total_amount\":\"50.00\",\"qr_pay_mode\":\"4\",\"qrcode_width\":200,\"product_code\":\"FAST_INSTANT_TRADE_PAY\",\"business_params\":{\"mcCreateTradeIp\":\"183.17.66.55\",\"outTradeRiskInfo\":\"{\\\"extraAccountRiskLevel\\\":\\\"low\\\",\\\"mcCreateTradeTime\\\":\\\"2023-08-01 21:27:57\\\",\\\"extraAccountPhoneLastTwo\\\":\\\"20\\\",\\\"extraAccountCertnoLastSix\\\":\\\"128334\\\",\\\"desensitizedUid\\\":\\\"b94772fa8e759cca71a2c60f01e2307d\\\"}\"},\"promo_params\":{\"merchantpromo_tag\":\"hdyh\"}}";
        HttpResponse execute = HttpRequest.get(address)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();
        String location = execute.header("Location");

        HttpResponse executeLocation = HttpRequest.get(location)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();

        String html = executeLocation.body();
        System.out.println(html.contains("该订单已付款"));
    }

    private static void test2() {
        String ck = "pay_openid:FF2BB2C934188CA4F060310E3F20E5C8;pay_openkey:B723146D49CD9EB3D40CD9D722474D21;";
        String openId = CommonUtil.getCookieValue(ck, "openid");
        String openKey = CommonUtil.getCookieValue(ck, "openkey");

        String formUrl = "https://api.unipay.qq.com/v1/r/1450000490/trade_record_query" +
                "?pf=mds_storeopen_qb-__mds_default_v1_0_0.qb-html5&pfkey=pfkey&from_h5=1" +
                "&session_token=90324BE6-E4D9-4DA9-AE57-44D3DE03C8F8&webversion=stdV2.16.0.1.other.other&r=0.4950957161982934" +
                "&openid=B7C04C6D624CE758BED547E970C9D32A" +
                "&openkey=C18F10E9C5A14669E6F6248911309DFC" +
                "&session_id=openid&session_type=kp_accesstoken&qq_appid=&SerialNo=QQACCT_SAVE-20230618-DVM0DIDbwOOj" +
                "&CmdCode=query2&SubCmdCode=default&SystemType=portal" +
                "&EndUnixTime=1697069897&BeginUnixTime=1660810697" +
                "&Order=desc&PageNum=1&PageSize=100&anti_auto_script_token_id=E86CDBACCB84586D35C73C0B5FD0869D0CC23B6014F0D5ED09E42B823F4338E8C5D606F78B48CC9B2444B720F45277DE26CFE054DAD6BC06EDF407CE52FCF1E3&__refer=https%3A%2F%2Fpay.qq.com%2Fh5%2Findex.shtml%3Fr%3D0.7360455474285279" +
                "&encrypt_msg=ddcb93f583700dcf845ebb3a54dca62b5d623ce6bcafa3af58d41604917c8bb3a7cdc70bad9c406c009852ee9abc07b389da2b9f0041dc51d5655cc7679bc05739d4d4b4af72150ebdc63a1a4051c81931137759a5276911279136a0a141c6bde6982a6b383e1cb998661455244b20b775b270e1f8d9a6b0083b7895d1a4d267&msg_len=126";

        long pre_half_hour = 30 * 60 * 1000;
        long entTime = System.currentTimeMillis() / 1000;
//        long startTime = entTime - pre_half_hour;
        long startTime = 1690121248;
//        System.out.println(entTime);
//        System.out.println(startTime);

        URL urlPay = URLUtil.url(formUrl);
        Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
        Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);
        urlPayMap.put("EndUnixTime", entTime);
        urlPayMap.put("BeginUnixTime", startTime);
//        urlPayMap.remove("EndUnixTime");
//        urlPayMap.remove("BeginUnixTime");
        urlPayMap.remove("SerialNo");
        urlPayMap.put("openid", openId);
        urlPayMap.put("openkey", openKey);


        String payRs = HttpRequest.post("https://api.unipay.qq.com/v1/r/1450000490/trade_record_query")
                .form(urlPayMap)
                .header("Referer", "https://pay.qq.com/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute().body();
//        log.warn("tx trade_record_query, : {}" ,payRs);

        JSONObject jsonResp = JSONObject.parseObject(payRs);
//        log.warn("tx trade_record_query, : {}", jsonResp.get("ret"));
        List<TxWaterList> rl = jsonResp.getList("WaterList", TxWaterList.class);

        // 使用HashMap来保存相同充值金额的充值账号
        Map<Integer, List<String>> map = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        // 遍历rechargeList进行充值金额的筛选
        for (TxWaterList recharge : rl) {
            Integer payAmt = recharge.getPayAmt() / 100;
            String provideID = recharge.getProvideID();
            long payTime = recharge.getPayTime();
            Instant instant = Instant.ofEpochSecond(payTime);
            LocalDateTime parse = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
            LocalDateTime pre30min = now.plusMinutes(-30);
            // 如果该充值金额已存在于结果集中，则将充值账号添加进对应的列表中
            if (parse.isAfter(pre30min)) {
                List<String> accountList = map.computeIfAbsent(payAmt, k -> new ArrayList<>());
                accountList.add(provideID);
            }
        }
    }

    private static void test() {
        //        String formUrl = "https://api.unipay.qq.com/v1/r/1450000490/trade_record_query" +
//                "?pf=mds_storeopen_qb-__mds_default_v1_0_0.qb-html5&pfkey=pfkey&from_h5=1" +
//                "&session_token=90324BE6-E4D9-4DA9-AE57-44D3DE03C8F8&webversion=stdV2.16.0.1.other.other&r=0.4950957161982934" +
//                "&openid=B7C04C6D624CE758BED547E970C9D32A" +
//                "&openkey=C18F10E9C5A14669E6F6248911309DFC" +
//                "&session_id=openid&session_type=kp_accesstoken&qq_appid=&SerialNo=QQACCT_SAVE-20230618-DVM0DIDbwOOj" +
//                "&CmdCode=query2&SubCmdCode=default&SystemType=portal" +
//                "&EndUnixTime=1697069897&BeginUnixTime=1660810697" +
//                "&Order=desc&PageNum=1&PageSize=10&anti_auto_script_token_id=E86CDBACCB84586D35C73C0B5FD0869D0CC23B6014F0D5ED09E42B823F4338E8C5D606F78B48CC9B2444B720F45277DE26CFE054DAD6BC06EDF407CE52FCF1E3&__refer=https%3A%2F%2Fpay.qq.com%2Fh5%2Findex.shtml%3Fr%3D0.7360455474285279" +
//                "&encrypt_msg=ddcb93f583700dcf845ebb3a54dca62b5d623ce6bcafa3af58d41604917c8bb3a7cdc70bad9c406c009852ee9abc07b389da2b9f0041dc51d5655cc7679bc05739d4d4b4af72150ebdc63a1a4051c81931137759a5276911279136a0a141c6bde6982a6b383e1cb998661455244b20b775b270e1f8d9a6b0083b7895d1a4d267&msg_len=126";
//
//        long pre_half_hour = 30 * 60 * 1000;
//        long entTime = System.currentTimeMillis() / 1000;
//        long startTime = entTime - pre_half_hour;
//        System.out.println(entTime);
//        System.out.println(startTime);
//
//        URL urlPay = URLUtil.url(formUrl);
//        Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
//        Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);
//        urlPayMap.put("EndUnixTime", entTime);
//        urlPayMap.put("BeginUnixTime", startTime);
//        urlPayMap.remove("SerialNo");
//
//        String payRs = HttpRequest.post("https://api.unipay.qq.com/v1/r/1450000490/trade_record_query")
//                .form(urlPayMap)
//                .header("Referer", "https://pay.qq.com/")
//                .header("Content-Type", "application/x-www-form-urlencoded")
//                .execute().body();
//
//        JSONObject jsonObject = JSONObject.parseObject(payRs);
//        JSONArray waterList = jsonObject.getJSONArray("WaterList");
//
//        JSONObject o = waterList.getJSONObject(0);
//        long payTime = o.getLongValue("PayTime");
//        Instant instant = Instant.ofEpochSecond(payTime);
//        LocalDateTime parse = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
//        System.out.println(parse);
//
//
//        //pgv_pvi=5688113152; RK=usp5+jSJO0; ptcz=09bb8313e22e3d125eac32f9a401376d0d370ad10355f745c8670f9cf046d774; pgv_pvid=6960940894; o_cookie=1697047283; tvfe_boss_uuid=270f85dd4903d642; pac_uid=1_1697047283; iip=0; h_uid=h591656404271869389; _tucao_session=dHJEVXdyT3g4SjQwUjRYbmJqSS8rY3VZbDhaNGdSdWxzNjRBOGVJcnFUU0ZIR1Y5OWFBZUJ1bnBhWUFad1loNE9ybFZwMXBOd2J1cWpXdXJ5TG5tYzl3bGhEZG8zMC81VlhRaVZqbGpFNGM9--D9dmYjGbXOnxOcgzV5QZVQ%3D%3D; pgv_info=ssid=s1503686639284071; pt2gguin=o0384774115; ETK=; skey=@VgIYweV9V; pt_recent_uins=4aa168e90f229ac4a07ea50c45aedbb421142765fc771df244eb90638391a127ce43998775b7e8347bb6cea29447e0e775b037ffdaad5356; ptnick_384774115=e59091e697a5e891b5; uin=o384774115; olu=2e912054f3be6de1a4dae642697b51310c88c617fac39ad7; pt_login_sig=e76nn0JbyFIJvoneliJXRuRl4yfHOLynj*sd-dmZwPNY5e4DRMR0CcLgEw5ZPn0c; pt_clientip=0caf2400da00c002ff21395110ffb25cf266c18b; pt_serverip=7ebb7f000001e2a9; pt_local_token=-579018090; uikey=82cbb4e62ba6c801e98d79f0c421f50f38ca1e67539be68f210ee5532d0e6a04; pt_guid_sig=ea7dd41fcad64bd1cb681f425cc1ffb8d14f6dfd95beeb772054810628159b3d; confirmuin=0; ptui_loginuin=1697047283; ptdrvs=d47g2o9mqWYlbtp8c6eDG8ttOoZLDBJFuHaLnqeDYEs*SRnw3ltGROxR2EiFVvZhz5Etg-2a8kg_; pt_sms_ticket=Z9j1MHK4RIHmdq10nZWf5ELM1qgBYDlhjyTKYOKICRvMgOjBcwAaTQNOLkTCPDaL-U3rlX9OKkTAyHYxol9wRHjPHRqId6fa; pt_sms_phone=182******65;
//
//        boolean url = CommonUtil.isUrl("http://pay.chengyou.click/notify/Vbox/notify_res.htm");
//        System.out.println(url);

        String ck = "pay_openid:0923CC23465D8245EF38D83414EA03A0;pay_openkey:59380A75318B3CF97BECAC67A664C3D9;";
        String openId = CommonUtil.getCookieValue(ck, "openid");
        String openKey = CommonUtil.getCookieValue(ck, "openkey");

        System.out.println(openId);
        System.out.println(openKey);
    }


}