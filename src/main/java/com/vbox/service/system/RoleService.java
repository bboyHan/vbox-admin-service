package com.vbox.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.Role;
import com.vbox.persistent.pojo.param.RoleParam;
import com.vbox.persistent.pojo.param.RoleStatusParam;
import com.vbox.persistent.pojo.vo.RoleMenuVO;
import com.vbox.persistent.pojo.vo.RoleVO;

import java.util.List;

public interface RoleService extends IService<Role> {

    ResultOfList<List<RoleVO>> listRole();

    int createOrUpdRole(RoleParam role);

    int deleteRole(Long id);

    int setRoleStatus(RoleStatusParam roleStatusParam);

    List<RoleMenuVO> listAllRole();
}
