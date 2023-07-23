package com.vbox.test;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class SdoQyTest {

    public static void main(String[] args) {

//        String sessionId = "c1724d6720e5309b35c98f56b73abf31";
//        String formUrl = "https://pay.sdo.com/api/orderlist?page=1&range=2";
//
//        String payRs = HttpRequest.post(formUrl)
//                .cookie("nsessionid=" + sessionId)
//                .header("Referer", "https://pay.sdo.com/user/detail")
//                .header("Content-Type", "application/x-www-form-urlencoded")
//                .execute().body();
//
//        JSONObject jsonObject = JSONObject.parseObject(payRs);
//        JSONObject data = jsonObject.getJSONObject("data");
////        JSONObject data = data.Get("orders");

        String url = "https://mapi.alipay.com/gateway.do?_input_charset=utf-8&anti_phishing_key=KP9eBNnadqwczbzs_Q%3D%3D&exter_invoke_ip=60.23.138.185&it_b_pay=5m&notify_url=http%3A%2F%2Fpaycallback.sdo.com%2Fcallback%2Falipaydirect&out_trade_no=C1010127026167230721202251000002&partner=2088721828009755&payment_type=1&return_url=http%3A%2F%2Fpay.sdo.com%2Fcashier%2Fresult%2FresultAlipaydirect&seller_email=syshiyou%40shandagames.com&seller_id=2088721828009755&service=create_direct_pay_by_user&sign=4d292311fe13cec1c2a249be3c3ce483&sign_type=MD5&subject=%E6%B8%B8%E6%88%8F%E5%85%85%E5%80%BC%7C%E8%B0%A8%E9%98%B2%E8%AF%88%E9%AA%97%7C%E5%AE%A2%E6%9C%8D%E7%83%AD%E7%BA%BF021-50504724&total_fee=0.98";

        HttpResponse execute = HttpRequest.get(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();
        String location = execute.header("Location");

        HttpResponse executeLocation = HttpRequest.get(location)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();

        String html = executeLocation.body();
        if (html.contains("交易已经支付")) {
            System.out.println("lalala");
        }
//        "alipays://platformapi/startApp?appId=10000007&sourceId=excashierQrcodePay&actionType=route&qrcode=https://qr.alipay.com/upx05196bsaswqfo7l1m25af"

    }

    private static void sdo() {
        String url = "https://mapi.alipay.com/gateway.do?";
        String param = "_input_charset=utf-8&anti_phishing_key=KP9eBNj1tJg8lXlxIQ%3D%3D&exter_invoke_ip=106.39.149.62&it_b_pay=5m&notify_url=http%3A%2F%2Fpaycallback.sdo.com%2Fcallback%2Falipaydirect&out_trade_no=C1010127026168230720221102000001&partner=2088721828009755&payment_type=1&return_url=http%3A%2F%2Fpay.sdo.com%2Fcashier%2Fresult%2FresultAlipaydirect&seller_email=syshiyou%40shandagames.com&seller_id=2088721828009755&service=create_direct_pay_by_user&sign=36f834883a69d7fcc14dd0a9a5947eb0&sign_type=MD5&subject=%E6%B8%B8%E6%88%8F%E5%85%85%E5%80%BC%7C%E8%B0%A8%E9%98%B2%E8%AF%88%E9%AA%97%7C%E5%AE%A2%E6%9C%8D%E7%83%AD%E7%BA%BF021-50504724&total_fee=99.96";

        HttpResponse execute = HttpRequest.get(url + param)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();
        String location = execute.header("Location");

        HttpResponse executeLocation = HttpRequest.get(location)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();
        String excashier = executeLocation.header("Location");

        HttpResponse response = HttpRequest.get(excashier)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .execute();
        String htmlBody = response.body();
        String qrCodeValue = getQrCodeValue(htmlBody);
        System.out.println(qrCodeValue);
    }

    public static String getQrCodeValue(String html) {
        String regex = "<input[^>]*name=\"qrCode\"[^>]*value=\"([^\"]+)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }


}
