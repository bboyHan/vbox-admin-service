package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.Channel;
import com.vbox.persistent.entity.ChannelShop;
import com.vbox.persistent.pojo.dto.ChannelMultiShop;
import com.vbox.persistent.pojo.param.*;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.CGatewayVO;
import com.vbox.persistent.pojo.vo.VboxUserVO;
import com.vbox.service.channel.ChannelShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChannelShopController {

    @Autowired
    private ChannelShopService channelShopService;

    @GetMapping("/channel/shop/types")
    public ResponseEntity<Result<Object>> getChannelShopTypes(ChannelShopParam channelShopParam) {
        List<CChannel> rs = channelShopService.getChannelShopTypes(channelShopParam);
        return Result.ok(rs);
    }

    @GetMapping("/channel/shop")
    public ResponseEntity<Result<Object>> listChannelShop(ChannelShopParam channelShopParam) {
        ResultOfList<List<ChannelShop>> rs = channelShopService.listChannelShop(channelShopParam);
        return Result.ok(rs);
    }

    @GetMapping("/channel/shop/remark")
    public ResponseEntity<Result<Object>> listManageChannelShop(String shopRemark) {
        ResultOfList<List<ChannelShop>> rs = channelShopService.listManageChannelShop(shopRemark);
        return Result.ok(rs);
    }

    @GetMapping("/channel/shop/multi/remark")
    public ResponseEntity<Result<Object>> listMultiChannelShop(ChannelShopParam ChannelShopParam) {
        ResultOfList<List<ChannelMultiShop>> rs = channelShopService.listMultiChannelShop(ChannelShopParam);
        return Result.ok(rs);
    }

    @PostMapping("/channel/shop")
    public ResponseEntity<Result<Integer>> createChannelShop(@RequestBody ChannelShopParam channelShopParam) {
        int rl = channelShopService.createChannelShop(channelShopParam);
        return Result.ok(rl);
    }

    @DeleteMapping("/channel/shop/{id}")
    public ResponseEntity<Result<Integer>> deleteChannelShop(@PathVariable Integer id) {
        int rl = channelShopService.deleteChannelShop(id);
        return Result.ok(rl);
    }

    @DeleteMapping("/channel/shop/mark/{shopRemark}")
    public ResponseEntity<Result<Integer>> deleteChannelShop(@PathVariable String shopRemark) {
        int rl = channelShopService.deleteChannelShopByShopRemark(shopRemark);
        return Result.ok(rl);
    }

    @PutMapping("/channel/shop")
    public ResponseEntity<Result<Integer>> updateChannelShop(@RequestBody ChannelShopParam channelShopParam) throws IOException {
        int rl = channelShopService.updateChannelShop(channelShopParam);
        return Result.ok(rl);
    }

    @PutMapping("/channel/shop/enable")
    public ResponseEntity<Result<Integer>> enableChannelShop(@RequestBody CSEnableParam param) throws IOException {
        int rl = channelShopService.enableChannelShop(param);
        return Result.ok(rl);
    }

    @PutMapping("/channel/shop/address")
    public ResponseEntity<Result<Integer>> updateShopAddress(@RequestBody UpdateAddrParam param) throws IOException {
        String address = param.getAddress();
        Integer id = param.getId();
        int rl = channelShopService.updateShopAddress(address, id);
        return Result.ok(rl);
    }


    @PutMapping("/channel/shop/multi/enable")
    public ResponseEntity<Result<Integer>> multiEnableChannelShop(@RequestBody CSMultiEnableParam param) throws IOException {
        String shopRemark = param.getShopRemark();
        Integer status = param.getStatus();
        int rl = channelShopService.multiEnableChannelShop(shopRemark, status);
        return Result.ok(rl);
    }

}
