package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeptVO {

    private Long id;
    private Long pid;
    private Long parentDept;
    private Long orderNo;
    private String deptName;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private List<DeptVO> children;

}
