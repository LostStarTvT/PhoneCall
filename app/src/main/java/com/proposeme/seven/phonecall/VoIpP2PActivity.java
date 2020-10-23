package com.proposeme.seven.phonecall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proposeme.seven.phonecall.audio.AudioDecoder;
import com.proposeme.seven.phonecall.net.BaseData;
import com.proposeme.seven.phonecall.net.IPSave;
import com.proposeme.seven.phonecall.net.NettyReceiverHandler;
import com.proposeme.seven.phonecall.provider.ApiProvider;
import com.proposeme.seven.phonecall.service.VoIPService;
import com.proposeme.seven.phonecall.utils.NetUtils;

import static com.proposeme.seven.phonecall.net.BaseData.IFS;
import static com.proposeme.seven.phonecall.net.BaseData.PHONE_ANSWER_CALL;
import static com.proposeme.seven.phonecall.net.BaseData.PHONE_CALL_END;
import static com.proposeme.seven.phonecall.net.BaseData.PHONE_MAKE_CALL;

// 此界面就是进行呼叫、接听、响铃、正常的切换
public class VoIpP2PActivity extends AppCompatActivity implements View.OnClickListener{

    private Chronometer timer; // 通话计时器
    private CountDownTimer mCountDownTimer; //打电话超时计时器

    // API控制对象
    private ApiProvider provider;

    private boolean isAnswer = false;  //是否接电话
    private boolean isBusy = false;  //是否正在通话中。true 表示正忙 false 表示为不忙。
    private String newEndIp = null; //检测通话结束ip是否合法。

    private EditText mEditText; //记录用户输入ip地址


    private VoIPService IPService;
    // 获取到service对象引用，获取到provider
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IPService = ((VoIPService.MyBinder) service).getService();
            provider = IPService.getProvider();
            // 只能在获取到provider 以后才能进行网络的初始化
            netInit();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    // 状态切换逻辑
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            //根据标志记性自定义的操作，这个操作可以操作主线程。
            if (msg.what == PHONE_MAKE_CALL) { //  接受方接收
                if (!isBusy){ //如果不忙 则跳转到通话界面。
                    showRingView(); //跳转到响铃界面。
                    isBusy = true;
                }
            }else if (msg.what == PHONE_ANSWER_CALL){ // 发送方接收
                showTalkingView();
                provider.startRecordAndPlay();
                isAnswer = true; //接通电话为真
                mCountDownTimer.cancel(); // 关闭倒计时定时器。
            }else if (msg.what == PHONE_CALL_END){ //收到通话结束的信息 接收方和发送方都可能接收。
                if (newEndIp.equals(provider.getTargetIP())){
                    showBeginView();
                    isAnswer = false;
                    isBusy = false;
                    provider.stopRecordAndPlay();
                    timer.stop();
                }
            }
        }
    };

    //跳转activity
    public static void newInstance(Context context) {
        context.startActivity(new Intent(context, VoIpP2PActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);

        //获取打电话ip，然后更新界面。
        setContentView(R.layout.activity_voip_p2p);
        findViewById(R.id.calling_view).setVisibility(View.GONE);
        findViewById(R.id.talking_view).setVisibility(View.GONE);
        findViewById(R.id.ring_view).setVisibility(View.GONE);
        findViewById(R.id.begin_view).setVisibility(View.GONE);
        findViewById(R.id.user_input_ip_view).setVisibility(View.GONE);

        ((TextView)findViewById(R.id.create_ip_addr)).setText(BaseData.LOCALHOST);
        timer = findViewById(R.id.timer);

        //设置挂断按钮
        findViewById(R.id.calling_hangup).setOnClickListener(this);
        findViewById(R.id.talking_hangup).setOnClickListener(this);
        findViewById(R.id.ring_pickup).setOnClickListener(this);
        findViewById(R.id.ring_hang_off).setOnClickListener(this);
        //设置手动输入ip
        findViewById(R.id.Create_button).setOnClickListener(this);
        findViewById(R.id.user_input_phoneCall).setOnClickListener(this);

        mEditText = findViewById(R.id.user_input_TargetIp);
        showBeginView();//显示初始的界面

        //拨打电话倒计时计时器。倒计时10s
        mCountDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                if (!isAnswer){ //如果没有人应答，则挂断
                    hangupOperation();
                    Toast.makeText(VoIpP2PActivity.this,"打电话超时，请稍后再试！",Toast.LENGTH_SHORT).show();
                }
            }
        };
        //启动服务。
        Intent intent = new Intent(this,VoIPService.class);
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);

        // 检测是否是由service启动的activity。
        boolean isFromService = getIntent().getBooleanExtra(IFS,false);
        if (isFromService){
            showRingView(); // 显示通话的界面。
            isBusy = true;
        }
    }

    /**
     *  自动关闭输入法
     * @param act 当前activity
     * @param v 绑定的控件。
     */
    public void hideOneInputMethod(Activity act, View v) {
        InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    //网络初始化操作
    private void netInit(){
        // 注册接口回调。
        provider.registerFrameResultedCallback(new NettyReceiverHandler.FrameResultedCallback() {
            //接受通话信令
            @Override
            public void onTextMessage(String msg) {

                // 发送来的信息总共有三种 100 200 300
                mHandler.sendEmptyMessage(Integer.parseInt(msg));
                /*
                 PHONE_MAKE_CALL = 100; //拨打电话
                 PHONE_ANSWER_CALL = 200; //接听电话   //此时需要进行界面的切换。将打电话切成通话界面。
                 PHONE_CALL_END = 300; //通话结束
                */
            }

            // 对于录音来说，需要知道对方的IP将音频流发送出去，而对于播放而言，只需要开启线程进行播放即可。
            @Override
            public void onAudioData(byte[] data) {
                if (isAnswer){
                    AudioDecoder.getInstance().addData(data, data.length);
                }
            }

            //获得对方返回过过来的ip地址
            @Override
            public void onGetRemoteIP(String ip) {
                newEndIp = ip; //每次都会记录新的ip。
                Log.e("ccc", "收到对方ip" + ip);
                if ((!ip.equals("")) && (!isBusy)){  //如果正忙那么就不能够更改自己的ip。 只能做无应答操作。
                    provider.setTargetIP(ip);
                }
            }
        });
    }

    //点击后退键触发的方法
    @Override
    public void onBackPressed(){

        // 这时候就需要重新进行注册监听。
        hangupOperation();// 这时候也会进行挂断。
        IPService.registerCallBack(); //重新注册监听打电话请求的监听。
        timer.stop();
        finish(); //确定以后调用退出方法
    }

    // 显示初始界面
    private void showBeginView(){
        findViewById(R.id.begin_view).setVisibility(View.VISIBLE);
        findViewById(R.id.talking_view).setVisibility(View.GONE);
        findViewById(R.id.ring_view).setVisibility(View.GONE);
        findViewById(R.id.calling_view).setVisibility(View.GONE);
        findViewById(R.id.user_input_ip_view).setVisibility(View.GONE);
    }

    // 显示用户输入ip界面
    private void showUserInputIpView(){
        findViewById(R.id.user_input_ip_view).setVisibility(View.VISIBLE);
        findViewById(R.id.talking_view).setVisibility(View.GONE);
        findViewById(R.id.ring_view).setVisibility(View.GONE);
        findViewById(R.id.calling_view).setVisibility(View.GONE);
        findViewById(R.id.begin_view).setVisibility(View.GONE);
        // 从缓存中找到IP地址。
        mEditText.setText(IPSave.getIP(this));
    }
    // 显示呼叫时候的view
    private void showCallingView(){
        findViewById(R.id.calling_view).setVisibility(View.VISIBLE);
        findViewById(R.id.talking_view).setVisibility(View.GONE);
        findViewById(R.id.ring_view).setVisibility(View.GONE);
        findViewById(R.id.begin_view).setVisibility(View.GONE);
        findViewById(R.id.user_input_ip_view).setVisibility(View.GONE);

        //开启定时器。
        mCountDownTimer.start();
    }
    //显示说话时候的view
    private void showTalkingView(){

        findViewById(R.id.talking_view).setVisibility(View.VISIBLE);
        findViewById(R.id.calling_view).setVisibility(View.GONE);
        findViewById(R.id.ring_view).setVisibility(View.GONE);
        findViewById(R.id.begin_view).setVisibility(View.GONE);
        findViewById(R.id.user_input_ip_view).setVisibility(View.GONE);
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
    }

    //显示响铃界面
    private void showRingView(){
        findViewById(R.id.ring_view).setVisibility(View.VISIBLE);
        findViewById(R.id.calling_view).setVisibility(View.GONE);
        findViewById(R.id.talking_view).setVisibility(View.GONE);
        findViewById(R.id.begin_view).setVisibility(View.GONE);
        findViewById(R.id.user_input_ip_view).setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        provider.disConnect();
    }

    //设置点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ring_pickup: //在响铃界面接电话
                showTalkingView();
                provider.sentTextData(PHONE_ANSWER_CALL.toString());
                // 开始发送语音信息
                provider.startRecordAndPlay();
                isAnswer = true; //接通电话为真
                break;
            case R.id.calling_hangup: //正在拨打中挂断
                hangupOperation();
                break;
            case R.id.talking_hangup: //通话中挂断
                hangupOperation();
                break;
            case R.id.ring_hang_off: //响铃中挂断
                hangupOperation();
                break;
            case R.id.Create_button: //手动输入ip地址
                showUserInputIpView();
                break;
            case R.id.user_input_phoneCall: // 拨打电话的入口
                //获取ip地址
                String ip = mEditText.getText().toString();
                // 检测是否为合法IP
                if (NetUtils.ipCheck(ip)){
                    provider.setTargetIP(ip);
                    //1 显示拨打界面
                    showCallingView();
                    isBusy = true;
                    //2 发送一条拨打电话的信息。
                    provider.sentTextData(PHONE_MAKE_CALL.toString());
                    IPSave.saveIP(this, ip); //保存IP
                    hideOneInputMethod(this,mEditText); // 隐藏输入法
                }else {
                    Toast.makeText(this,"IP格式不对，请重新输入~",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //进行挂断电话时候的逻辑
    private void hangupOperation(){
        provider.sentTextData(PHONE_CALL_END.toString());
        isBusy = false;
        showBeginView();
        isAnswer = false;
        provider.stopRecordAndPlay();
        timer.stop();
        mCountDownTimer.cancel();
    }

}
