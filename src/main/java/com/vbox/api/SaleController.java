package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.CAEnableParam;
import com.vbox.persistent.pojo.param.CAccountParam;
import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.CGatewayVO;
import com.vbox.persistent.pojo.vo.SaleVO;
import com.vbox.persistent.pojo.vo.VboxUserVO;
import com.vbox.service.channel.ChannelService;
import com.vbox.service.channel.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SaleController {

    @Autowired
    private SaleService saleService;

    /**
     * 我的码商
     */
    @GetMapping("/sale/info")
    public ResponseEntity<Result<Object>> listSaleInfo() {
        List rs = saleService.listSaleInfo();
        return Result.ok(rs);
    }

    /**
     * 码商看板
     */
    @GetMapping("/sale/overview")
    public ResponseEntity<Result<Object>> listSaleOverView() {
        List<SaleVO> rs = saleService.listSaleOverView();
        return Result.ok(rs);
    }

    /**
     * 所有码商帐号
     */
    @GetMapping("/sale/cAccount")
    public ResponseEntity<Result<Object>> listSaleCAccount(Integer status, Integer page, Integer pageSize) {
        ResultOfList rs = saleService.listSaleCAccount(status,page, pageSize);
        return Result.ok(rs);
    }


    /**
     * 添加码商
     */
    @PostMapping("/sale/createSub")
    public ResponseEntity<Result<Integer>> createSub(@RequestBody UserSubCreateOrUpdParam subCreateOrUpdParam) throws Exception {
        int rl = saleService.createSub(subCreateOrUpdParam);
        return Result.ok(rl);
    }

}
