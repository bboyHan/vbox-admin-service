package com.vbox.service.system.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vbox.common.ResultOfList;
import com.vbox.common.util.DistinctKeyUtil;
import com.vbox.persistent.entity.JoinRoleMenu;
import com.vbox.persistent.entity.Menu;
import com.vbox.persistent.entity.RelationRoleMenu;
import com.vbox.persistent.entity.Role;
import com.vbox.persistent.pojo.param.RoleParam;
import com.vbox.persistent.pojo.param.RoleStatusParam;
import com.vbox.persistent.pojo.vo.RoleMenuVO;
import com.vbox.persistent.pojo.vo.RoleVO;
import com.vbox.persistent.repo.RelationRMMapper;
import com.vbox.persistent.repo.RoleMapper;
import com.vbox.service.system.RalationRMService;
import com.vbox.service.system.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RalationRMService rmService;

    @Autowired
    private RelationRMMapper rmMapper;

    public ResultOfList<List<RoleVO>> listRole() {
        List<JoinRoleMenu> rmList = roleMapper.listRole();

        List<RoleVO> tmpUL = rmList.stream().map(r -> {
            RoleVO rv = new RoleVO();
            BeanUtils.copyProperties(r, rv);

            List<Menu> menus = getMenuListByRole(r.getId(), rmList);
            rv.setMenu(menus);
            return rv;
        }).collect(Collectors.toList());

        List<RoleVO> rsList = tmpUL.stream().filter(DistinctKeyUtil.distinctByKey(RoleVO::getId)).collect(Collectors.toList());

        ResultOfList<List<RoleVO>> rl = new ResultOfList<>(rsList, rsList.size());

        return rl;
    }

    private List<Menu> getMenuListByRole(Long id, List<JoinRoleMenu> rmList) {

        List<Menu> menus = rmList.stream().filter(u ->
                (id.equals(u.getId()))
        ).map(rm -> {
            Menu menu = new Menu();
            BeanUtils.copyProperties(rm, menu);
            return menu;
        }).collect(Collectors.toList());

        return menus;
    }

    @Override
    public List<RoleMenuVO> listAllRole() {

        List<JoinRoleMenu> rmList = roleMapper.listRole();
        // copy to new vo list
        List<RoleMenuVO> tmpUL = rmList.stream().map(r -> {
            RoleMenuVO rv = new RoleMenuVO();
            BeanUtils.copyProperties(r, rv);

            List<Long> menus = getMenuIdsByRole(r.getId(), rmList);

            rv.setMenu(menus);
            return rv;
        }).collect(Collectors.toList());

        List<RoleMenuVO> rsList = tmpUL.stream().filter(DistinctKeyUtil.distinctByKey(RoleMenuVO::getId)).collect(Collectors.toList());

        return rsList;
    }

    private List<Long> getMenuIdsByRole(Long id, List<JoinRoleMenu> rmList) {
        List<Long> menuIds = rmList.stream().filter(u ->
                (id.equals(u.getId()))
        ).map(JoinRoleMenu::getMid).collect(Collectors.toList());
        return menuIds;
    }

    @Override
    public int createOrUpdRole(RoleParam role) {

        boolean isUpdate = role.getId() != null;

        Role rolePO = new Role();
        BeanUtils.copyProperties(role, rolePO);
        rolePO.setOrderNo(1L);
        rolePO.setCreateTime(LocalDateTime.now());

        saveOrUpdate(rolePO);

        if (isUpdate) {
            rmMapper.deleteByRid(role.getId());
            log.warn("role upd, menu list renew...{}", role.getMenu());
        }

        //get id
        Long rid = rolePO.getId();
        List<RelationRoleMenu> rmList = role.getMenu().stream().map(m -> {
            RelationRoleMenu rm = new RelationRoleMenu();
            rm.setMid(Long.parseLong(m));
            rm.setRid(rid);
            return rm;
        }).collect(Collectors.toList());

        rmService.saveOrUpdateBatch(rmList);

        return 0;
    }

    @Override
    public int deleteRole(Long id) {

        //1. del relation
        rmMapper.deleteByRid(id);

        //2. del role
        return roleMapper.deleteById(id);
    }

    @Override
    public int setRoleStatus(RoleStatusParam roleStatusParam) {
        Role updRole = new Role();

        updRole.setStatus(roleStatusParam.getStatus());
        updRole.setId(roleStatusParam.getId());

        return roleMapper.updateById(updRole);
    }


}
