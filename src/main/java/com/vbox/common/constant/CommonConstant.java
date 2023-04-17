package com.vbox.common.constant;

public interface CommonConstant {
    /**
     * gee4
     */
    String CHANNEL_ACCOUNT_GEE = "gee_sec_code:";

    /**
     * 通道账户
     */
    String CHANNEL_ACCOUNT_QUEUE = "channel_account_queue:";

    /**
     * 超时队列
     */
    String ORDER_DELAY_QUEUE = "order_delay_queue";

    /**
     * 订单创建
     */
    String ORDER_CREATE_QUEUE = "order_create_queue";

    /**
     * 订单查询
     */
    String ORDER_QUERY_QUEUE = "order_query_queue";

    /**
     * 订单回调
     */
    String ORDER_CALLBACK_QUEUE = "order_callback_queue";


    /**
     * 订单创建等待
     */
    String ORDER_WAIT_QUEUE = "order_create_queue:wait:";

    String ACCOUNT_CK = "account:ck:";


//    String ENV_HOST = "http://47.94.207.53:9099";
//    String ENV_HOST = "http://47.97.200.110";
//    String ENV_HOST = "http://116.62.103.195";
//    String ENV_HOST = "http://59.110.141.171";
    String ENV_HOST = "http://116.62.24.139:10717";
//    String ENV_HOST = "http://mng.vboxjjjxxx.info";
    String ENV_HOST_PAY_URL = "http://116.62.24.139:10717/#/code/pay?orderId=";
//    String ENV_HOST_PAY_URL = "http://59.110.141.171/#/code/pay?orderId=";
//    String ENV_HOST_PAY_URL = "http://mng.vboxjjjxxx.info/#/code/pay?orderId=";
//    String ENV_HOST = "http://47.94.207.53";
}
