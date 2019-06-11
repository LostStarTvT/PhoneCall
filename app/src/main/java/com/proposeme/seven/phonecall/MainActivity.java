package com.proposeme.seven.phonecall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.gyz.voipdemo_speex.util.Speex;
import com.proposeme.seven.phonecall.utils.HttpUtil;
import com.proposeme.seven.phonecall.utils.MLOC;
import com.proposeme.seven.phonecall.utils.UserNameUtil;

import java.io.IOException;

import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.proposeme.seven.phonecall.utils.NetUtils.getIPAddress;

//p2p电话的主界面。
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        MLOC.localIpAddress = getIPAddress(this); //获取本机ip地址
        Speex.getInstance().init();
        if(checkIsFirstRun()){
            Intent intent = new Intent(this,RegisterActivity.class);
            startActivity(intent);
        }
        //点击之后跳转到打电话。并将ip等信息传递过去
        findViewById(R.id.phoneCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateOnlineState(); //更新用户状态。
                VoipP2PActivity.newInstance(MainActivity.this);
            }
        });

        //点击之后跳转到打电话。点击多人电话会议测试。
        findViewById(R.id.Multi_phoneCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                updateOnlineState(); //更新用户状态。
                MultiVoIpActivity.newInstance(MainActivity.this);
            }
        });

        Log.e("ccc", "文件路径" + getFilesDir());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //检测是否为第一次运行
    public boolean checkIsFirstRun(){
        SharedPreferences preferences = getSharedPreferences("isFirstRun",MODE_PRIVATE);
        boolean isFirstRun = preferences.getBoolean("key",true);
        if(isFirstRun){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("key",false);
            editor.apply();
        }
        return isFirstRun;
    }

    // 更新上线状态
    public void updateOnlineState(){
        String username = UserNameUtil.getUsername(this);
        HttpUtil.sendOkHttpRequest( MLOC.baseURl+"server/stateServlet1?username="+username+"&ip="+ getIPAddress(MainActivity.this), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"上线成功",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
