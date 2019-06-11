package com.proposeme.seven.phonecall;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.proposeme.seven.phonecall.utils.HttpUtil;
import com.proposeme.seven.phonecall.utils.MLOC;
import com.proposeme.seven.phonecall.utils.NetUtils;
import com.proposeme.seven.phonecall.utils.PermissionManager;
import com.proposeme.seven.phonecall.utils.UserNameUtil;
import com.yanzhenjie.permission.Permission;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Describe: 注册用户界面
 */
public class RegisterActivity extends AppCompatActivity {
    EditText et1;
    EditText et2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        et1 = (EditText) findViewById(R.id.et_1);
        et2 = (EditText) findViewById(R.id.et_2);
        et2.setText(NetUtils.getIPAddress(this));
        findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog alertDialog=new AlertDialog.Builder(RegisterActivity.this).create();
                alertDialog.setTitle("温馨提示");
                alertDialog.setMessage("您确定要注册吗？");
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        register();
                        Toast.makeText(RegisterActivity.this,"注册成功",Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        });
        initPermission();
    }

    public void register(){
        final String param1 = et1.getText().toString();
        String param2 = et2.getText().toString();
        HttpUtil.sendOkHttpRequest(MLOC.baseURl+ "server/registerServlet?username="+param1+"&ip="+param2, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                UserNameUtil.saveUsername(RegisterActivity.this,param1);
            }
        });
    }

    /**
     * 初始化权限事件
     */
    private void initPermission() {
        //检查权限
        PermissionManager.requestPermission(RegisterActivity.this, new PermissionManager.Callback() {
            @Override
            public void permissionSuccess() {
                PermissionManager.requestPermission(RegisterActivity.this, new PermissionManager.Callback() {
                    @Override
                    public void permissionSuccess() {
                        PermissionManager.requestPermission(RegisterActivity.this, new PermissionManager.Callback() {
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
