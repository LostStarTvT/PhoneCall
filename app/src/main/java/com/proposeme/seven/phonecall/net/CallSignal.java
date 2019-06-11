package com.proposeme.seven.phonecall.net;

/**
 * Describe: 记录打电话时候后的交互信令
 */
public class CallSignal {
    public static final String PHONE_MAKE_CALL = "PHONE_MAKE_CALL"; //拨打电话

    public static final String PHONE_ANSWER_CALL = "PHONE_ANSWER_CALL"; //接听电话
    public static final String PHONE_IS_BUSY = "PHONE_IS_BUSY"; //通话中

    public static final String PHONE_CALL_END = "PHONE_CALL_END"; //通话结束
    public static final String PHONE_CALL_END_OK = "PHONE_CALL_END_OK"; //通话结束成功
}
