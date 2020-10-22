package com.proposeme.seven.phonecall;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.gyz.voipdemo_speex.util.Speex;
import com.proposeme.seven.phonecall.utils.MLOC;
import com.proposeme.seven.phonecall.utils.PermissionManager;
import com.yanzhenjie.permission.Permission;

import butterknife.ButterKnife;


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

        //点击之后跳转到打电话。并将ip等信息传递过去
        findViewById(R.id.phoneCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            VoIpP2PActivity.newInstance(MainActivity.this);
            }
        });

        //点击之后跳转到打电话。点击多人电话会议测试。
        findViewById(R.id.Multi_phoneCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            MultiVoIpActivity.newInstance(MainActivity.this);
            }
        });
        initPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 初始化权限事件
     */
    private void initPermission() {
        //检查权限
        PermissionManager.requestPermission(MainActivity.this, new PermissionManager.Callback() {
            @Override
            public void permissionSuccess() {
                PermissionManager.requestPermission(MainActivity.this, new PermissionManager.Callback() {
                    @Override
                    public void permissionSuccess() {
                        PermissionManager.requestPermission(MainActivity.this, new PermissionManager.Callback() {
                            @Override
                            public void permissionSuccess() {

                            }
                            @Override
                            public void permissionFailed() {
                            }
                        }, Permission.Group.STORAGE);
                    }

                    @Override
                    public void permissionFailed() {

                    }
                }, Permission.Group.MICROPHONE);
            }

            @Override
            public void permissionFailed() {

            }
        }, Permission.Group.CAMERA);
    }
}
