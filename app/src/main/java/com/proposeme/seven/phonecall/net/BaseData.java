package com.proposeme.seven.phonecall.net;

/**
 * Describe: 记录打电话时候后的交互信令
 */
public class BaseData {

    // control text
    public static final Integer PHONE_MAKE_CALL = 100; //拨打电话
    public static final Integer PHONE_ANSWER_CALL = 200; //接听电话
    public static final Integer PHONE_CALL_END = 300; //通话结束

    // localhost
    public static String LOCALHOST = "127.0.0.1"; // 本机的IP地址，在发送文本数据的时候需要发送出去。

    // port
    public static final int PORT = 7777; // 监听端口。

    // isFromService
    public static final String IFS = "IFS";
}
