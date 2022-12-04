package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.UserLoginParam;
import com.vbox.persistent.pojo.param.UserCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.UserInfoVO;
import com.vbox.persistent.pojo.vo.UserVO;
import com.vbox.service.system.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Result<Object>> login(@RequestBody UserLoginParam userLogin) throws Exception {

        UserInfoVO rs = userService.login(userLogin);
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
    public ResponseEntity<Result<Integer>> deleteUser(@RequestBody Long id) {
        int role1 = 0;
        try {
            role1 = userService.deleteUser(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(role1);
    }
}
