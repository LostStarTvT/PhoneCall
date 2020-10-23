# PhoneCall

[English](README-En.md)

## 一、介绍 
基于netty框架开发的局域网IP电话，用户输入对方IP地址便能够进行语音通话。为了实现网络连接与Activity无关，使用service管理netty对象，现阶段可以在APP的任意界面监听到打电话请求，并跳转到响铃界面。另外，本项目将需要的操作封装到了ApiProvider类中。

ps：本项目是在[VideoCalling](<https://github.com/xmtggh/VideoCalling>)上进行改进的，该项目是实现局域网的视频传输，我将其语音传输抽取出来进行更改实现语音通话，感谢作者的无私贡献。

## 测试 

Releases v3为最新版本，将两个手机连接到同一个局域网中，一方输入对方IP便可以进行语音通话。网络监听线程设置在Service中，当监听到打电话请求便会直接跳转到响铃界面。

## 实现思路

首先使用AudioRecord进行音频录制，使用speex进行降噪并编码生成语音流，然后使用socket发送给出去， 接受方收到语音数据后将进行解码，然后使用AudioTrack播放。其中，电话交互逻辑使用文本信令控制。

对于群组通话，假设有ABC三人进行通话，首先A开启一个聊天室，然后BC加入聊天室，此时BC只需将自己的语音流发送给A，然后在A进行语音的合成操作，将合成的语音在本地播放和发送给BC即可。

## 界面展示

![call.png](https://pic.tyzhang.top/images/2020/10/23/call.png)
## 项目架构

- audio包：进行音频的录制、编码、解码、播放操作 
- net包：网络连接的包 
  - CallSingal:定义电话信令，如拨打电话操作 
  - Message: 传输数据，包括字节与音流和文字 
  - NettyClient: netty网络连接代理 
  - NettyReceiverHandler: 处理发送数据和接受数据，定义接口回调返回语音信息和电话信令信息
- **ApiProvider: 提供网络发送API，音频播放和录制API，连接断开等API。整个项目的入口文件。**
- mixAudioUtils: 混音用的工具类
- MultiVoIPActivity:实现混音界面，需要录制两端音频然后点击混音按钮，之后点击输出混音即可播放
- VoipP2PActivity:IP电话的主要界面，因为需要监听打电话的请求，但是不会写service进行后台监听，所以就在一个activity中写了五个界面进行切换..以后有机会可能会改

## 控制信令逻辑图

![PhoneCallCh.png](https://pic.tyzhang.top/images/2020/10/20/PhoneCallCh.png)

## 二、代码

### 0. API

打电话的逻辑是基于以下API实现，**包括音频的录制与播放，语音流的发送与接收，连接的断开与关闭，以上包括了实现PhoneCall所需要的全部功能。**

```java
public class ApiProvider {
    /**
     *  注册回调，处理接收到的音频和文本。
     * @param callback 回调变量。
     */
    public void registerFrameResultedCallback(NettyReceiverHandler.FrameResultedCallback callback){}

    /**
     * 发送音频数据
     * @param data 音频流
     */
    public void sendAudioFrame(byte[] data) {}

    /**
     *  通过设置默认IP进行发送数据。
     * @param msg 消息
     */
    public void sentTextData(String msg) {}
    /**
     * 通过指定IP发送文本信息
     * @param targetIp 目标IP
     * @param msg 文本消息。
     */
    public void UserIPSentTextData(String targetIp, String msg) {}
    /**
     * 通过指定IP发送音频信息
     * @param targetIp 目标IP
     * @param data 数据流
     */
    public void UserIpSendAudioFrame(String targetIp ,byte[] data) {}

    /**
     * 关闭Netty客户端，
     */
    public void shutDownSocket(){}

    /**
     *  关闭连接，打电话结束
     * @return true or false
     */
    public boolean disConnect(){}

    /**
     *  获取目标地址
     * @return 此时目标地址。
     */
    public String getTargetIP() {}

    /**
     *  设置目标地址
     * @param targetIP 设置目标地址。
     */
    public void setTargetIP(String targetIP) {}

    /**
     * 开始录音 在开始以下操作之前，必须先把目标IP设置对，否则会出现问题。
     */
    public void startRecord(){}

    /**
     * 停止录音
     */
    public void  stopRecord(){}

    /**
     *  录音线程是否正在录音
     * @return true 正在录音 or false 没有在录音
     */
    public boolean isRecording(){}

    /**
     * 开始播放音频
     */
    public void startPlay(){}

    /**
     * 停止播放音频
     */
    public void stopPlay(){}

    /**
     *  是否正在播放
     * @return true 正在播放;  false 停止播放
     */
    public boolean isPlaying(){}

    /**
     *  开启录音与播放
     */
    public void startRecordAndPlay(){}

    /**
     * 关闭录音与播放
     */
    public void stopRecordAndPlay(){}
}
```

### 1. 监听端口

每个客户端在启动时，都需要初始化一个Netty客户端进行监听请求，当收到请求以后，需要捕获发送方IP，然后进行主动的回复。

```java
Bootstrap b = new Bootstrap();
group = new NioEventLoopGroup();
try {
    //设置netty的连接属性。
    b.group(group)
        .channel(NioDatagramChannel.class) //异步的 UDP 连接
        .option(ChannelOption.SO_BROADCAST, true)
        .option(ChannelOption.SO_RCVBUF, 1024 * 1024)//接收区2m缓存
        .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65535))//加上这个，里面是最大接收、发送的长度
        .handler(handler); //设置数据的处理器
    b.bind(localPort).sync().channel().closeFuture().await();
} catch (Exception e) {
    e.printStackTrace();
} finally {
    group.shutdownGracefully();
}
```
### 2. 数据传输

主要包括两种数据：

1. 数字信令(Integer)：建立连接过程中的控制数字，发送的时候被转成String类型，(为了兼容Handler只能发送数字)。
2. 语音数据：通话中的语音数据

每次传输需要判断需要发送的是什么类型的数据，做相应的处理后装入运输载体Message对象中，最后用`ChannelHandlerContext`对象将转换为Json格式的Message对象发送至目标IP地址相应的端口。

```java
//发送数据。
public void sendData(String ip, int port, Object data, String type) {
     Message message = null;
     if (data instanceof byte[]) {
         message = new Message();
         message.setFrame((byte[]) data);
         message.setMsgtype(type);
         message.setTimestamp(System.currentTimeMillis());
     }else if (data instanceof String){
         message = new Message();
         message.setMsgBody((String) data);
         message.setMsgtype(type);
         message.setTimestamp(System.currentTimeMillis());
         message.setMsgIp(MLOC.localIpAddress);
     }
     if (channelHandlerContext != null) {
         channelHandlerContext.writeAndFlush(new DatagramPacket(
                 Unpooled.copiedBuffer(JSON.toJSONString(message).getBytes()),
                 new InetSocketAddress(ip, port)));
     }
 }
```

在进行接收数据的时候也是需要进行相同判断操作，然后进行数据的获取。

```java
//接收数据。
 ByteBuf buf = (ByteBuf) packet.copy().content(); //字节缓冲区
 byte[] req = new byte[buf.readableBytes()];
 buf.readBytes(req);
 String str = new String(req, "UTF-8");
 Message message = JSON.parseObject(str,Message.class);

Netty框架中有一个类SimpleChannelInboundHandler，主要是对监听的端口传来的数据进行处理的。自定义一个继承自它的类，并重写处理收到数据的方法channelRead0()，将数据写入Message对象。

//发送文字类型信息回调

if (message.getMsgtype().equals(Message.MES_TYPE_NOMAL)){
     if (frameCallback !=null){
         frameCallback.onTextMessage(message.getMsgBody());
         frameCallback.onGetRemoteIP(message.getMsgIp());
     }
 }else if (message.getMsgtype().equals(Message.MES_TYPE_AUDIO)){

//发送语音数据接口回调
     if (frameCallback !=null){
         frameCallback.onAudioData(message.getFrame());
     }
 }
```
### 3. 数字信令

总共的控制代码有三种:

```java
// control text
public static final Integer PHONE_MAKE_CALL = 100; // make call 
public static final Integer PHONE_ANSWER_CALL = 200; // answer call 
public static final Integer PHONE_CALL_END = 300; //  call end 
```

假设A向B进行打电话：

1. 当B收到*PHONE_MAKE_CALL*后，B需要先判断此时自己是否正忙（正在打电话），如果不忙则跳到响铃界面，否则直接丢包。
2. 当A收到B的PHONE_ANSWER_CALL后，则直接显示对话界面，开始录音并且将接电话标识设置为true。
3. 当A或者B收到PHONE_CALL_END后，此时需要判断发出此条结束消息的来源是否是正在通话的客户，防止在第三方进行呼叫是出现错误挂断的情形。

```java
// 状态切换逻辑
@SuppressLint("HandlerLeak")
private Handler mHandler = new Handler() {
    public void handleMessage(Message msg) {
        //根据标志记性自定义的操作，这个操作可以操作主线程。
        if (msg.what == PHONE_MAKE_CALL) { //收到打电话的请求。
            if (!isBusy){ //如果不忙 则跳转到通话界面。
                showRingView(); //跳转到响铃界面。
                isBusy = true;
            }
        }else if (msg.what == PHONE_ANSWER_CALL){ //接听电话
            showTalkingView();
            provider.startRecordAndPlay();
            isAnswer = true; //接通电话为真
        }else if (msg.what == PHONE_CALL_END){ //收到通话结束的信息
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
```
###  4.混音

混音采用的是平均混音算法，通过测试可以实现混音。主要涉及到安卓文件的创建和以字节流的方式进行文件的读取。

```java 
public static byte[] averageMix(String file1,String file2) throws IOException {

        byte[][] bMulRoadAudioes =  new byte[][]{
                FileUtils.getContent(file1),    //第一个文件
                FileUtils.getContent(file2)     //第二个文件
        };

        byte[] realMixAudio = bMulRoadAudioes[0]; //保存混音之后的数据。
        Log.e("ccc", " bMulRoadAudioes length " + bMulRoadAudioes.length); //2
        //判断两个文件的大小是否相同，如果不同进行补齐操作
        for (int rw = 0; rw < bMulRoadAudioes.length; ++rw) { //length一直都是等于2.依次检测file长度和file2长度
            if (bMulRoadAudioes[rw].length != realMixAudio.length) {
                Log.e("ccc", "column of the road of audio + " + rw + " is diffrent.");
                if (bMulRoadAudioes[rw].length<realMixAudio.length){
                    realMixAudio = subBytes(realMixAudio,0,bMulRoadAudioes[rw].length); //进行数组的扩展
                }
                else if (bMulRoadAudioes[rw].length>realMixAudio.length){
                    bMulRoadAudioes[rw] = subBytes(bMulRoadAudioes[rw],0,realMixAudio.length);
                }
            }
        }

        int row = bMulRoadAudioes.length;       //行
        int column = realMixAudio.length / 2;   //列
        short[][] sMulRoadAudioes = new short[row][column];
        for (int r = 0; r < row; ++r) {         //前半部分
            for (int c = 0; c < column; ++c) {
                sMulRoadAudioes[r][c] = (short) ((bMulRoadAudioes[r][c * 2] & 0xff) | (bMulRoadAudioes[r][c * 2 + 1] & 0xff) << 8);
            }
        }
        short[] sMixAudio = new short[column];
        int mixVal;
        int sr = 0;
        for (int sc = 0; sc < column; ++sc) {
            mixVal = 0;
            sr = 0;
            for (; sr < row; ++sr) {
                mixVal += sMulRoadAudioes[sr][sc];
            }
            sMixAudio[sc] = (short) (mixVal / row);
        }

        //合成混音保存在realMixAudio
        for (sr = 0; sr < column; ++sr) { //后半部分
            realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }

        //保存混合之后的pcm
        FileOutputStream fos = null;
        //保存合成之后的文件。
        File saveFile = new File(FileUtils.getFileBasePath()+ "averageMix.pcm" );
        if (saveFile.exists()) {
            saveFile.delete();
        }
        fos = new FileOutputStream(saveFile);// 建立一个可存取字节的文件
        fos.write(realMixAudio);
        fos.close();// 关闭写入流
        return realMixAudio; //返回合成的混音。
    }

    //合并两个音轨。
    private static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }
```

传入文件名称，返回文件内容的字节流
```java
    //将文件流读取到数组中，
    public static byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            Log.d("ccc","file too big...");
            return null;
        }
        FileInputStream fi = new FileInputStream(file);
        byte[] buffer = new byte[(int) fileSize];
        int offset = 0;
        int numRead = 0;
        //while循环会使得read一直进行读取，fi.read()在读取完数据以后会返回-1
        while (offset < buffer.length
                && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }
        //确保所有数据均被读取
        if (offset != buffer.length) {
            throw new IOException("Could not completely read file "
                    + file.getName());
        }
        fi.close();
        return buffer;
    }
```
万水千山总是情，点个star行不行~  
