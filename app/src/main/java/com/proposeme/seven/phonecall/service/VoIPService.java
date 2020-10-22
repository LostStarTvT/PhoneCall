package com.proposeme.seven.phonecall.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.proposeme.seven.phonecall.provider.ApiProvider;

/**
 * 语音后台支持，当有语音通话的时候，会直接的跳转到对应的界面，所以这时候就需要使用Fragment进行界面的切换。
 *
 * // 如何一直在后台进行监听语音的通话，
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

        // 这种其实直接使用fragment 会比较好用。
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 关闭所有的组件。
        provider.shutDownSocket();

        provider.stopRecordAndPlay();
    }
}
