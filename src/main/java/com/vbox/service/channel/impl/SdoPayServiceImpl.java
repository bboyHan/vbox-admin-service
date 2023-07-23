package com.vbox.service.channel.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.config.exception.ServiceException;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.pojo.dto.SdoWater;
import com.vbox.persistent.repo.CAccountMapper;
import com.vbox.persistent.repo.ChannelPreMapper;
import com.vbox.service.channel.SdoPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SdoPayServiceImpl implements SdoPayService {

    @Autowired
    private CAccountMapper cAccountMapper;
    @Autowired
    private ChannelPreMapper channelPreMapper;
    @Override
    public boolean tokenCheck(String sessionId) {
        String formUrl = "https://pay.sdo.com/api/orderlist?page=1&range=1";

        String payRs = HttpRequest.get(formUrl)
                .cookie("nsessionid=" + sessionId)
                .header("Referer", "https://pay.sdo.com/user/detail")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute().body();

//        log.warn("tx token校验结果: {}", payRs!= null? payRs.trim(): null);

        JSONObject userResp = JSONObject.parseObject(payRs);
        if (userResp == null) return false;
        Integer ret = userResp.getInteger("return_code");
        if (ret == null) return false;
        return ret == 0;

    }

    @Override
    public List<SdoWater> queryOrderBy2Day(String sessionId, String ckid) {
        String formUrl = "https://pay.sdo.com/api/orderlist?page=1&range=2";

        String payRs = HttpRequest.get(formUrl)
                .cookie("nsessionid=" + sessionId)
                .header("Referer", "https://pay.sdo.com/user/detail")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute().body();

//        log.warn("tx token校验结果: {}", payRs!= null? payRs.trim(): null);

        JSONObject userResp = JSONObject.parseObject(payRs);
        if (!userResp.getString("return_code").equals("0")){

            List<ChannelPre> pres = channelPreMapper.listChannelPreByCKID(ckid);
            CAccount ckAccount = cAccountMapper.getCAccountByAcid(ckid);
            for (ChannelPre pre : pres) {
                cAccountMapper.stopByACID("查单账号"+ ckAccount.getAcRemark()+", ck过期，关闭账号", pre.getAcid());
                log.error("查单账号{}, ck过期，关闭账号: {}", ckAccount.getAcRemark(), pre.getAcid());
            }

            cAccountMapper.stopByACID("查单账号ck过期，关闭账号", ckid);
            log.error("查单账号ck过期，关闭账号... resp: {}", payRs);

            throw new ServiceException("queryOrderBy2Day ex");
        }
        JSONObject data = userResp.getJSONObject("data");
        List<SdoWater> orders = data.getList("orders", SdoWater.class);

        return orders;
    }

}
