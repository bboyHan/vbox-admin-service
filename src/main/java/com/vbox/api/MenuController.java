package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.persistent.pojo.param.MenuParam;
import com.vbox.persistent.pojo.vo.MenuVO;
import com.vbox.persistent.pojo.vo.RouteVO;
import com.vbox.service.system.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping("/system/getMenuList")
    public ResponseEntity<Result<List<MenuVO>>> listMenu() {
        List<MenuVO> rl = menuService.listMenu();
        return Result.ok(rl);
    }

    @GetMapping("/getMenuList")
    public ResponseEntity<Result<List<RouteVO>>> listM(@RequestHeader Map<String, String> headers) {
        String token = headers.get("authorization");
        List<RouteVO> rl = menuService.listRoute(token);
        return Result.ok(rl);
    }

    @PostMapping("/system/menu")
    public ResponseEntity<Result<Integer>> createMenu(@RequestBody MenuParam menuParam) {
        int role1 = menuService.createOrUpdMenu(menuParam);
        return Result.ok(role1);
    }

    @DeleteMapping("/system/menu")
    public ResponseEntity<Result<Integer>> deleteMenu(@RequestBody Long id) throws Exception {
        int role1 = menuService.deleteMenu(id);
        return Result.ok(role1);
    }
}
