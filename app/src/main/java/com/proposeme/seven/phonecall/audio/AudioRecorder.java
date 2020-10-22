package com.proposeme.seven.phonecall.audio;


import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.util.Log;

import com.gyz.voipdemo_speex.util.Speex;

/**
 * Describe: 进行音频录音 这个是音频录制的入口文件
 */
public class AudioRecorder  implements Runnable {
    String LOG = "Recorder";
    //音频录制对象
    private AudioRecord audioRecord;

    // 单例对象。
    private static AudioRecorder mAudioRecorder;
    //回声消除
    private AcousticEchoCanceler canceler;

    private Thread runner = null;
    private AudioRecorder(){
    }

    //是否正在录制
    private volatile boolean isRecording = false;

    // 获取单例模式实例。
    public static AudioRecorder getAudioRecorder(){
        if (mAudioRecorder == null){
            mAudioRecorder = new AudioRecorder();
        }
        return mAudioRecorder;
    }

    //开始录音的逻辑
    public void startRecording() {
        Log.e("ccc", "开启录音");
        //计算缓存大小
        int audioBufSize = AudioRecord.getMinBufferSize(AudioConfig.SAMPLERATE, AudioConfig.PLAYER_CHANNEL_CONFIG2, AudioConfig.AUDIO_FORMAT);
        //实例化录制对象
        if (null == audioRecord && audioBufSize != AudioRecord.ERROR_BAD_VALUE) {
            audioRecord = new AudioRecord(AudioConfig.AUDIO_RESOURCE,
                    AudioConfig.SAMPLERATE,
                    AudioConfig.PLAYER_CHANNEL_CONFIG2,
                    AudioConfig.AUDIO_FORMAT, audioBufSize);
        }
        //消回音处理
        assert audioRecord != null;
        initAEC(audioRecord.getAudioSessionId());
        runner = new Thread(this); //启动本线程。即实现接口的启动方式
        runner.start();
    }

    // 关闭录制
    public void stopRecording() {
        this.isRecording = false;
    }

    // 是否正在录制、
    public boolean isRecording() {
        return this.isRecording;
    }

    //消除回音
    public boolean initAEC(int audioSession) {
        if (canceler != null) {
            return false;
        }
        if (!AcousticEchoCanceler.isAvailable()){
            return false;
        }
        canceler = AcousticEchoCanceler.create(audioSession);
        canceler.setEnabled(true);
        return canceler.getEnabled();
    }

    @Override
    public void run() {

        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e("ccc", "unInit");
            return;
        }

        // 在录音之前实例化一个编码类，在编码类中实现的数据的发送。
        AudioEncoder encoder = AudioEncoder.getInstance();
        encoder.startEncoding();
        audioRecord.startRecording();

        this.isRecording = true;
        Log.e("ccc", "开始编码");
        int size = Speex.getInstance().getFrameSize();

        short[] samples = new short[size];

        while (isRecording) {
            int bufferRead = audioRecord.read(samples, 0, size);
            if (bufferRead > 0) {
                encoder.addData(samples,bufferRead);
            }
        }
        encoder.stopEncoding();
    }
}
