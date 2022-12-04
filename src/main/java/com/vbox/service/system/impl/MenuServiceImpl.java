package com.vbox.service.system.impl;

import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.Menu;
import com.vbox.persistent.pojo.param.MenuParam;
import com.vbox.persistent.pojo.vo.MenuVO;
import com.vbox.persistent.pojo.vo.RouteMetaVO;
import com.vbox.persistent.pojo.vo.RouteVO;
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

        return parentList;
    }


    @Override
    public List<RouteVO> listRoute(String token) {

        List<String> ids = TokenInfoThreadHolder.getToken().getMIds();
        List<Menu> menus = menuMapper.listMenuInIds(ids);

        // copy to new vo list
        List<RouteVO> tmpML = menus.stream().map(m -> {
            RouteVO route = new RouteVO();
            route.setId(m.getId());
            route.setPid(m.getPid());
            route.setComponent(m.getComponent());
            route.setName(m.getMenuName());
            route.setPath(m.getRoutePath());
            route.setRedirect(m.getRedirect());

            RouteMetaVO meta = new RouteMetaVO();
            meta.setIcon(m.getIcon());
            meta.setHideMenu(m.getIsHide() == null ? null : m.getIsHide() != 0);
            meta.setCurrentActiveMenu(m.getCurrentActiveMenu());
            meta.setFrameSrc(m.getFrameSrc());
            meta.setShowMenu(m.getIsShow() == null ? null : m.getIsShow() != 0);
            meta.setIgnoreKeepAlive(m.getKeepAlive() == null ? null : m.getIsShow() != 0);
            meta.setTitle(m.getTitle());

            route.setMeta(meta);
            return route;
        }).collect(Collectors.toList());

        tmpML.forEach(t -> {
            List<RouteVO> childrenList = getChildrenRList(t.getId(), tmpML);

            t.setChildren(childrenList);
        });

        List<RouteVO> parentList = tmpML.stream().filter(t -> t.getPid().equals(0L)).collect(Collectors.toList());

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

    public List<RouteVO> getChildrenRList(Long id, List<RouteVO> list) {
        return list.stream().filter(t -> (t.getPid().equals(id))).collect(Collectors.toList());
    }
}
