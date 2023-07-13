package com.vbox.service.system.impl;

import cn.hutool.core.codec.Base32;
import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vbox.common.ResultOfList;
import com.vbox.common.enums.GenderEnum;
import com.vbox.common.enums.LoginEnum;
import com.vbox.common.util.DistinctKeyUtil;
import com.vbox.common.util.RandomNameUtil;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.param.UserLoginParam;
import com.vbox.persistent.pojo.param.UserCreateOrUpdParam;
import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.UserInfoVO;
import com.vbox.persistent.pojo.vo.UserVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.system.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserExtMapper userExtMapper;
    @Autowired
    private UserLoginMapper userLoginMapper;
    @Autowired
    private UserAuthMapper userAuthMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private RelationUDMapper udMapper;
    @Autowired
    private RelationURMapper urMapper;
    @Autowired
    private RelationUSMapper usMapper;

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

    private String getRolesStringByUser(Integer id, List<JoinUserRole> users) {
        List<String> rl = users.stream().filter(u ->
                (id.equals(u.getId()))
        ).map(JoinUserRole::getRoleName).collect(Collectors.toList());

        String roles = String.join(",", rl);
        return roles;
    }

    private List<Role> getRolesByUser(Integer id, List<JoinUserRole> users) {
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
    public int createOrUpdUser(UserCreateOrUpdParam userCreateOrUpdParam) throws Exception {

        User u = new User();
        BeanUtils.copyProperties(userCreateOrUpdParam, u);

        if (userCreateOrUpdParam.getId() != null) {
            int i = userMapper.updateById(u);
            return i;
        }
        //check account
        String account = u.getAccount();
        if (null == account) throw new Exception("account is null!");
        Integer exist = userMapper.isExistAccount(account);
        if (null != exist && exist != 0) throw new Exception("user is exist!");

        //setting time
        u.setCreate_time(LocalDateTime.now());
        //nickname
        u.setNickname(RandomNameUtil.randomChineseName());
        save(u);

        // user - ext
        UserExt userExt = new UserExt();
        userExt.setUid(u.getId());
        userExt.setAvatar("");
        userExt.setGender(GenderEnum.valid(userCreateOrUpdParam.getGender()));
        userExtMapper.insert(userExt);

        //user - login
        UserLogin userLogin = new UserLogin();
        userLogin.setUid(u.getId());
        userLogin.setUsername(u.getAccount());
        userLogin.setCaptcha(userCreateOrUpdParam.getPass() != null
                ? MD5.create().digestHex((userCreateOrUpdParam.getPass()))
                : MD5.create().digestHex("123456"));
        userLogin.setLoginType(LoginEnum.ACCOUNT.getType());
        userLogin.setCreateTime(LocalDateTime.now());
        userLogin.setRemark("账户登陆");
        userLoginMapper.insert(userLogin);

        //user - dept
        RelationUserDept ud = new RelationUserDept();
        ud.setUid(u.getId());
        ud.setDid(userCreateOrUpdParam.getDeptId());
        udMapper.insert(ud);

        //user - role
        RelationUserRole ur = new RelationUserRole();
        ur.setUid(u.getId());
        ur.setRid(userCreateOrUpdParam.getRoleId());
        urMapper.insert(ur);

        //auth
        UserAuth auth = new UserAuth();
        KeyPair rsa = SecureUtil.generateKeyPair("RSA");
        auth.setSecret(Base64.encode(rsa.getPrivate().getEncoded()));
        auth.setPub(Base64.encode(rsa.getPublic().getEncoded()));
        auth.setUid(u.getId());
        auth.setCreateTime(LocalDateTime.now());
        userAuthMapper.insert(auth);

        return 0;
    }

    @Override
    public int deleteUser(Integer id) throws Exception {

        //check user
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new Exception("user not exist!");
        }

        //del user
        int i = userMapper.deleteById(user.getId());

        //del user - role
        urMapper.deleteByUid(user.getId());

        //del user - dept
        udMapper.deleteByUid(user.getId());

        //del user - auth
        userAuthMapper.deleteByUid(user.getId());

        //del user - login
        userLoginMapper.deleteByUid(user.getId());

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
                ul.setCaptcha(MD5.create().digestHex(userLoginParam.getPassword()));
                break;
        }

        UserLogin userLogin = userLoginMapper.validateLogin(ul);

        if (userLogin != null) {

            UserInfoVO rs = new UserInfoVO();
            BeanUtils.copyProperties(userLogin, rs);

            // create new token
            UserAuth userAuth = userAuthMapper.getAuthByUid(userLogin.getUid());
            String secret = userAuth.getSecret();
            PrivateKey privateKey = SecureUtil.rsa(secret, null).getPrivateKey();

            //2. get roles
            String account = userLogin.getUsername();

            List<JoinUserRole> juList = userMapper.getUserByUserName(account);
            List<String> roleIds = juList.stream().map(m -> {
                Integer rid = m.getRid();
                if (rid == null) return null;
                return rid.toString();
            }).collect(Collectors.toList());

            //3. get menus
            List<JoinRoleMenu> rmList = roleMapper.listRoleInIds(roleIds);
            List<String> menuIds = rmList.stream().map(r -> {
                Integer mid = r.getMid();
                if (mid == null) return null;
                return mid.toString();
            }).collect(Collectors.toList());

            // setting expire time
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 1);

            String token = JWT.create()
                    .addHeaders(new HashMap<>())
                    .setPayload("username", account)
                    .setPayload("uid", userLogin.getUid())
                    .setPayload("mIds", menuIds)
                    .setPayload("pub", userAuth.getPub())
                    .setExpiresAt(calendar.getTime())
                    .sign(JWTSignerUtil.rs256(privateKey));

            rs.setToken(token);

            return rs;
        }

        throw new Exception("user not exist! ");
    }

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
    @Override
    public UserInfoVO getUserInfo(String token) throws Exception {

        if (token != null) {
            JWT jwt = JWTUtil.parseToken(token);
            String account = jwt.getPayload("username").toString();
            UserAuth auth = userAuthMapper.getAuthByAccount(account);
            String pub = auth.getPub();

            //1. check user token
            PublicKey pubKey = SecureUtil.rsa(null, pub).getPublicKey();

            boolean verify = JWTUtil.verify(token, JWTSignerUtil.rs256(pubKey));
//            System.out.println("verify: " + verify);
            // check expire time
            JWTValidator.of(token).validateDate();

            //2. get roles
            List<JoinUserRole> juList = userMapper.getUserByUserName(account);
            List<Role> roles = juList.stream().map(m -> {
                Role role = new Role();
                BeanUtils.copyProperties(m, role);
                role.setId(m.getRid());
                return role;
            }).collect(Collectors.toList());

            //3. get user/ ext
            User user = userExtMapper.getUserByAccount(account);
            UserExt userExt = userExtMapper.getUserInfoByUid(user.getId());

            UserInfoVO userInfoVO = new UserInfoVO();
            userInfoVO.prop(user, userExt);
            userInfoVO.setRole(roles);

            return userInfoVO;
        } else {
            throw new Exception("token is not validate");
        }

    }

    public static void main(String[] args) throws InterruptedException {

        RSA rsa = SecureUtil.rsa();
        PrivateKey privateKey = rsa.getPrivateKey();
        PublicKey publicKey = rsa.getPublicKey();
        String privateKeyBase64 = rsa.getPrivateKeyBase64();
        String publicKeyBase64 = rsa.getPublicKeyBase64();
        System.out.println(privateKeyBase64);
        System.out.println("-------------");
        System.out.println(publicKeyBase64);
        System.out.println("-------------");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, 1);

        String token = JWT.create()
                .addHeaders(new HashMap<>())
                .setPayload("username", "zhangSan")
                .setExpiresAt(calendar.getTime())
                .sign(JWTSignerUtil.rs256(privateKey));

        PublicKey pubKey = SecureUtil.rsa(null, publicKeyBase64).getPublicKey();

        boolean verify = JWTUtil.verify(token, JWTSignerUtil.rs256(pubKey));
        JWTValidator.of(token).validateDate();
        System.out.println("verify 1: " + verify);
        Thread.sleep(10000);
        boolean verify2 = JWTUtil.verify(token, JWTSignerUtil.rs256(pubKey));
        System.out.println("verify 2: " + verify2);
        JWTValidator.of(token).validateDate();
        JWT jwt = JWTUtil.parseToken(token);
    }

    private List<Menu> getMenuListByRole(Integer id, List<JoinRoleMenu> rmList) {

        List<Menu> menus = rmList.stream().filter(u ->
                (id.equals(u.getId()))
        ).map(rm -> {
            Menu menu = new Menu();
            BeanUtils.copyProperties(rm, menu);
            return menu;
        }).collect(Collectors.toList());

        return menus;
    }
}
