package com.proposeme.seven.phonecall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.proposeme.seven.phonecall.audio.AudioDecoder;
import com.proposeme.seven.phonecall.audio.AudioRecorder;
import com.proposeme.seven.phonecall.net.CallSignal;
import com.proposeme.seven.phonecall.net.NettyReceiverHandler;
import com.proposeme.seven.phonecall.provider.EncodeProvider;
import com.proposeme.seven.phonecall.users.User;
import com.proposeme.seven.phonecall.users.UserAdapter;
import com.proposeme.seven.phonecall.utils.HttpUtil;
import com.proposeme.seven.phonecall.utils.MLOC;
import com.proposeme.seven.phonecall.utils.NetUtils;
import com.proposeme.seven.phonecall.utils.UserNameUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

// 此界面就是进行呼叫、接听、响铃、正常的切换
// 需要实现的功能就是
// 1 建立通道，发送希望通话信令。
public class VoipP2PActivity extends AppCompatActivity implements View.OnClickListener{

    private Chronometer timer;
    private String TargetIp = "127.0.0.1"; //目标地址

    //音频播放的变量
    private EncodeProvider provider;
    private AudioRecorder audioRecorder; //记录音频。

    private boolean isAnswer = false;  //是否接电话
    private boolean isBusy = false;  //是否正在通话中。true 表示正忙 false 表示为不忙。
    private int port = 7777;
    private int localPort = 7777;
    private String newEndIp = "127.0.0.1"; //检测通话结束ip是否合法。

    private EditText mEditText; //记录用户输入ip地址
    //用户信息码识别
    private  final int phone_make_call = 100;
    private  final int phone_answer_call = 200;
    private  final int phone_call_end = 300;

    private CountDownTimer mCountDownTimer; //打电话超时计时器
    //显示服务器用户列表的变量
    ListView listView;
    UserAdapter adapter;
    List<User> userList = new ArrayList<>();

    //通过定时器进行更新列表
    @SuppressLint("HandlerLeak")
    Handler timeHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            updateData();
            timeHandler.sendEmptyMessageDelayed(0,2000);
        }
    };

    //作为监听器监听返接口回调收到的信息，然后在主线程上记性相应的操作。
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            //根据标志记性自定义的操作，这个操作可以操作主线程。
            if (msg.what == phone_make_call) { //收到打电话的请求。
                if (!isBusy){ //如果不忙 则跳转到通话界面。
                    showRingView(); //跳转到响铃界面。
                    isBusy = true;
                }
            }else if (msg.what == phone_answer_call){ //接听电话
                showTalkingView();
                audioRecorder.startRecording(); //开始语音播放。
                isAnswer = true; //接通电话为真
            }else if (msg.what == phone_call_end){ //收到通话结束的信息
                if (newEndIp.equals(TargetIp)){
                    showBeginView();
                    isAnswer = false;
                    isBusy = false;
                    audioRecorder.stopRecording(); //关闭录音和发送数据
                    timer.stop();
                }
            }
        }
    };

    //跳转activity
    public static void newInstance(Context context) {
        context.startActivity(new Intent(context, VoipP2PActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);

        audioRecorder = new AudioRecorder();//播放音频类
        //获取打电话ip，然后更新界面。
        setContentView(R.layout.activity_voip_p2p);
        findViewById(R.id.calling_view).setVisibility(View.GONE);
        findViewById(R.id.talking_view).setVisibility(View.GONE);
        findViewById(R.id.ring_view).setVisibility(View.GONE);
        findViewById(R.id.begin_view).setVisibility(View.GONE);
        findViewById(R.id.user_input_ip_view).setVisibility(View.GONE);

        ((TextView)findViewById(R.id.create_ip_addr)).setText(MLOC.localIpAddress);
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
        //初始化网络
        netInit();
        showBeginView();//显示初始的界面
        getServerUserList(); //获取服务器列表

        //拨打电话倒计时计时器。倒计时10s
        mCountDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                if (!isAnswer){ //如果没有人应答，则挂断
                    hangupOperation(TargetIp);
                    Toast.makeText(VoipP2PActivity.this,"打电话超时，请稍后再试！",Toast.LENGTH_SHORT).show();
                }

            }
        };
    }

    //网络初始化操作
    private void netInit(){
        //设置ip地址和端口号
        //1.初始化录音类。
        audioRecorder = new AudioRecorder();
        //2.接收对方发送过来的音频流  ip 目标ip地址，port 目标端口 localPort 本地监听端口
        provider = new EncodeProvider(TargetIp, port, localPort, new NettyReceiverHandler.FrameResultedCallback() {
            //接受通话信令
            @Override
            public void onTextMessage(String msg) {
                if (CallSignal.PHONE_MAKE_CALL.equals(msg)){
                    //收到邀请通话的信令
                    mHandler.sendEmptyMessage(phone_make_call);
                    Log.e("ccc", "收到make call ");
                }else if (CallSignal.PHONE_ANSWER_CALL.equals(msg)){
                    //收到对方接通的信令
                    //此时需要进行界面的切换。将打电话切成通话界面。
                    mHandler.sendEmptyMessage(phone_answer_call);
                    Log.e("ccc", "收到answer");
                }else if (CallSignal.PHONE_CALL_END.equals(msg)){
                    //收到对方挂断的信令
                    mHandler.sendEmptyMessage(phone_call_end);
                    Log.e("ccc", "收到phone_end");
                }
            }
            //接收到对方音频数据，进行解码和播放。
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
                    MLOC.remoteIpAddress = ip;
                    TargetIp = MLOC.remoteIpAddress;
                }
            }
        });

    }

    //点击后退键触发的方法
    @Override
    public void onBackPressed(){
        new AlertDialog.Builder(VoipP2PActivity.this).setCancelable(true)
                .setTitle("是否退出?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        timer.stop();
                        finish(); //确定以后调用退出方法
                    }
                }
        ).show();
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
        provider.shutDownSocket();
        updateOfflineState(); //进行下线操作
    }

    //设置点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ring_pickup: //在响铃界面接电话
                showTalkingView();
                provider.UserIPSentTestData(TargetIp,CallSignal.PHONE_ANSWER_CALL);
                audioRecorder.startRecording();// 开始发送语音信息
                isAnswer = true; //接通电话为真
                break;
            case R.id.calling_hangup: //正在拨打中挂断
                hangupOperation(TargetIp);
                break;
            case R.id.talking_hangup: //通话中挂断
                hangupOperation(TargetIp);
                break;
            case R.id.ring_hang_off: //响铃中挂断
                hangupOperation(TargetIp);
                break;
            case R.id.Create_button: //手动输入ip地址
                showUserInputIpView();
                break;
            case R.id.user_input_phoneCall: //手动输入ip地址进行拨打操作
                //获取ip地址
                TargetIp = mEditText.getText().toString();
                MLOC.remoteIpAddress = TargetIp;
                //1 显示拨打界面
                showCallingView();
                isBusy = true;
                //2 发送一条拨打电话的信息。
                provider.UserIPSentTestData(TargetIp,CallSignal.PHONE_MAKE_CALL);
                break;
        }
    }

    //进行挂断电话时候的逻辑
    private void hangupOperation(String targetIp){
        provider.UserIPSentTestData(targetIp,CallSignal.PHONE_CALL_END);
        isBusy = false;
        showBeginView();
        isAnswer = false;
        audioRecorder.stopRecording(); //关闭录音和发送数据
        timer.stop();
        mCountDownTimer.cancel();
    }

    //退出时候需要进行下线。
    public void updateOfflineState(){
        String username = UserNameUtil.getUsername(this);

        HttpUtil.sendOkHttpRequest(MLOC.baseURl+"server/stateServlet?username="+username, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }

    //获取用户列表
    private void getServerUserList() {
        //初始化网络ip信息显示
        listView = findViewById(R.id.list_view);
        adapter = new UserAdapter(this,R.layout.item_layout,userList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                User user = userList.get(i);
                 final String inputId = user.getIp();
                listMakeCall(inputId);
            }
        });
        timeHandler.sendEmptyMessageDelayed(0,2000);
    }

    //点击列表时候获得到的ip。并且进行拨打电话操作。最初的操作。
    private void listMakeCall(String inputId){
        showCallingView();
        //2 发送一条拨打电话的信息。
        TargetIp = inputId;
        MLOC.remoteIpAddress = inputId; //需要更新目标ip
        isBusy = true;
        provider.UserIPSentTestData(TargetIp,CallSignal.PHONE_MAKE_CALL);
        Log.e("ccc", "获取IP" + inputId);
    }

    //更新用户列表
    public void updateData(){
        HttpUtil.sendOkHttpRequest(MLOC.baseURl+ "server/dataServlet?ip="+ NetUtils.getIPAddress(this), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();

                if (responseData!=null){
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<User>>(){}.getType();
                    List<User> list = gson.fromJson(responseData, type);
                    userList.clear();
                    for (User user:list){
                        userList.add(user);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }
}
