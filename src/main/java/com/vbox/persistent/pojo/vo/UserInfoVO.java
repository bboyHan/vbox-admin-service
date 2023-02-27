package com.vbox.persistent.pojo.vo;

import com.vbox.common.enums.GenderEnum;
import com.vbox.persistent.entity.Role;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.entity.UserExt;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserInfoVO {

    private String token;

    private Integer id;
    private String nickname;
    private String account;
    private String avatar;
    private String gender;
    private List<Role> role;
    private LocalDateTime createTime;
    private String remark;

    //ext
    private String realName;
    private String country;
    private String desc;
    private String introduction;
    private String signature;
    private String address;
    private String phone;

    public UserInfoVO prop(User user, UserExt ext) {
        if (user != null) {
            this.id = user.getId();
            this.account = user.getAccount();
            this.nickname = user.getNickname();
            this.createTime = user.getCreate_time();
            this.remark = user.getRemark();
        }
        if (ext != null) {
            this.address = ext.getAddress();
            this.avatar = ext.getAvatar();
            this.country = ext.getCountry();
            this.phone = ext.getPhone();
            this.desc = ext.getDesc();
            this.introduction = ext.getIntroduction();
            this.realName = ext.getRealName();
            this.signature = ext.getSignature();
            this.gender = GenderEnum.of(ext.getGender());
        }
        return this;
    }
}
