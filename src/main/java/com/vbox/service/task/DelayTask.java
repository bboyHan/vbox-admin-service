package com.vbox.service.task;

import lombok.Data;

@Data
public class DelayTask<T> {
    /**
     * 消息id
     */
    private String id;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 具体任务内容
     */
    private T task;
}
