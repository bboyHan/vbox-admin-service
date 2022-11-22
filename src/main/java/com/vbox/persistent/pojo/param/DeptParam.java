package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeptParam {

    private Long id;
    private Long pid;
    private Long parentDept;
    private Long orderNo;
    private String deptName;
    private String status;
    private String remark;
    private LocalDateTime createTime;

}
