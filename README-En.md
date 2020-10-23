# PhoneCall
# 1. Introduce

This is a LAN IP phone developed based on the Netty framework, the user can enter the IP address of the other party to make a phone call. In order to realize that the network connection is independent of Activity, the service is used to manage the netty object. At this stage, the call request can be monitored on any interface of the application and jump to the ringing interface. In addition, this project encapsulates the required operations into the ApiProvider class.

PS: This project is improved on [VideoCalling](https://github.com/xmtggh/VideoCalling), thanks to the author’s contribution.

## Test Demo

Preparation:  Two mobile phones A and B.

Releases v3 is the latest version. When two mobile phones are connected to the same LAN, one party can enter the other party's IP to make a phone call. The network monitoring thread is set in the Service, and when a call request is monitored, it will directly jump to the ringing interface.

##  Implement

Firstly, using `AudioRecord` for recording, and  then use the `speex`for audio encoding to generate a byte stream and use `Netty` to send the byte stream. When receiver receive byte steam, the receiver first decodes it, and then uses `AudioTrack` to play it. Besides, the control logic uses number to control.  

## Interface Display

![call.png](https://pic.tyzhang.top/images/2020/10/23/call.png)
## Project structure

- audio package: Implement audio recording, encoding, decoding, and playback functions
- net package: Implement network connection
  - CallSingal: Define phone control text
  - Message: Transmission of data, including bytes and audio streams and text
  - NettyClient:  netty network connection client
  - NettyReceiverHandler: Process sending data and receiving data, use interface callback to return voice and control text
- **ApiProvider:  Provide API for sending text and audio streams. The core API file of the project.**
- MultiVoIPActivity: for mixing audio.
- VoipP2PActivity: The main interface of the PhoneCall

##  control signaling logic diagram

![PhoneCallEn.png](https://pic.tyzhang.top/images/2020/10/20/PhoneCallEn.png)

# 2. Code
### 0.API

The logic of making a call is implemented based on the following API, including audio recording and play, voice streaming sending and receiving, connection disconnection and closing.

```java
public class ApiProvider {

    public void registerFrameResultedCallback(NettyReceiverHandler.FrameResultedCallback callback){}

    public void sendAudioFrame(byte[] data) {}

    public void sentTextData(String msg) {}

    public void UserIPSentTextData(String targetIp, String msg) {}

    public void UserIpSendAudioFrame(String targetIp ,byte[] data) {}

    public void shutDownSocket(){}

    public boolean disConnect(){}

    public String getTargetIP() {}

    public void setTargetIP(String targetIP) {}

    public void startRecord(){}

    public void  stopRecord(){}

    public boolean isRecording(){}

    public void startPlay(){}

    public void stopPlay(){}

    public boolean isPlaying(){}

    public void startRecordAndPlay(){}

    public void stopRecordAndPlay(){}
}
```

### 1. Listening Port

Each client needs to monitor the call request and obtain the requester's IP. The following code is to initialize a netty instance.

```java
Bootstrap b = new Bootstrap();
group = new NioEventLoopGroup();
try {
    b.group(group)
        .channel(NioDatagramChannel.class)
        .option(ChannelOption.SO_BROADCAST, true)
        .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
        .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65535))
        .handler(handler);
    b.bind(localPort).sync().channel().closeFuture().await();
} catch (Exception e) {
    e.printStackTrace();
} finally {
    group.shutdownGracefully();
}
```
### 2. Data Transmission

It mainly includes two kinds of data:

- Control Number (Integer): Connection control command, and converted to String when sending.(For compatibility with Handler, only numbers can be sent)
- Audio data ( byte[ ] ): Audio binary stream

Before data transmission, it is necessary to determine the type of data(`String or byte[]`) to be sent, and then encapsulate it into different carriers. Finally, use `ChannelhanlerContext` send to  data to the target host (ip + port).

```java
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

Similarly, the receiver needs to make the same judgment when receiving data.

```java
ByteBuf buf = (ByteBuf) packet.copy().content();
byte[] req = new byte[buf.readableBytes()];
buf.readBytes(req);
String str = new String(req, "UTF-8");
Message message = JSON.parseObject(str,Message.class);

// interface callback
if (message.getMsgtype().equals(Message.MES_TYPE_NOMAL)){
     if (frameCallback !=null){
         frameCallback.onTextMessage(message.getMsgBody());
         frameCallback.onGetRemoteIP(message.getMsgIp());
     }
 }else if (message.getMsgtype().equals(Message.MES_TYPE_AUDIO)){
    if (frameCallback !=null){
        frameCallback.onAudioData(message.getFrame());
    }
 }
```
### 3. Control Number

There are three types of control number:

```java
// control text
public static final Integer PHONE_MAKE_CALL = 100; // make call 
public static final Integer PHONE_ANSWER_CALL = 200; // answer call 
public static final Integer PHONE_CALL_END = 300; //  call end 
```

Take A calling B as an example：

1. When B receives *PHONE_MAKE_CALL*, B needs to judge whether he is busy at this time, if he is not busy, he jumps to the ringing interface, otherwise the packet is lost directly.
2. When A receives the *PHONE_ANSWER_CALL* sent by B, A will directly display the dialogue interface, start recording and set the call-receiving flag to true.
3. When A or B receives *PHONE_CALL_END*, it needs to first determine whether the source of the message is the client that is on the call, so as to prevent the wrong hang-up when a third party makes a call.

```java
public void handleMessage(Message msg) {
    if (msg.what == PHONE_MAKE_CALL) {  // B receive 
        if (!isBusy){ 
            showRingView(); 
            isBusy = true;
        }
    }else if (msg.what == PHONE_ANSWER_CALL){  //  A receive
        showTalkingView();
        provider.startRecordAndPlay();
        isAnswer = true; 
    }else if (msg.what == PHONE_CALL_END){  // Both A and B may receive
        if (newEndIp.equals(provider.getTargetIP())){
            showBeginView();
            isAnswer = false;
            isBusy = false;
            provider.stopRecordAndPlay();
            timer.stop();
        }
    }
}
```
### 4、Mixed audio

The audio mixing algorithm uses a two-dimensional byte array to save two audio streams and then merge them. Need to pass in the name of the saved file:

```java 
public static byte[] averageMix(String file1,String file2) throws IOException {

        byte[][] bMulRoadAudioes =  new byte[][]{
                FileUtils.getContent(file1),    // first file
                FileUtils.getContent(file2)     // second file
        };

        byte[] realMixAudio = bMulRoadAudioes[0]; //save the data after mixing.
        Log.e("ccc", " bMulRoadAudioes length " + bMulRoadAudioes.length); //2

        for (int rw = 0; rw < bMulRoadAudioes.length; ++rw) { 
            if (bMulRoadAudioes[rw].length != realMixAudio.length) {
                Log.e("ccc", "column of the road of audio + " + rw + " is diffrent.");
                if (bMulRoadAudioes[rw].length<realMixAudio.length){
                    realMixAudio = subBytes(realMixAudio,0,bMulRoadAudioes[rw].length); 
                }
                else if (bMulRoadAudioes[rw].length>realMixAudio.length){
                    bMulRoadAudioes[rw] = subBytes(bMulRoadAudioes[rw],0,realMixAudio.length);
                }
            }
        }

        int row = bMulRoadAudioes.length;       
        int column = realMixAudio.length / 2;   
        short[][] sMulRoadAudioes = new short[row][column];
        for (int r = 0; r < row; ++r) {         
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


        for (sr = 0; sr < column; ++sr) { 
            realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }

        FileOutputStream fos = null;

        File saveFile = new File(FileUtils.getFileBasePath()+ "averageMix.pcm" );
        if (saveFile.exists()) {
            saveFile.delete();
        }
        fos = new FileOutputStream(saveFile);
        fos.write(realMixAudio);
        fos.close();
        return realMixAudio; 
    }

    private static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }
```

Input the file name and return the byte stream of the file content.
```java
    //Read the file stream into an array,
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

        while (offset < buffer.length
                && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset != buffer.length) {
            throw new IOException("Could not completely read file "
                    + file.getName());
        }
        fi.close();
        return buffer;
    }
```
Thanks Star~
