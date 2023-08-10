package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.VboxUserWallet;
import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.SaleVO;
import com.vbox.service.channel.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * 我的码商简单列表
     */
    @GetMapping("/sale/simple/list")
    public ResponseEntity<Result<Object>> listSaleUser() {
        List rs = saleService.listSaleUser();
        return Result.ok(rs);
    }

    /**
     * 一类核销的当前余额
     */
    @GetMapping("/sale/mng_self/recharge")
    public ResponseEntity<Result<Object>> mngSaleRecharge() {
        Object rs = saleService.getMngSaleRecharge();
        return Result.ok(rs);
    }

    /**
     * 我的码商积分
     */
    @GetMapping("/sale/recharge") //TODO
    public ResponseEntity<Result<Object>> listSaleRecharge(Integer page, Integer pageSize) {
        ResultOfList rs = saleService.listSaleRecharge(page, pageSize);
        return Result.ok(rs);
    }

    /**
     * 码商积分充值
     */
    @PostMapping("/sale/recharge")
    public ResponseEntity<Result<Object>> addSaleRecharge(@RequestBody VboxUserWallet param) {
        Object rs = saleService.addSaleRecharge(param.getUid(), param.getRecharge());
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
    public ResponseEntity<Result<Object>> listSaleCAccount(Integer status, String saleName, String acRemark, String channel, Integer page, Integer pageSize) {
        ResultOfList rs = saleService.listSaleCAccount(status, saleName, acRemark, channel, page, pageSize);
        return Result.ok(rs);
    }

    /**
     * 所有码商帐号
     */
    @GetMapping("/sale/cAccount/overview/today")
    public ResponseEntity<Result<Object>> listSaleCAccountOverview(Integer status, Integer page, Integer pageSize) {
        ResultOfList rs = saleService.listSaleCAOverviewToday();
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
