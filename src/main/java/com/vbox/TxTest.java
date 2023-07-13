package com.vbox;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.ExpireQueue;
import com.vbox.common.util.CommonUtil;
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
public class TxTest {
    public static void main(String[] args) throws Exception {
//        String ck = "RK=7TnUmX1ceO; ptcz=5e70fab72e3583a45a5fb8750f2e70abf031116f413b608e9546cb424492af82; pgv_pvid=1680018700537613; tvfe_boss_uuid=d62a8a33484fe65f; tencent_tdrc=SCuazW00uycui7aVsCWhXB8HXqem8H4eeM; ptui_loginuin=446794914; pay_mobile_account=qq; pay_mobile_game_account=; pay_openid=B7C04C6D624CE758BED547E970C9D32A; pay_openkey=C18F10E9C5A14669E6F6248911309DFC; pay_session_id=openid; pay_session_type=kp_accesstoken; pay_qq_appid=101502376; pay_qq_avatar=https://thirdqq.qlogo.cn/g?b=oidb&k=qoFw0wl4IA0PufcFszsOBA&kti=ZFUrcwAAAAA&s=100&t=1552966142; pay_qq_nickname=二十三; midas_cookie_samesite_flag=1; o_cookie=446794914; qq_domain_video_guid_verify=351ea21e36e28731; kepler_fp=kfp1KLCR17j-CwcnIExA67GCetDyGB0EapaoOnH3salpl_nSOyVtWsal-w**; __qc_wId=257; currentAccountType=QQ; _qpsvr_localtk=0.06697471127841181; midas_txcz_openid=B7C04C6D624CE758BED547E970C9D32A; midas_txcz_openkey=C18F10E9C5A14669E6F6248911309DFC; midas_txcz_sessionid=openid; midas_txcz_sessiontype=kp_accesstoken; midas_txcz_qqAppid=101502376; __qc__k=TC_MK=C18F10E9C5A14669E6F6248911309DFC; tKeplerToken=tid0GudsJVsFjUJYICv0SaTuHQGL1wsToBw7bcuEfgvQ1Tw*; kepler_ticket=wt2pcVW0JwhA3AvElCnqB0pU2QMugQ6JgDSMA0RoaXuy928m4-mhvCpCg125-0JnKm3Ilh3HOdneICy0EAFS3V1xYuwE98a3zm7yuYjV7AfNv3UopTNb8UJB-h2L-CipiWznbBbkHy84xMB95JpC7gXAGYUS9FRzJDe; tgw_l7_route=651e7187f749374d691cf466d08431e2";
        String ck = " RK=7TnUmX1ceO; ptcz=5e70fab72e3583a45a5fb8750f2e70abf031116f413b608e9546cb424492af82; pgv_pvid=1680018700537613; tvfe_boss_uuid=d62a8a33484fe65f; tencent_tdrc=SCuazW00uycui7aVsCWhXB8HXqem8H4eeM; ptui_loginuin=446794914; pay_mobile_account=qq; pay_mobile_game_account=; pay_openid=B7C04C6D624CE758BED547E970C9D32A; pay_openkey=C18F10E9C5A14669E6F6248911309DFC; pay_session_id=openid; pay_session_type=kp_accesstoken; pay_qq_appid=101502376; pay_qq_avatar=https://thirdqq.qlogo.cn/g?b=oidb&k=qoFw0wl4IA0PufcFszsOBA&kti=ZFUrcwAAAAA&s=100&t=1552966142; midas_cookie_samesite_flag=1; o_cookie=446794914; qq_domain_video_guid_verify=351ea21e36e28731; kepler_fp=kfp1KLCR17j-CwcnIExA67GCetDyGB0EapaoOnH3salpl_nSOyVtWsal-w**; __qc_wId=257; currentAccountType=QQ; _qpsvr_localtk=0.06697471127841181; midas_txcz_openid=B7C04C6D624CE758BED547E970C9D32A; midas_txcz_openkey=C18F10E9C5A14669E6F6248911309DFC; midas_txcz_sessionid=openid; midas_txcz_sessiontype=kp_accesstoken; midas_txcz_qqAppid=101502376; __qc__k=TC_MK=C18F10E9C5A14669E6F6248911309DFC; tKeplerToken=tid0GudsJVsFjUJYICv0SaTuHQGL1wsToBw7bcuEfgvQ1Tw*; kepler_ticket=wt2bQHEQDv2jRKH6h5Q5tnVbqcbd4oaJbBH7F7O3dklUPgThP7-Rcrx314Q5scydnizOb8XtwZCsg2hoEY6Q2v0BVsBtaAl6CMa2H4STAlI7rLyJGiZbXwJky-EgI3RTKpprVZYIHqfYRb5IJ6vD_hvkB1hWTOiBlOGTeVxAdeypDY*";
        String openID = "B7C04C6D624CE758BED547E970C9D32A";
        String openKey = "C18F10E9C5A14669E6F6248911309DFC";
        String amount = "20";
        String appID = "1450000490";
        String uuid = IdUtil.simpleUUID();
        String shtml = "";

        switch (appID) {
            //QB appID 1450000490
            case "1450000490":
                shtml = "https://pay.qq.com/h5/index.shtml?m=buy&c=qqacct_save&dialog=1&midasApiVersion=6&transactionid=" + uuid +
                        "&dh=1&pf=mds_storeopen_qb-2199_v1_0_0.search_v1-html5&version=2.13.13&readyState=complete&docWidth=375" +
                        "&docHeight=667&sinceNavigationStart=18552&closeOnSuccess=1&hidePrice=1&gray_new_ui=store" +
                        "&openid=" + openID + "&openkey=" + openKey + "&sessionid=openid&sessiontype=kp_accesstoken&u=&wxAppid2=wx951bdcac522929b6" +
                        "&n=" + amount + "&appid=" + appID + "&as=1";
                break;
            //王者红包 1450035065
            case "1450035065":
                break;
            //QQ华夏金元宝 1450000254
            case "1450000254":
                break;
            //QQ华夏银元宝 1450000252
            case "1450000252":
                break;
            //QQ华夏铜元宝 1450000253
            case "1450000253":
                break;
            //pc game 1450000238
            case "1450000238":
                break;
            //pc game 1450000238
            case "1450002258":
                break;
            default:
                String zoneID = "";
                shtml = "https://pay.qq.com/h5/index.shtml?m=buy&dialog=1&midasApiVersion=6&transactionid=" + uuid +
                        "&dh=1&pf=mds_storeopen_qb-2199_v1_0_0.common1_v1-html5&version=2.13.13&c=pcgame&appid=" + appID + "" +
                        "&readyState=complete&docWidth=400&docHeight=330&sinceNavigationStart=10098&closeOnSuccess=1&groupid=" +
                        "&gray_new_ui=store&openid=" + openID + "&openkey=" + openKey + "&qqAppid=101502376" +
                        "&sessionid=openid&sessiontype=kp_accesstoken&shownick=1&supercoupons=noCoupon&u=&wxAppid2=wx951bdcac522929b6" +
                        "&aid=mvip.pingtai.wechat.wxye_ktvip&n=" + amount + "&account=qq&zoneid=" + zoneID + "&as=1";
        }

        String html = HttpRequest.get(shtml)
                .cookie(ck)
                .header("Referer", "https://pay.qq.com/h5/index.shtml?r=0.9701233538808092")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                .header("Accept", " text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .execute().body();
//        System.out.println(html);

        String mKey = html.substring(html.indexOf("mkey") + 6, html.indexOf("mkey") + 129 + 5);
        String xMidasToken = html.substring(html.indexOf("xMidasToken") + 20 , html.indexOf("xMidasToken") + 96 + 20);
        String xMidasOps = html.substring(html.indexOf("xMidasOps") + 12, html.indexOf("]</script>") + 1);

        System.out.println(mKey);
        System.out.println(xMidasToken);
        System.out.println(xMidasOps);

        /**
         {
         "openid": "B7C04C6D624CE758BED547E970C9D32A",
         "openkey": "C18F10E9C5A14669E6F6248911309DFC",
         "qq_appid": "101502376",
         "pf": "mds_storeopen_qb-__mds_default_v1_0_0.qb-html5",
         "appid": "1450000238",
         "session_id": "openid",
         "session_type": "kp_accesstoken",
         "uuid": "D4B77B80-EB1F-4A6A-B3D8-56BE7DCF53BD",
         "xMidasToken": "E7C8B281148C47C91CD9D021729BA65017374C0581EB6D5DC7C3A0075B52D84B03E6CB72E19F597D7E00312037F50050",
         "mkey": "E86CDBACCB84586D35C73C0B5FD0869D0CC23B6014F0D5ED09E42B823F4338E8E199FDD86ED159BFA84831534688C828166D2B86DC1D018515EFFFCB44E478E0",
         "data": {
         "cr_flex": "1",
         "fk_tips": "1",
         "pay_method": "wechat",
         "zoneid": "",
         "buy_quantity": 20,
         "accounttype": "common",
         "service_name": "Q币",
         "isnewmpaymode": 1,
         "provide_uin": "",
         "game_info_mode": 1,
         "aid4open": "mvip.pingtai.wechat.wxye_ktvip",
         "biz_appid": "",
         "wx_direct_pay": 3,
         "wx_publice_pay": 1,
         "uuid": "C9DF9496-9F13-4D16-BD7E-6847390C5681",
         "pushtype": "NodeJS",
         "h5_schema": "default",
         "wcp": "type%3DCNY%26amt%3D5000"
         },
         "kp": "wt2E08TwXe2giaO1byZjhXgchi3j6M5EKi2J0NPc9k0yzoM3wjghRzxV2YtQbodvqfdW_LVBnzCsTt-fDoHEouA3QiNtbE6y6PFOQ6-6krkfsh83CHx32mM4M4yhPSvFXnK4iBzKHfLGBzgmX3YMUcu4WMQnyLq5aua",
         "ops_url": "https://pay.qq.com/h5/index.shtml?m=buy&c=qqacct_save&dialog=1&midasApiVersion=6&transactionid=D2FAE32F-DBC7-4C55-8960-AA513BD27D75&dh=1&pf=mds_storeopen_qb-__mds_default_v1_0_0.qb-html5&version=2.13.13&readyState=complete&docWidth=360&docHeight=740&innerHeight=740&sinceNavigationStart=101150&closeOnSuccess=1&hidePrice=1&gray_new_ui=store&openid=B7C04C6D624CE758BED547E970C9D32A&openkey=C18F10E9C5A14669E6F6248911309DFC&sessionid=openid&sessiontype=kp_accesstoken&u=&wxAppid2=wx951bdcac522929b6&n=20&appid=1450000490&as=1",
         "ops": []
         }
         */

        JSONObject msgBody = new JSONObject();
        msgBody.put("openid", openID);
        msgBody.put("openkey", openKey);
        msgBody.put("qq_appid", "101502376");
        msgBody.put("pf", "mds_storeopen_qb-__mds_default_v1_0_0.qb-html5");
        msgBody.put("appid", appID);
        msgBody.put("session_id", "openid");
        msgBody.put("session_type", "kp_accesstoken");
        msgBody.put("uuid", uuid);
        msgBody.put("xMidasToken", xMidasToken);
        msgBody.put("mkey", mKey);

        JSONObject msgData = new JSONObject();
        msgData.put("cr_flex", "1");
        msgData.put("fk_tips", "1");
        msgData.put("pay_method", "wechat");
        msgData.put("zoneid", "");
        msgData.put("buy_quantity", amount);
        msgData.put("accounttype", "common");
        msgData.put("service_name", "Q币");
        msgData.put("isnewmpaymode", "1");
        msgData.put("provide_uin", "");
        msgData.put("game_info_mode", "mvip.pingtai.wechat.wxye_ktvip");
        msgData.put("biz_appid", "");
        msgData.put("wx_direct_pay", 3);
        msgData.put("wx_publice_pay", 1);
        msgData.put("uuid", uuid);
        msgData.put("pushtype", "NodeJS");
        msgData.put("h5_schema", "default");
        msgData.put("wcp", "type%3DCNY%26amt%3D5000");

        msgBody.put("data", msgData);
        msgBody.put("kp", "wt2E08TwXe2giaO1byZjhXgchi3j6M5EKi2J0NPc9k0yzoM3wjghRzxV2YtQbodvqfdW_LVBnzCsTt-fDoHEouA3QiNtbE6y6PFOQ6-6krkfsh83CHx32mM4M4yhPSvFXnK4iBzKHfLGBzgmX3YMUcu4WMQnyLq5aua");
        msgBody.put("ops_url", shtml);
        msgBody.put("ops", xMidasOps);
        String encrypt_msg = HttpRequest.post("http://127.0.0.1:8088/v1/tx")
                .body(msgBody.toJSONString())
                .execute().body();

        System.out.println(encrypt_msg);

        String formUrl = "https://api.unipay.qq.com/v1/r/1450000490/mobile_save?pf=mds_storeopen_qb-__mds_default_v1_0_0.qb-html5&pfkey=pfkey&from_h5=1&session_token=D2FAE32F-DBC7-4C55-8960-AA513BD27D75&webversion=stdV2.16.0.1.android.other&r=0.41779251498660464&openid=B7C04C6D624CE758BED547E970C9D32A&openkey=C18F10E9C5A14669E6F6248911309DFC&session_id=openid&session_type=kp_accesstoken&qq_appid=&cr_flex=1&fk_tips=1&pay_method=wechat&zoneid=1&buy_quantity=20&accounttype=qb&provide_uin=&service_name=Q%E5%B8%81&biz_appid=&wx_direct_pay=3&wx_publice_pay=1&uuid=AB18971E-4EAB-44AB-9DFF-61CE9C0EC096&pushtype=NodeJS&h5_schema=default&wcp=&anti_auto_script_token_id=E86CDBACCB84586D35C73C0B5FD0869D0CC23B6014F0D5ED09E42B823F4338E8E199FDD86ED159BFA84831534688C82818718BA237070A215AC6E4197FC6A044&__refer=https%3A%2F%2Fpay.qq.com%2Fh5%2Findex.shtml%3Fr%3D0.9701233538808092&encrypt_msg=4ffe0efb4379393fd28a0307000a1ee26080983f3e815ddfddb91a891caf78c3f738b2ebde706b561aa80a68bfe21c08baf62394776a00398ab0e4adc6682b64919a08f66882b2670af6322d2548781603460120230e6c429ac3250fa19ea1b8280fb1926f787fb9d4b4250ca6460087a77fd7e0dc7ebf3a2be69250e492dac17df1061f396ddfe6c1068b0324b1aa8be083bde9eb07fbaf5fae94a511037f8ba050311a93fc32478b4cc2fc888b988cbeb8be8e953adf4ca82026711da038040997ee16385ad3bedaaa83bbfadfa73e8ab29d9a11cba43e2c392955d24a9ffdce8146588a8d98661bad92f34c57fb60c5f161a10a69e228aaf74f94b88df0f488fde76050449eacc4ff458d52e9cb1f26f368f9e05ca428deefae6c39aad64aebc5dac6dee38480a0410e27626ab8c49c197bdf042cd197b3cef133799c4ec6a0c42ae08ba0690128b14ec1249b63d3cecd3cdbdb1607ac4f3995a58456cf28633c6170d9aa16feaec5fcf59c46f6c83e9703413428fa13b429f493c58c562706fe281daec6eb93bf990d2f9abe0b4fd7e273157760e927b2fa2085600c4f003e6004468475a85561a10ef707b645830841038ee940f5d10ea0f23a68992e1a6471efb10c3f0af0e3cc7b07e368a080ebff072e1f0529e11b7d557e0f757ccf78107d1ef60324f57afb5742a3351113cad7cf97272632daec13e365d2cbe88ad24e24cf449f9e9c1dfcce5b19914a944b06124b2f534ba462868ccbd5a03f2df07cc4027c720d33d41c7bb14b067c9de2c64db67acef832c637358f7bc639d1eddcc20398c17425b2f86f0ad480e136&base_key_version=H5_1.0.21&encrypt_way=web_new_encrypt&web_token=627AAD693EDA27C1B3FB31752C1ACD7419B62531D2AA6CB0DDF7A713882F671FBDE62E192F2BFF119FBA02C8F9B393CC";


        URL urlPay = URLUtil.url(formUrl);
        Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
        Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);

        urlPayMap.put("encrypt_msg", encrypt_msg);
        urlPayMap.put("web_token", xMidasToken);
        urlPayMap.put("buy_quantity", amount);

        String payRs = HttpRequest.post("https://api.unipay.qq.com/v1/r/1450000490/mobile_save")
                .form(urlPayMap)
                .header("Referer", "https://pay.qq.com/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute().body();

        System.out.println(payRs);
    }


}