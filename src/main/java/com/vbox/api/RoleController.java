package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.RoleParam;
import com.vbox.persistent.pojo.param.RoleStatusParam;
import com.vbox.persistent.pojo.vo.RoleMenuVO;
import com.vbox.persistent.pojo.vo.RoleVO;
import com.vbox.service.system.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping("/system/getRoleListByPage")
    public ResponseEntity<Result<ResultOfList<List<RoleVO>>>> listRole() {
        ResultOfList<List<RoleVO>> rl = roleService.listRole();
        return Result.ok(rl);
    }

    @GetMapping("/system/getAllRoleList")
    public ResponseEntity<Result<List<RoleMenuVO>>> listAllRole() {
        List<RoleMenuVO> rl = roleService.listAllRole();
        return Result.ok(rl);
    }

    @PostMapping("/system/role")
    public ResponseEntity<Result<Integer>> createOrUpdRole(@RequestBody RoleParam role) {
        int role1 = 0;
        try {
            role1 = roleService.createOrUpdRole(role);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(role1);
    }

    @DeleteMapping("/system/role")
    public ResponseEntity<Result<Integer>> deleteRole(@RequestBody Integer id) {
        int role1 = 0;
        try {
            role1 = roleService.deleteRole(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(role1);
    }

    @PostMapping("/system/setRoleStatus")
    public ResponseEntity<Result<Integer>> setRoleStatus(@RequestBody RoleStatusParam roleStatusParam) {
        int role1 = 0;
        try {
            role1 = roleService.setRoleStatus(roleStatusParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(role1);
    }

}
