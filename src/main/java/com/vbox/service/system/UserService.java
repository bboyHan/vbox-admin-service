package com.vbox.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.pojo.param.UserLoginParam;
import com.vbox.persistent.pojo.param.UserCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.UserInfoVO;
import com.vbox.persistent.pojo.vo.UserVO;

import java.util.List;

public interface UserService extends IService<User> {

    ResultOfList<List<UserVO>> listUser();

    int createOrUpdUser(UserCreateOrUpdParam userCreateOrUpdParam) throws Exception;

    int deleteUser(Long id) throws Exception;

    Boolean isAccountExist(String account);

    UserInfoVO login(UserLoginParam userLogin) throws Exception;

    UserInfoVO getUserInfo(String token) throws Exception;
}
