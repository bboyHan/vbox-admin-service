package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeptParam {

    private Integer id;
    private Integer pid;
    private Integer parentDept;
    private Integer orderNo;
    private String deptName;
    private String status;
    private String remark;
    private LocalDateTime createTime;

}
