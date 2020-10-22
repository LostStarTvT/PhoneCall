package com.proposeme.seven.phonecall.provider;

import android.util.Log;

import com.proposeme.seven.phonecall.audio.AudioPlayer;
import com.proposeme.seven.phonecall.audio.AudioRecorder;
import com.proposeme.seven.phonecall.net.Message;
import com.proposeme.seven.phonecall.net.NettyClient;
import com.proposeme.seven.phonecall.net.NettyReceiverHandler;

/**
 * Describe: 提供网络连接API 和控制逻辑API，这些都是在Service中进行依托。
 * 在进行停止录音与开启录音的时候，必须调用本API进行设置目标IP。
 */

public class ApiProvider {

    private NettyClient nettyClient; //初始化网络发送代理
    private static ApiProvider provider;
    private String targetIP = null; // 目标地址。

    // 需要先进行构造Netty，然后才能进行调用。
    public static ApiProvider getProvider() {
        if (provider == null) {
            provider = new ApiProvider();
        }
        return provider;
    }

    private AudioRecorder mAudioRecorder; //录音机
    private AudioPlayer mAudioPlayer; // 播放器。

    //构造方法
    private ApiProvider() {
        // 1配置client的信息，目标ip和端口。
        nettyClient = NettyClient.getClient();
        mAudioRecorder = AudioRecorder.getAudioRecorder();
        mAudioPlayer = AudioPlayer.getInstance();
        provider = this;
    }

    /**
     *  注册回调
     * @param callback 回调变量。
     */
    public void registerFrameResultedCallback(NettyReceiverHandler.FrameResultedCallback callback){
        nettyClient.setFrameResultedCallback(callback);
    }

    //发送文字指令 根据绑定的构造函数进行发送数据。
    public void sendTestData(String msg) {
        if (targetIP != null)
            nettyClient.UserIPSendData(targetIP, msg, Message.MES_TYPE_NORMAL);
        Log.e("ccc","发送一条信息");
    }

    /**
     * 发送音频数据
     * @param data 音频流
     */
    public void sendAudioFrame(byte[] data) {
        if (targetIP!= null)
            nettyClient.UserIPSendData(targetIP, data, Message.MES_TYPE_AUDIO);
        //需要处理为空的异常。
    }

    /**
     *  通过设置默认IP进行发送数据。
     * @param msg 消息
     */
    public void sentTextData(String msg) {
        if (targetIP != null)
            nettyClient.UserIPSendData(targetIP, msg, Message.MES_TYPE_NORMAL);
        Log.e("ccc","发送一条信息" + msg );
    }

    /**
     * 通过指定IP发送文本信息
     * @param targetIp 目标IP
     * @param msg 文本消息。
     */
    public void UserIPSentTextData(String targetIp, String msg) {
        if (targetIp != null)
            nettyClient.UserIPSendData(targetIp, msg, Message.MES_TYPE_NORMAL);
        Log.e("ccc","发送一条信息" + msg );
    }


    /**
     * 通过指定IP发送音频信息
     * @param targetIp 目标IP
     * @param data 数据流
     */
    public void UserIpSendAudioFrame(String targetIp ,byte[] data) {
        if (targetIp != null)
            nettyClient.UserIPSendData(targetIp ,data, Message.MES_TYPE_AUDIO);
    }

    /**
     * 关闭Netty客户端，
     */
    public void shutDownSocket(){
        nettyClient.shutDownBootstrap();
    }

    /**
     *  关闭连接，打电话结束
     * @return true or false
     */
    public boolean disConnect(){
        return  nettyClient.DisConnect();
    }

    /**
     *  获取目标地址
     * @return 此时目标地址。
     */
    public String getTargetIP() {
        return targetIP;
    }

    /**
     *  设置目标地址
     * @param targetIP 设置目标地址。
     */
    public void setTargetIP(String targetIP) {
        this.targetIP = targetIP;
    }

    /**
     * 开始录音 在开始以下操作之前，必须先把目标IP设置对，否则会出现问题。
     */
    public void startRecord(){
        mAudioRecorder.startRecording();
    }

    /**
     * 停止录音
     */
    public void  stopRecord(){
        mAudioRecorder.stopRecording();
    }

    /**
     *  录音线程是否正在录音
     * @return true 正在录音 or false 没有在录音
     */
    public boolean isRecording(){
        return mAudioRecorder.isRecording();
    }

    /**
     * 开始播放音频
     */
    public void startPlay(){
        mAudioPlayer.startPlaying();
    }

    /**
     * 停止播放音频
     */
    public void stopPlay(){
        mAudioPlayer.stopPlaying();
    }

    /**
     *  是否正在播放
     * @return true 正在播放;  false 停止播放
     */
    public boolean isPlaying(){
        return mAudioPlayer.isPlaying();
    }


    /**
     *  开启录音与播放
     */
    public void startRecordAndPlay(){
        startPlay();
        startRecord();
    }

    /**
     * 关闭录音与播放
     */
    public void stopRecordAndPlay(){
        stopRecord();
        stopPlay();
    }
}
