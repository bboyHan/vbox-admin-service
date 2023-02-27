package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.persistent.pojo.param.DeptParam;
import com.vbox.persistent.pojo.vo.DeptVO;
import com.vbox.service.system.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping("/system/getDeptList")
    public ResponseEntity<Result<List<DeptVO>>> listDept() {
        List<DeptVO> rl = deptService.listDept();
        return Result.ok(rl);
    }

    @PostMapping("/system/dept")
    public ResponseEntity<Result<Integer>> createDept(@RequestBody DeptParam deptParam) {
        int role1 = 0;
        try {
            role1 = deptService.createDept(deptParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(role1);
    }

    @DeleteMapping("/system/dept")
    public ResponseEntity<Result<Integer>> deleteDept(@RequestBody Integer id) throws Exception {
        int role1 = 0;
        role1 = deptService.deleteDept(id);
        return Result.ok(role1);
    }
}
