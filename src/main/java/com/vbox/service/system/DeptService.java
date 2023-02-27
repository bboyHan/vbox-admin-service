package com.vbox.service.system;

import com.vbox.persistent.pojo.param.DeptParam;
import com.vbox.persistent.pojo.vo.DeptVO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface DeptService {

    List<DeptVO> listDept();

    int createDept(DeptParam menuParam);

    int deleteDept(Integer id) throws Exception;
}
