package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.CAEnableParam;
import com.vbox.persistent.pojo.param.CAccountParam;
import com.vbox.persistent.pojo.param.TxCAccountParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.CGatewayVO;
import com.vbox.persistent.pojo.vo.VboxUserVO;
import com.vbox.service.channel.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @GetMapping("/channel/vboxUser")
    public ResponseEntity<Result<Object>> getVboxUser() {
        VboxUserVO rs = channelService.getVboxUser();
        return Result.ok(rs);
    }

    @GetMapping("/channel/vboxUser/view/orderSum")
    public ResponseEntity<Result<Object>> getVboxUserViewOrderSum() {
        List<Object> rs = channelService.getVboxUserViewOrderSum();
        return Result.ok(rs);
    }

    @GetMapping("/channel/vboxUser/view/orderNum")
    public ResponseEntity<Result<Object>> getVboxUserViewOrderNum() {
        List<Long> rs = channelService.getVboxUserViewOrderNum();
        return Result.ok(rs);
    }

    @PostMapping("/channel/CAccount")
    public ResponseEntity<Result<Integer>> createChannelAccount(@RequestBody CAccountParam caParam) {
        int rl = channelService.createChannelAccount(caParam);
        return Result.ok(rl);
    }
    @PostMapping("/channel/txCAccount")
    public ResponseEntity<Result<Integer>> createTxChannelAccount(@RequestBody TxCAccountParam caParam) {
        int rl = channelService.createTxChannelAccount(caParam);
        return Result.ok(rl);
    }
    @PostMapping("/channel/sdoCAccount")
    public ResponseEntity<Result<Integer>> createSdoChannelAccount(@RequestBody CAccountParam caParam) {
        int rl = channelService.createSdoChannelAccount(caParam);
        return Result.ok(rl);
    }

    @GetMapping("/channel/CAccount")
    public ResponseEntity<Result<ResultOfList<List<CAccountVO>>>> getCAccountList(CAccountParam caParam) {
        ResultOfList<List<CAccountVO>> rl = channelService.listCAccount(caParam);
        return Result.ok(rl);
    }

    @DeleteMapping("/channel/CAccount/{cid}")
    public ResponseEntity<Result<Integer>> delCAccount(@PathVariable Integer cid) {
        int rl = channelService.deleteCAccount(cid);
        return Result.ok(rl);
    }

    @PutMapping("/channel/CAccount/enable")
    public ResponseEntity<Result<Integer>> enableCAccount(@RequestBody CAEnableParam param) throws IOException {
        int rl = channelService.enableCAccount(param);
        return Result.ok(rl);
    }

    @PutMapping("/channel/CAccount")
    public ResponseEntity<Result<Integer>> updCAccount(@RequestBody CAccountParam param) throws IOException {
        int rl = channelService.updateCAccount(param);
        return Result.ok(rl);
    }

    @PutMapping("/channel/txCAccount")
    public ResponseEntity<Result<Integer>> updTxCAccount(@RequestBody TxCAccountParam param) throws IOException {
        int rl = channelService.updateTxCAccount(param);
        return Result.ok(rl);
    }

    @PutMapping("/channel/sdoCAccount")
    public ResponseEntity<Result<Integer>> updateSdoCAccount(@RequestBody CAccountParam param) throws IOException {
        int rl = channelService.updateSdoCAccount(param);
        return Result.ok(rl);
    }

    @GetMapping("/channel/gateway")
    public ResponseEntity<Result<List<CGatewayVO>>> getGatewayList(@RequestParam(value = "c_channel_id", required = false) String c_channel_id) {
        List<CGatewayVO> rl = new ArrayList<>();
        try {
            rl = channelService.getGatewayList(c_channel_id);
            return Result.ok(rl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.ok(rl);
    }

    @GetMapping("/channel/txQuery/{id}")
    public ResponseEntity<Result<Object>> getTxQuery(@PathVariable String id) {
        String rl = channelService.getTxQuery(id);
        return Result.ok(rl);
    }
}
