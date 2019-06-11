package com.proposeme.seven.phonecall.utils.mixAduioUtils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Describe: 此工具类进行混音的测试工具类，1 实现录音 2 创建文件和文件夹。
 */
public class AudioUtil {
    private static AudioUtil mInstance;
    private AudioRecord recorder;
    //声音源
    private static int audioSource = MediaRecorder.AudioSource.MIC;
    //录音的采样频率
    private static int audioRate = 44100;
    //录音的声道，单声道
    private static int audioChannel = AudioFormat.CHANNEL_IN_STEREO;
    //量化的精度
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //缓存的大小
    private static int bufferSize = AudioRecord.getMinBufferSize(audioRate , audioChannel , audioFormat);
    //记录播放状态
    private boolean isRecording = false;
    //数字信号数组
    private byte[] noteArray;
    //PCM文件
    private File pcmFile;
    //wav文件
    private File wavFile;
    //文件输出流
    private OutputStream os;
    //文件根目录
    private String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/record/";

    private AudioUtil()
    {
    }

    //创建文件夹,首先创建目录，根据传递过来的文件名进行构建新的文件。
    public void createFile(String fileName)
    {

        File baseFile = new File(basePath);
        if (!baseFile.exists())
            baseFile.mkdirs(); //创建一个目录。

        pcmFile = new File(basePath + fileName);

        if (pcmFile.exists()) //检测文件是否存在
            pcmFile.delete();

        try
        {
            pcmFile.createNewFile();  //调用这个方法才会真实的进行文件的创建。
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //获取一个实例。
    public synchronized static AudioUtil getInstance()
    {
        if (mInstance == null)
        {
            mInstance = new AudioUtil();
        }
        return mInstance;
    }

    //读取录音数字数据线程
    class WriteThread implements Runnable
    {
        @Override
        public void run()
        {
            writeData();
        }
    }

    //录音线程执行体将录音信息写入文件。
    private void writeData()
    {
        noteArray = new byte[bufferSize];
        //建立文件输出流
        try
        {
            //首先新建一个输入流的文件，然后将录音的数据 以字节的方式写入到pcm文件中去。
            os = new BufferedOutputStream(new FileOutputStream(pcmFile));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        while (isRecording)
        {
            int recordSize = recorder.read(noteArray , 0 , bufferSize);
            if (recordSize > 0)
            {
                try
                {
                    os.write(noteArray);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        if (os != null)
        {
            try
            {
                os.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    //开始录音，设置录音参数
    public void startRecord()
    {
        recorder = new AudioRecord(audioSource , audioRate ,
                audioChannel , audioFormat , bufferSize);
        isRecording = true;
        recorder.startRecording();
    }

    //记录数据
    public void recordData()
    {
        new Thread(new WriteThread()).start();
    }

    //停止录音
    public void stopRecord()
    {
        if (recorder != null)
        {
            isRecording = false;
            recorder.stop(); //释放资源
            recorder.release();
            recorder = null;

        }
    }
}
