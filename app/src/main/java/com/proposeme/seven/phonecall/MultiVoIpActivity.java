package com.proposeme.seven.phonecall;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.proposeme.seven.phonecall.utils.mixAduioUtils.AudioUtil;
import com.proposeme.seven.phonecall.utils.mixAduioUtils.FileUtils;
import com.proposeme.seven.phonecall.utils.mixAduioUtils.MixAudioUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//实现多人语音通话的活动，现在只是实现两个音轨的合成。
public class MultiVoIpActivity extends AppCompatActivity implements View.OnClickListener {

    private AudioUtil mAudioUtil;
    private static final int BUFFER_SIZE = 1024 * 2;
    private byte[] mBuffer;

    private ExecutorService mExecutorService;
    private static final String TAG = "MainActivity";

    //跳转activity
    public static void newInstance(Context context) {
        context.startActivity(new Intent(context, MultiVoIpActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_voip);

        findViewById(R.id.first_start_record_button).setOnClickListener(this);
        findViewById(R.id.first_stop_record_button).setOnClickListener(this);
        findViewById(R.id.first_play).setOnClickListener(this);

        findViewById(R.id.second_start_record_button).setOnClickListener(this);
        findViewById(R.id.second_stop_record_button).setOnClickListener(this);
        findViewById(R.id.second_play).setOnClickListener(this);

        findViewById(R.id.beginMix).setOnClickListener(this);
        findViewById(R.id.MixaudioPlayer).setOnClickListener(this);
        mBuffer = new byte[BUFFER_SIZE];

        mExecutorService = Executors.newSingleThreadExecutor();
        mAudioUtil = AudioUtil.getInstance();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            //第一个录音操作
            case R.id.first_start_record_button: // 录音开始
                mAudioUtil.createFile("firstPcm.pcm");
                mAudioUtil.startRecord();
                mAudioUtil.recordData();
                break;
            case R.id.first_stop_record_button: // 录音结束
                mAudioUtil.stopRecord();
                break;
            case R.id.first_play: // 录音播放
                audioPlayer("firstPcm.pcm");
                break;

            //第二个录音操作
            case R.id.second_start_record_button:
                mAudioUtil.createFile("secondPcm.pcm");
                mAudioUtil.startRecord();
                mAudioUtil.recordData();
                break;
            case R.id.second_stop_record_button:
                mAudioUtil.stopRecord();
                break;
            case R.id.second_play:
                audioPlayer("secondPcm.pcm");
                break;

            //混音操作
            case R.id.beginMix:  //混音开始
                try {
                    byte[] realMixAudio = MixAudioUtil.averageMix(FileUtils.getFileBasePath()+"firstPcm.pcm",FileUtils.getFileBasePath()+"secondPcm.pcm");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.MixaudioPlayer:  //混音播放
                audioPlayer("averageMix.pcm");
                break;
        }
    }

    private void audioPlayer(String fileName) {
        //在播放的时候需要提前设置好录音文件。
        final File mAudioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/record/"+ fileName);
        mExecutorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                playAudio(mAudioFile); //读入传入的文件。
            }
        });
    }

    //将pcm文件读入并且进行播放。
    private void playAudio(File audioFile) //读入的是pcm文件。
    {
        Log.d("MainActivity" , "播放开始");
        int streamType = AudioManager.STREAM_MUSIC;   //按照音乐流进行播放
        int simpleRate = 44100;   //播放的赫兹
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int mode = AudioTrack.MODE_STREAM;

        int minBufferSize = AudioTrack.getMinBufferSize(simpleRate , channelConfig , audioFormat);
        AudioTrack audioTrack = new AudioTrack(streamType , simpleRate , channelConfig , audioFormat ,
                Math.max(minBufferSize , BUFFER_SIZE) , mode);
        audioTrack.play();
        Log.d(TAG , minBufferSize + " is the min buffer size , " + BUFFER_SIZE + " is the read buffer size");

        FileInputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream(audioFile); //读入pcm文件。
            int read;
            while ((read = inputStream.read(mBuffer)) > 0)
            {
                Log.d("MainActivity" , "录音开始 kaishi11111");

                audioTrack.write(mBuffer , 0 , read); //将文件流添加播放
            }
        }
        catch (RuntimeException | IOException e)
        {
            e.printStackTrace();
        }
    }
}
