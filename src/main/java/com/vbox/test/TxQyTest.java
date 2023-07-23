package com.vbox.test;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.util.CommonUtil;

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
    public static void main(String[] args) throws Exception {

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