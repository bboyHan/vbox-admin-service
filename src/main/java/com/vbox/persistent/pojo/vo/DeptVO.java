package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeptVO {

    private Integer id;
    private Integer pid;
    private Integer parentDept;
    private Integer orderNo;
    private String deptName;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private List<DeptVO> children;

}
