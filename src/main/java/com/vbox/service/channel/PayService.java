package com.vbox.service.channel;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.PAccount;
import com.vbox.persistent.pojo.dto.PayInfo;
import com.vbox.persistent.pojo.param.*;
import com.vbox.persistent.pojo.vo.*;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface PayService extends IService<PAccount> {

    int createPAccount(PAccountParam param);

    void addProxy(String area, String payIp, String pr);

    Object createOrder(OrderCreateExtParam orderCreateExtParam, String area, String pr) throws Exception;

    Object createAsyncOrder(OrderCreateExtParam orderCreateExtParam, String area, String pr) throws Exception;

    Object createTestOrder(Integer num, String acid, String channel, String area, String pr, String payIp) throws Exception;

    OrderQueryVO queryOrderToP(OrderCreateParam orderCreateParam) throws Exception;

    JSONObject queryOrder(String orderId) throws Exception;

    JSONObject queryOrderForQuery(String orderId) throws Exception;

    ResultOfList<List<PAccountVO>> listPAccount() throws Exception;

    int delPAccount(Integer pid);

    int updPAccount(Integer pid, PAccountParam param) throws Exception;

    String preAuth(OrderPreAuthParam authParam) throws Exception;

    Object listOrderWait(OrderQueryParam queryParam);
    Object listOrder(OrderQueryParam queryParam);

    long orderCallback(OrderCallbackParam callbackParam) throws Exception;

    String getCK(String acAccount, String acPwd) throws IOException;

    String getCKforQuery(String acAccount, String acPwd) throws IOException;

    String testOrderCallback(String orderId) throws IllegalAccessException;

    String callbackOrder(String orderId, String captcha) throws IllegalAccessException;

    PayOrderCreateVO orderQuery(String orderId);

    OrderQueryVO queryAndCallback(String orderId) throws Exception;

    Object handleRealOrder(HttpServletRequest request, String orderId) throws Exception;

    Object tttt() throws IOException;

    Integer getBalance(String gateway, String ck, String acPwd) throws IOException;

    Integer getBalance2JXAcc(String gateway, CAccount c);
    JSONObject getBalanceJson2JXAcc(String gateway, CAccount c);
}
