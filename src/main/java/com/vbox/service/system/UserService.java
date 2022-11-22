package com.vbox.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.pojo.param.UserLoginParam;
import com.vbox.persistent.pojo.param.UserParam;
import com.vbox.persistent.pojo.vo.UserInfoVO;
import com.vbox.persistent.pojo.vo.UserVO;

import java.util.List;

public interface UserService extends IService<User> {

    ResultOfList<List<UserVO>> listUser();

    int createOrUpdUser(UserParam userParam);

    int deleteUser(Long id);

    Boolean isAccountExist(String account);

    UserInfoVO login(UserLoginParam userLogin) throws Exception;

    UserInfoVO getUserInfo(String token) throws Exception;
}
