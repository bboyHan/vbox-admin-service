package com.vbox.service.channel;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.PAccount;
import com.vbox.persistent.pojo.param.OrderCallbackParam;
import com.vbox.persistent.pojo.param.OrderCreateParam;
import com.vbox.persistent.pojo.param.OrderPreAuthParam;
import com.vbox.persistent.pojo.param.PAccountParam;
import com.vbox.persistent.pojo.vo.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface PayService extends IService<PAccount> {

    int createPAccount(PAccountParam param);

    Object createOrder(OrderCreateParam orderCreateParam) throws Exception;
//    Object createAsyncOrder(OrderCreateParam orderCreateParam) throws Exception;

    OrderQueryVO queryOrderToP(OrderCreateParam orderCreateParam) throws Exception;

    JSONObject queryOrder(String orderId) throws Exception;

    ResultOfList<List<PAccountVO>> listPAccount() throws Exception;

    int delPAccount(Integer pid);

    int updPAccount(Integer pid, PAccountParam param) throws Exception;

    String preAuth(OrderPreAuthParam authParam) throws Exception;

    Object listOrder();

    long orderCallback(OrderCallbackParam callbackParam) throws Exception;

    String getCK(String acAccount, String acPwd) throws IOException;

    String testOrderCallback(String orderId) throws IllegalAccessException;

    PayOrderCreateVO orderQuery(String orderId);
}
