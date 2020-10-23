package com.proposeme.seven.phonecall.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.proposeme.seven.phonecall.VoIpP2PActivity;
import com.proposeme.seven.phonecall.net.NettyReceiverHandler;
import com.proposeme.seven.phonecall.provider.ApiProvider;

import static com.proposeme.seven.phonecall.net.BaseData.IFS;
import static com.proposeme.seven.phonecall.net.BaseData.PHONE_MAKE_CALL;

/**
 * 语音后台支持，当有语音通话的时候，会直接的跳转到对应的界面
 */
public class VoIPService extends Service {

    //音频播放的变量
    private ApiProvider provider; // 唯一的作用就是将这个变量保存到这里。

    public VoIPService() {

    }

    // 创建一个netty进行监听端口。
    @Override
    public void onCreate() {
        super.onCreate();
        provider = ApiProvider.getProvider();
        registerCallBack();
    }

    /**
     *  此接口只是单纯的监听PHONE_MAKE_CALL请求，
     */
    public void registerCallBack(){
        provider.registerFrameResultedCallback(new NettyReceiverHandler.FrameResultedCallback() {

            @Override
            public void onTextMessage(String msg) {
                Log.e("ccc", "收到消息" + msg);
                if (Integer.parseInt(msg) == PHONE_MAKE_CALL){
                    startActivity();
                }
            }

            @Override
            public void onAudioData(byte[] data) {

            }

            @Override
            public void onGetRemoteIP(String ip) {
                if ((!ip.equals(""))){  // 当IP不为空 则更改provider中的IP地址。
                    provider.setTargetIP(ip);
                }
            }
        });
    }

    public ApiProvider getProvider() {
       return provider;
    }

    /**
     * 返回一个Binder对象
     */
    @Override
    public IBinder onBind(Intent intent) {

        return new MyBinder();
    }
    // 这样外部就可以直接的获取到Service对象，然后就可以直接的操作。

    //1.service中有个类部类继承Binder，然后提供一个公有方法，返回当前service的实例。
    public class  MyBinder extends Binder {
        public VoIPService getService(){
            return VoIPService.this;
        }
    }

    // 从service中启动一个服务。
    private void  startActivity(){
        Intent intent = new Intent(this,VoIpP2PActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(IFS,true);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 关闭所有的组件。
        provider.shutDownSocket();
        provider.stopRecordAndPlay();
    }
}
