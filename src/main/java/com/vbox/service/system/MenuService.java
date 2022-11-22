package com.vbox.service.system;

import com.vbox.persistent.pojo.param.MenuParam;
import com.vbox.persistent.pojo.vo.MenuVO;

import java.util.List;

public interface MenuService {

    List<MenuVO> listMenu();

    int createOrUpdMenu(MenuParam menuParam);

    int deleteMenu(Long id) throws Exception;
}
