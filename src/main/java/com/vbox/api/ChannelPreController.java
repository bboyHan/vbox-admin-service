package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.pojo.dto.ChannelPreCount;
import com.vbox.persistent.pojo.param.ChannelPreBatchAcListParam;
import com.vbox.persistent.pojo.param.ChannelPreBatchParam;
import com.vbox.persistent.pojo.param.ChannelPreParam;
import com.vbox.service.channel.ChannelPreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChannelPreController {

    @Autowired
    private ChannelPreService channelPreService;

    @PostMapping("/channel/pre/upload")
    public ResponseEntity<Result<Integer>> batchChannelPreFile(HttpServletRequest request) {
        int rs = 0;
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        if (files.size() != 0) {
            MultipartFile multipartFile = files.get(0);
            rs = channelPreService.batchChannelPre(multipartFile);
        }
        return Result.ok(rs);
    }

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
        ChannelPreParam param = new ChannelPreParam();
        param.setStatus(1);
        List<CAccount> rs = channelPreService.listCAccount(param);
        return Result.ok(rs);
    }

    @GetMapping("/channel/pre/account/{channel}")
    public ResponseEntity<Result<Object>> listChannelPreAccountByChannel(@PathVariable String channel) {
        ChannelPreParam param = new ChannelPreParam();
        param.setChannel(channel);
        param.setStatus(1);
        List<CAccount> rs = channelPreService.listCAccount(param);
        return Result.ok(rs);
    }

    @GetMapping("/channel/pre")
    public ResponseEntity<Result<Object>> listChannelPre(ChannelPreParam param) {
        ResultOfList<List<ChannelPre>> rs = channelPreService.listChannelPre(param);
        return Result.ok(rs);
    }

    @PostMapping("/channel/pre")
    public ResponseEntity<Result<Integer>> createChannelPre(@RequestBody ChannelPreParam param) throws Exception {
        int rl = channelPreService.createChannelPre(param);
        return Result.ok(rl);
    }

    @PostMapping("/channel/pre/batch/acList")
    public ResponseEntity<Result<Integer>> batchCreateChannelPreForAcList(@RequestBody ChannelPreBatchAcListParam param) throws Exception {
        int rl = channelPreService.batchCreateChannelPreForAcList(param);
        return Result.ok(rl);
    }

    @PostMapping("/channel/pre/batch")
    public ResponseEntity<Result<Integer>> batchCreateChannelPre(@RequestBody ChannelPreBatchParam param) throws Exception {
        int rl = channelPreService.batchCreateChannelPre(param);
        return Result.ok(rl);
    }

    @DeleteMapping("/channel/pre/{id}")
    public ResponseEntity<Result<Integer>> deleteChannelPre(@PathVariable Integer id) {
        int rl = channelPreService.deleteChannelPre(id);
        return Result.ok(rl);
    }

    @DeleteMapping("/channel/pre/clear/{acid}")
    public ResponseEntity<Result<Integer>> clearChannelPre(@PathVariable String acid) {
        int rl = channelPreService.clearChannelPre(acid);
        return Result.ok(rl);
    }

    @PutMapping("/channel/pre")
    public ResponseEntity<Result<Integer>> updateChannelPre(@RequestBody ChannelPreParam param) throws IOException {
        int rl = channelPreService.updateChannelPre(param);
        return Result.ok(rl);
    }

}
