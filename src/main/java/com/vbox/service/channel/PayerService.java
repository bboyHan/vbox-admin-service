package com.vbox.service.channel;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.PAccount;
import com.vbox.persistent.pojo.param.*;
import com.vbox.persistent.pojo.vo.OrderQueryVO;
import com.vbox.persistent.pojo.vo.PAOverviewVO;
import com.vbox.persistent.pojo.vo.PAccountVO;
import com.vbox.persistent.pojo.vo.PayOrderCreateVO;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public interface PayerService extends IService<PAccount> {

    int createPAccount(PAccountParam param);

    ResultOfList<List<PAccountVO>> listPAccount() throws Exception;

    int delPAccount(Integer pid);

    int updPAccount(Integer pid, PAccountParam param) throws Exception;

    ResultOfList<List<PAOverviewVO>> listPAccountOverview(PAOverviewParam param);
}
