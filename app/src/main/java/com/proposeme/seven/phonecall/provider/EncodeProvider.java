package com.proposeme.seven.phonecall.provider;

import android.util.Log;

import com.proposeme.seven.phonecall.net.Message;
import com.proposeme.seven.phonecall.net.NettyClient;
import com.proposeme.seven.phonecall.net.NettyReceiverHandler;
import com.proposeme.seven.phonecall.utils.MLOC;

/**
 * Describe: 将数据的编码、数据的发送、数据的接受进行结合。
 * 所有的网络提供类
 */

public class EncodeProvider {

    private NettyClient nettyClient; //初始化网络发送代理
    private static EncodeProvider provider;

    public static EncodeProvider getProvider() {
        if (provider != null) {
            return provider;
        }
        return null;
    }

    //整合发送类和接收类。 此为构造方法
    public EncodeProvider(String targetIp, int targetPort, int bindPort, final NettyReceiverHandler.FrameResultedCallback frameResultedCallback) {
        // 1配置client的信息，目标ip和端口。
        nettyClient = new NettyClient.
                Builder()
                .targetIp(targetIp)
                .targetport(targetPort)
                .localPort(bindPort)
                .frameResultedCallback(frameResultedCallback) //返回处理过后的数据
                .build();
        provider = this;
    }

    //发送文字指令
    public void setTestData(String msg) {
        nettyClient.sendData(msg, Message.MES_TYPE_NOMAL);
        Log.e("ccc","发送一条信息");
    }

    //发送音频数据
    public void sendAudioFrame(byte[] data) {
        nettyClient.UserIPSendData(MLOC.remoteIpAddress,data, Message.MES_TYPE_AUDIO);
    }

    //通过指定ip，发送文字指令
    public void UserIPSentTestData(String targetIp,String msg) {
        nettyClient.UserIPSendData(targetIp, msg, Message.MES_TYPE_NOMAL);
        Log.e("ccc","发送一条信息" + msg );
    }
    //通过指定IP，发送音频数据
    public void UserIpSendAudioFrame(String targetIp ,byte[] data) {
        nettyClient.UserIPSendData(targetIp ,data, Message.MES_TYPE_AUDIO);
    }

    //停止监听端口
    public void shutDownSocket(){
        nettyClient.shutDownBootstrap();
    }
}
