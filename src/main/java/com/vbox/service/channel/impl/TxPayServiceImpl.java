package com.vbox.service.channel.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import com.vbox.persistent.pojo.param.TxPreAuthParam;
import com.vbox.service.channel.TxPayService;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
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
                        "&aid=mvip.pingtai.wechat.wxye_ktvip&n=" + amount + "&account=qq&zoneid="+zoneID+"&as=1";
        }

        URL urlPay = URLUtil.url(shtml);
        Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
        Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);

        return null;
    }
}
