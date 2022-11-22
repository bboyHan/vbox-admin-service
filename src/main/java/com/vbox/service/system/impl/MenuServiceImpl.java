package com.vbox.service.system.impl;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.Menu;
import com.vbox.persistent.pojo.param.MenuParam;
import com.vbox.persistent.pojo.vo.MenuVO;
import com.vbox.persistent.repo.MenuMapper;
import com.vbox.persistent.repo.RelationRMMapper;
import com.vbox.service.system.MenuService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private RelationRMMapper rmMapper;

    @Override
    public List<MenuVO> listMenu() {

        List<Menu> menus = menuMapper.listMenu();

        // copy to new vo list
        List<MenuVO> tmpML = menus.stream().map(m -> {
            MenuVO menu = new MenuVO();
            BeanUtils.copyProperties(m, menu);
            return menu;
        }).collect(Collectors.toList());

        tmpML.forEach(t -> {
            List<MenuVO> childrenList = getChildrenList(t.getId(), tmpML);

            t.setChildren(childrenList);
            if (t.getPid() != 0) {
                t.setParentMenu(t.getPid());
            }
        });

        List<MenuVO> parentList = tmpML.stream().filter(t -> t.getPid().equals(0L)).collect(Collectors.toList());

        ResultOfList<List<MenuVO>> rs = new ResultOfList<>(parentList, parentList.size());

        return parentList;
    }

    @Override
    public int createOrUpdMenu(MenuParam menuParam) {


        Menu m = new Menu();
        BeanUtils.copyProperties(menuParam, m);

        Long pid = menuParam.getParentMenu();
        m.setPid(pid == null ? 0 : pid);

        if (m.getId() != null) {
            int i = menuMapper.updateById(m);
            return i;
        }

        m.setCreateTime(LocalDateTime.now());
        m.setKeepAlive(0);
        int insert = menuMapper.insert(m);
        return insert;
    }

    @Override
    public int deleteMenu(Long id) throws Exception {

        //1. if child menu exist, not allowed
        boolean isExist = menuMapper.isExistCMenu(id) != null;

        if (isExist) throw new Exception("child menu exist !");

        //2. del menu
        int i = menuMapper.deleteById(id);

        //3. del relation
        rmMapper.deleteByMid(id);
        return i;
    }

    public List<MenuVO> getChildrenList(Long id, List<MenuVO> list) {
        return list.stream().filter(t -> (t.getPid().equals(id))).collect(Collectors.toList());
    }
}
