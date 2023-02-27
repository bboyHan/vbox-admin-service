package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.UserLoginParam;
import com.vbox.persistent.pojo.param.UserCreateOrUpdParam;
import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.UserInfoVO;
import com.vbox.persistent.pojo.vo.UserVO;
import com.vbox.service.channel.SaleService;
import com.vbox.service.system.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private SaleService saleService;

    @PostMapping("/login")
    public ResponseEntity<Result<Object>> login(@RequestBody UserLoginParam userLogin) throws Exception {

        UserInfoVO rs = userService.login(userLogin);
        return Result.ok(rs);
    }

    @GetMapping("/getPermCode")
    public ResponseEntity<Result<Object>> login2() throws Exception {

        List<String> rs = new ArrayList<>();
        rs.add("1000");
        rs.add("3000");
        rs.add("5000");
        return Result.ok(rs);
    }

    @GetMapping("/logout")
    public ResponseEntity<Result<Object>> logout() {

        return Result.ok(null);
    }

    @GetMapping("/getUserInfo")
    public ResponseEntity<Result<Object>> getUserInfo(@RequestHeader Map<String, String> headers, UserLoginParam param) throws Exception {
        String token = headers.get("authorization");
        UserInfoVO rs = userService.getUserInfo(token);
        return Result.ok(rs);
    }

    @GetMapping("/system/getAccountList")
    public ResponseEntity<Result<ResultOfList<List<UserVO>>>> listUser() {
        ResultOfList<List<UserVO>> rl = userService.listUser();

        return Result.ok(rl);
    }

    @PostMapping("/system/accountExist")
    public ResponseEntity<Result<Boolean>> isAccountExist(@RequestBody UserCreateOrUpdParam user) {

        try {
            Boolean rl = userService.isAccountExist(user.getAccount());
            if (rl) return Result.errBool(false);
            return Result.ok(rl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.errBool(false);
    }

//    @PostMapping("/code/createSub")
//    public ResponseEntity<Result<Integer>> createSub(@RequestBody UserSubCreateOrUpdParam subCreateOrUpdParam) {
//        int rl = 0;
//        try {
//            rl = userService.createSubAccount(subCreateOrUpdParam);
//            return Result.ok(rl);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return Result.ok(rl);
//    }

    @PostMapping("/system/user")
    public ResponseEntity<Result<Integer>> createOrUpdUser(@RequestBody UserCreateOrUpdParam userCreateOrUpdParam) {
        int role1 = 0;
        try {
            role1 = userService.createOrUpdUser(userCreateOrUpdParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(role1);
    }

    @DeleteMapping("/system/user")
    public ResponseEntity<Result<Integer>> deleteUser(@RequestBody Integer id) {
        int role1 = 0;
        try {
            role1 = userService.deleteUser(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(role1);
    }
}
