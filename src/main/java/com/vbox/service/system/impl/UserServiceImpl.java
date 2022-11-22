package com.vbox.service.system.impl;

import cn.hutool.system.UserInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vbox.common.ResultOfList;
import com.vbox.common.enums.GenderEnum;
import com.vbox.common.enums.LoginEnum;
import com.vbox.common.util.DistinctKeyUtil;
import com.vbox.persistent.entity.Role;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.entity.JoinUserRole;
import com.vbox.persistent.entity.UserLogin;
import com.vbox.persistent.pojo.param.UserLoginParam;
import com.vbox.persistent.pojo.param.UserParam;
import com.vbox.persistent.pojo.vo.UserInfoVO;
import com.vbox.persistent.pojo.vo.UserVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.system.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserExtMapper userExtMapper;
    @Autowired
    private UserLoginMapper userLoginMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RelationUDMapper udMapper;
    @Autowired
    private RelationURMapper urMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public ResultOfList<List<UserVO>> listUser() {
        List<JoinUserRole> users = userMapper.listUser();

        // copy to new vo list
        List<UserVO> tmpUL = users.stream().map(m -> {
            UserVO user = new UserVO();
            BeanUtils.copyProperties(m, user);
            String gender = GenderEnum.of(m.getGender());
            user.setGender(gender);

            String roles = getRolesStringByUser(m.getId(), users);

            user.setRoles(roles);
            return user;
        }).collect(Collectors.toList());

        List<UserVO> rsList = tmpUL.stream().filter(DistinctKeyUtil.distinctByKey(UserVO::getId)).collect(Collectors.toList());
        ResultOfList<List<UserVO>> rs = new ResultOfList<>(rsList, users.size());

        return rs;
    }

    private String getRolesStringByUser(Long id, List<JoinUserRole> users) {
        List<String> rl = users.stream().filter(u ->
                (id.equals(u.getId()))
        ).map(JoinUserRole::getRoleName).collect(Collectors.toList());

        String roles = String.join(",", rl);
        return roles;
    }

    private List<Role> getRolesByUser(Long id, List<JoinUserRole> users) {
        return users.stream().filter(u ->
                (id.equals(u.getId()))
        ).map(m -> {
            Role r = new Role();
            BeanUtils.copyProperties(m, r);

            r.setId(m.getRid());
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public int createOrUpdUser(UserParam userParam) {

        User u = new User();
        BeanUtils.copyProperties(userParam, u);

        //TODO 关联部门、角色
        System.out.println(userParam.getDeptId());
        System.out.println(userParam.getRoleId());


        if (userParam.getId() != null) {
            int i = userMapper.updateById(u);
            return i;
        }
        u.setCreateTime(LocalDateTime.now());

        userMapper.insert(u);

        return 0;
    }

    @Override
    public int deleteUser(Long id) {
        int i = userMapper.deleteById(id);
        return i;
    }

    @Override
    public Boolean isAccountExist(String account) {
        Integer exist = userMapper.isExistAccount(account);
        return exist != null;
    }


    /**
     * {
     * 	"code": 0,
     * 	"result": {
     * 		"roles": [{
     * 			"roleName": "Super Admin",
     * 			"value": "super"
     *                }],
     * 		"userId": "1",
     * 		"username": "vben",
     * 		"token": "fakeToken1",
     * 		"realName": "Vben Admin",
     * 		"desc": "manager"* 	},
     * 	"message": "ok",
     * 	"type": "success"
     * }
     */
    @Override
    public UserInfoVO login(UserLoginParam userLoginParam) throws Exception {

        //1. login check
        System.out.println(userLoginParam);

        UserLogin ul = new UserLogin();
        ul.setUsername(userLoginParam.getUsername());
        //2. login type
        LoginEnum loginEnum = LoginEnum.of(userLoginParam.getLoginType());
        ul.setLoginType(loginEnum.getType());
        //3. pass
        switch (loginEnum) {
            case ACCOUNT:
            case WECHAT:
            case QQ:
            case GITHUB:
                ul.setCaptcha(userLoginParam.getPassword());
                break;
        }

        UserLogin userLogin = userLoginMapper.validateLogin(ul);

        if (userLogin != null) {

            UserInfoVO rs = new UserInfoVO();
            BeanUtils.copyProperties(userLogin, rs);

            // create new token
            String token = "fakeToken1";
            rs.setToken(token);

            return rs;
        }

        throw new Exception("user not exist! ");
    }

    @Override
    public UserInfoVO getUserInfo(String token) throws Exception {
        /**
         * {
         * 	"code": 0,
         * 	"result": {
         * 		"userId": "1",
         * 		"username": "vben",
         * 		"realName": "Vben Admin",
         * 		"avatar": "https://q1.qlogo.cn/g?b=qq&nk=190848757&s=640",
         * 		"desc": "manager",
         * 		"password": "123456",
         * 		"token": "fakeToken1",
         * 		"homePath": "/dashboard/analysis",
         * 		"roles": [{
         * 			"roleName": "Super Admin",
         * 			"value": "super"
         *                }]* 	},
         * 	"message": "ok",
         * 	"type": "success"
         * }
         */

        //1. check user token
        String account = "";
        if (token != null) {
            account = "zhangsan";
            //2. get roles
            List<JoinUserRole> juList = userMapper.getUserByUserName(account);
            List<Role> roles = juList.stream().map(m -> {
                Role role = new Role();
                BeanUtils.copyProperties(m, role);
                role.setId(m.getRid());
                return role;
            }).collect(Collectors.toList());

            //3. get user/ ext
            UserInfoVO userInfo = userExtMapper.getUserInfoByAccount(account);
            String gender = GenderEnum.of(Integer.parseInt(userInfo.getGender()));
            userInfo.setGender(gender);
            userInfo.setRole(roles);

            return userInfo;
        } else {
            throw new Exception("token is not validate");
        }

    }
}
