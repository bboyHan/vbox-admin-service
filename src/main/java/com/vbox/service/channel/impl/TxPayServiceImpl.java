package com.vbox.service.channel.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.persistent.pojo.dto.TxWaterList;
import com.vbox.persistent.pojo.param.TxPreAuthParam;
import com.vbox.service.channel.PayService;
import com.vbox.service.channel.TxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TxPayServiceImpl implements TxPayService {
    @Override
    public String preAuth(TxPreAuthParam authParam) {

        String openID = "";
        String openKey = "";
        String amount = "";
        String appID = "";
        String uuid = IdUtil.simpleUUID();
        String shtml = "";

        switch (appID) {
            //QB appID 1450000490
            case "1450000490":
                shtml = "https://pay.qq.com/h5/index.shtml?m=buy&c=qqacct_save&dialog=1&midasApiVersion=6&transactionid=" + uuid +
                        "&dh=1&pf=mds_storeopen_qb-2199_v1_0_0.search_v1-html5&version=2.13.13&readyState=complete&docWidth=375" +
                        "&docHeight=667&sinceNavigationStart=18552&closeOnSuccess=1&hidePrice=1&gray_new_ui=store" +
                        "&openid=" + openID + "&openkey=" + openKey + "&sessionid=openid&sessiontype=kp_accesstoken&u=&wxAppid2=wx951bdcac522929b6" +
                        "&n=" + amount + "&appid= " + appID + "&as=1";
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

        URL urlPay = URLUtil.url(shtml);
        Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
        Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);

        return null;
    }

    @Override
    public boolean tokenCheck(String openId, String openKey) {
        String formUrl = "https://graph.qq.com/user/get_user_info?oauth_consumer_key=101502376&access_token=86BCA68A8A456E65EC90109CC989DD98&openid=1D60346D2CDE3DAAD1328EDF74B10078&format=json";
        URL urlPay = URLUtil.url(formUrl);
        Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
        Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);

        urlPayMap.put("openid", openId);
        urlPayMap.put("access_token", openKey);

        String payRs = HttpRequest.post("https://graph.qq.com/user/get_user_info")
                .form(urlPayMap)
                .header("Referer", "https://pay.qq.com/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute().body();

        log.warn("tx token校验结果: {}", payRs != null ? payRs.trim() : null);

        JSONObject userResp = JSONObject.parseObject(payRs);
        if (userResp == null) return false;
        Integer ret = userResp.getInteger("ret");
        if (ret == null) return false;
        return ret == 0;

    }

    @Autowired
    private PayService payService;

    @Override
    public List<TxWaterList> queryOrderBy30(String openId, String openKey) {
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
        long startTime = entTime - pre_half_hour;
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

        if (ProxyInfoThreadHolder.getProxy() == null || ProxyInfoThreadHolder.getIpAddr() == null) {
            payService.addProxy(null, "127.0.0.1", null);
        }

        String payRs = HttpRequest.post("https://api.unipay.qq.com/v1/r/1450000490/trade_record_query")
                .form(urlPayMap)
                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                .header("Referer", "https://pay.qq.com/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute().body();
//        log.warn("tx trade_record_query, : {}" ,payRs);

        JSONObject jsonResp = JSONObject.parseObject(payRs);
        log.warn("tx trade_record_query, : {}", jsonResp.get("ret"));
        List<TxWaterList> txWaterListList = jsonResp.getList("WaterList", TxWaterList.class);
//        Map<String, Set<Integer>> s = new HashMap<>();
//
//        Set<Integer> amtSet = new HashSet<>();
//        for (TxWaterList wl : txWaterListList) {
//            Integer payAmt = wl.getPayAmt();
//            String provideID = wl.getProvideID();
//            amtSet.add(payAmt);
//            s.put(provideID, amtSet);
//        }

        return txWaterListList;
    }

    @Override
    public List<TxWaterList> queryOrderTXACBy30(String acAccount) {

        String formUrl = "https://api.unipay.qq.com/v1/r/1450000186/trade_record_query?" +
                "CmdCode=query2&SubCmdCode=default&PageNum=1&PageSize=200" +
                "&BeginUnixTime=1659803532&EndUnixTime=1691339532&SystemType=portal&pf=2199&pfkey=pfkey&from_h5=1" +
                "&session_token=63F728D4-74CB-4817-9F5D-3C344573837F1691339532798" +
                "&webversion=MidasTradeRecord1.0&r=0.10077481030292357" +
                "&openid=1528494424&openkey=openkey" +
                "&session_id=hy_gameid&session_type=st_dummy&__refer=" +
                "&encrypt_msg=ab00dc01d7748d2ea42b2f24971b6c52ba4ecee8b4b741031ffea3e0775f5e06edb08110ebba54a8dcc93fc9a7ff0a4bee0eb4f6ad2033d3c3b2a90e5d9547d1aa96750a759652b9fe44dbcb0dce4d19" +
                "&msg_len=76";

        long pre_half_hour = 30 * 60 * 1000;
        long entTime = System.currentTimeMillis() / 1000;
        long startTime = entTime - pre_half_hour;

        URL urlPay = URLUtil.url(formUrl);
        Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
        Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);
        urlPayMap.put("EndUnixTime", entTime);
        urlPayMap.put("BeginUnixTime", startTime);
//        urlPayMap.remove("EndUnixTime");
//        urlPayMap.remove("BeginUnixTime");
        urlPayMap.remove("SerialNo");
        urlPayMap.put("openid", acAccount);

        if (ProxyInfoThreadHolder.getProxy() == null || ProxyInfoThreadHolder.getIpAddr() == null) {
            payService.addProxy(null, "127.0.0.1", null);
        }
        String payRs;

        try {
            payRs = HttpRequest.post("https://api.unipay.qq.com/v1/r/1450000490/trade_record_query")
                    .form(urlPayMap)
                    .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                    .header("Referer", "https://pay.qq.com/")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .execute().body();
        } catch (Exception e) {
            payService.addProxy(null, "127.0.0.1", null);
            payRs = HttpRequest.post("https://api.unipay.qq.com/v1/r/1450000490/trade_record_query")
                    .form(urlPayMap)
                    .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                    .header("Referer", "https://pay.qq.com/")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .execute().body();
        }

//        log.warn("tx trade_record_query, : {}" ,payRs);

        JSONObject jsonResp = JSONObject.parseObject(payRs);
        log.warn("tx trade_record_query, : {}", jsonResp.get("ret"));
        List<TxWaterList> txWaterListList = jsonResp.getList("WaterList", TxWaterList.class);

        return txWaterListList;
    }
}
