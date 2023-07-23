package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.pojo.dto.ChannelPreCount;
import com.vbox.persistent.pojo.param.ChannelPreParam;
import com.vbox.service.channel.ChannelPreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChannelPreController {

    @Autowired
    private ChannelPreService channelPreService;

    @GetMapping("/channel/pre/types")
    public ResponseEntity<Result<Object>> getChannelPreTypes(ChannelPreParam param) {
        List<CChannel> rs = channelPreService.getChannelPreTypes(param);
        return Result.ok(rs);
    }

    @GetMapping("/channel/pre/unUsedCount")
    public ResponseEntity<Result<Object>> unUsedCount(ChannelPreParam param) {
        List<ChannelPreCount> rs = channelPreService.countForCAccounts(param);
        return Result.ok(rs);
    }

    @GetMapping("/channel/pre/account")
    public ResponseEntity<Result<Object>> listChannelPreAccount() {
        List<CAccount> rs = channelPreService.listCAccount();
        return Result.ok(rs);
    }

    @GetMapping("/channel/pre")
    public ResponseEntity<Result<Object>> listChannelPre(ChannelPreParam param) {
        ResultOfList<List<ChannelPre>> rs = channelPreService.listChannelPre(param);
        return Result.ok(rs);
    }

    @PostMapping("/channel/pre")
    public ResponseEntity<Result<Integer>> createChannelPre(@RequestBody ChannelPreParam param) {
        int rl = channelPreService.createChannelPre(param);
        return Result.ok(rl);
    }

    @DeleteMapping("/channel/pre/{id}")
    public ResponseEntity<Result<Integer>> deleteChannelPre(@PathVariable Integer id) {
        int rl = channelPreService.deleteChannelPre(id);
        return Result.ok(rl);
    }

    @PutMapping("/channel/pre")
    public ResponseEntity<Result<Integer>> updateChannelPre(@RequestBody ChannelPreParam param) throws IOException {
        int rl = channelPreService.updateChannelPre(param);
        return Result.ok(rl);
    }

}
