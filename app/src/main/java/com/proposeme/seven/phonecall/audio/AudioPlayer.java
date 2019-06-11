package com.proposeme.seven.phonecall.audio;


import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Describe:  接受解码音频流进行播放
 */
public class AudioPlayer implements  Runnable{
    String LOG = "AudioPlayer ";
    private static AudioPlayer player;

    private List<AudioData> dataList = null;
    private AudioData playData;
    private boolean isPlaying = false;

    private AudioTrack audioTrack;

    //媒体文件保存与读取。
    private File file;
    private FileOutputStream fos;

    private AudioPlayer() {
        //建立双向链表
        dataList = Collections.synchronizedList(new LinkedList<AudioData>());

        //文件播放测试。
        File mFile = new File(Environment.getExternalStorageDirectory(),"/audio");
        if (!mFile.exists()){
            if (mFile.mkdirs()){
                Log.d("ggh","创建成功");
            }else {
                Log.d("ggh","文件已存在！");
            }
        }

        file = new File(Environment.getExternalStorageDirectory(),"/audio/decode.amr");
        try {
            if (!file.exists())
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //获取播放音频实例
    public static AudioPlayer getInstance() {
        if (player == null) {
            player = new AudioPlayer();
        }
        return player;
    }

    //向播放流添加数据
    public void addData(short[] rawData, int size) {
        AudioData decodedData = new AudioData();
        decodedData.setSize(size);
        short[] tempData = new short[size];
        System.arraycopy(rawData, 0, tempData, 0, size);
        decodedData.setRealData(tempData);
        dataList.add(decodedData);
    }

    /*
     * 初始化播放器参数
     */
    private boolean initAudioTrack() {
        int bufferSize = AudioRecord.getMinBufferSize(AudioConfig.SAMPLERATE,
                AudioConfig.PLAYER_CHANNEL_CONFIG2,
                AudioConfig.AUDIO_FORMAT);
        if (bufferSize < 0) {
            Log.e(LOG, LOG + "initialize error!");
            return false;
        }
        Log.i(LOG, "Player初始化的 buffersize大小" + bufferSize);

        //设置播放器参数
        //MODE_STREAM 边读边播
        // STREAM_VOICE_CALL 表示用听筒播放。 STREAM_MUSIC 表示用扬声器
        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                AudioConfig.SAMPLERATE, AudioConfig.PLAYER_CHANNEL_CONFIG2,
                AudioConfig.AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);

        // set volume:设置播放音量
        audioTrack.setStereoVolume(1.0f, 1.0f);
        audioTrack.play();
        return true;
    }

    //播放数据的添加
    private void playFromList() throws IOException {
        while (isPlaying) {
            while (dataList.size() > 0) {
                playData = dataList.remove(0);
                audioTrack.write(playData.getRealData(), 0, playData.getSize());
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
        }
    }

    public void startPlaying() {
        if (isPlaying) {
            Log.e(LOG, "验证播放器是否打开" + isPlaying);
            return;
        }
        new Thread(this).start();
    }

    public void run() {
        this.isPlaying = true;
        //初始化播放器
        if (!initAudioTrack()) {
            Log.i(LOG, "播放器初始化失败");
            return;
        }
        Log.e(LOG, "开始播放");
        try {
            playFromList(); //开始播放
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.audioTrack != null) {
            if (this.audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                this.audioTrack.stop();
                this.audioTrack.release();
            }
        }
        Log.d(LOG, LOG + "end playing");
    }

    public void stopPlaying() {
        this.isPlaying = false;
    }
}
