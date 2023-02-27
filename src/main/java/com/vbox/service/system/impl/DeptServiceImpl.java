package com.vbox.service.system.impl;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.Dept;
import com.vbox.persistent.pojo.param.DeptParam;
import com.vbox.persistent.pojo.vo.DeptVO;
import com.vbox.persistent.repo.DeptMapper;
import com.vbox.service.system.DeptService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Override
    public List<DeptVO> listDept() {

        List<Dept> depts = deptMapper.listDept();

        // copy to new vo list
        List<DeptVO> tmpML = depts.stream().map(m -> {
            DeptVO dept = new DeptVO();
            BeanUtils.copyProperties(m, dept);
            return dept;
        }).collect(Collectors.toList());

        tmpML.forEach(t -> {
            List<DeptVO> childrenList = getChildrenList(t.getId(), tmpML);

            t.setChildren(childrenList);
            if (t.getPid() != 0) {
                t.setParentDept(t.getPid());
            }
        });

        List<DeptVO> parentList = tmpML.stream().filter(t -> t.getPid() == 0).collect(Collectors.toList());

        return parentList;
    }

    public List<DeptVO> getChildrenList(Integer id, List<DeptVO> list) {
        return list.stream().filter(t ->
                (t.getPid().equals(id))
        ).collect(Collectors.toList());
    }

    @Override
    public int createDept(DeptParam deptParam) {
        Dept m = new Dept();
        BeanUtils.copyProperties(deptParam, m);

        Integer pid = deptParam.getParentDept();
        m.setPid(pid == null ? 0 : pid);

        if (m.getId() != null) {
            int i = deptMapper.updateById(m);
            return i;
        }

        m.setCreateTime(LocalDateTime.now());

        int insert = deptMapper.insert(m);

        return insert;
    }

    @Override
    public int deleteDept(Integer id) throws Exception {

        // 判断子部门是否存在
        int c = deptMapper.countByPid(id);
        if (c > 0) {
            throw new Exception("not allow to del because of sub dept exist!");
        }

        return deptMapper.deleteById(id);
    }
}
