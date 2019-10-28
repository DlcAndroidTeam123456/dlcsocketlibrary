package com.dlc.socketLibrary;

import android.os.SystemClock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2018\6\16 0016.
 */

public class DLCSocket {
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private boolean isConnect;
    private Timer timer, checkConnectTimer;
    private TimerTask task, checkConnectTask;
    private String address;
    private int port;

    /**
     * 发送数据后，接收数据超时时间，毫秒
     */
    private long receiveTimeOut = 1000;

    /**
     * 两次接收数据之间，判断已不再会有数据的超时时间，毫秒
     */
    private long receiveIntervalTimeOut = 10;


    /**
     * 0一直监听数据接收，1只有发送的时候才监听数据回复，且有超时时间
     */
    private int callbackMode;

    /**
     * 是否主动断开
     */
    private boolean isActiveDisconnection;

    public void connect(final String address, final int port) {
        this.address = address;
        this.port = port;
        isActiveDisconnection = false;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    socket = new Socket(address, port);
                    is = socket.getInputStream();
                    os = socket.getOutputStream();
                    isConnect = true;
                    if (connectStatusListener != null) {
                        connectStatusListener.onConnectStatus(address, port, 1, isActiveDisconnection);
                    }
                    int buffSize = 1024;
                    byte[] buffer = new byte[buffSize];
                    int count = 0;
                    while (isConnect && callbackMode == 0) {
                        if ((count = is.read(buffer)) > 0) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream(
                                    buffSize);
                            out.write(buffer, 0, count);
                            out.flush();
                            byte[] content = out.toByteArray();
//                            spm("r:" + content);
                            if (socketDataReceiveListener != null) {
                                socketDataReceiveListener.onSocketDataReceive(address, port, content);
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    spm("e:" + e.toString());
                }
                cancelHeartTimer();
                isConnect = false;
                if (connectStatusListener != null) {
                    connectStatusListener.onConnectStatus(address, port, 0, isActiveDisconnection);
                }
                spm("tread end");
                super.run();
            }
        };
        thread.start();
    }

    public Socket getSocket() {
        return socket;
    }


    /**
     * @param callbackMode 0一直监听数据接收，1只有发送的时候才监听数据回复，且有超时时间
     */
    public void setCallbackMode(int callbackMode) {
        this.callbackMode = callbackMode;
    }

    /**
     * 设置超时时间
     *
     * @param receiveTimeOut
     */
    public void setReceiveTimeOut(long receiveTimeOut) {
        this.receiveTimeOut = receiveTimeOut;
    }

    private ConnectStatusListener connectStatusListener;

    public void setConnectStatusListener(ConnectStatusListener listener) {
        this.connectStatusListener = listener;
    }

    private HeartListener heartListener;

    public void setHeartListener(HeartListener heartListener) {
        this.heartListener = heartListener;
    }

    private SocketDataReceiveListener socketDataReceiveListener;

    public void setSocketDataReceiveListener(SocketDataReceiveListener socketDataReceiveListener) {
        this.socketDataReceiveListener = socketDataReceiveListener;
    }

    private boolean isWriting;

    public void send(final byte[] bytes) {
        if (isWriting) {
            return;
        }
        isWriting = true;
        try {
            os.write(bytes);
            os.flush();
            spm("write:" + new String(bytes));
        } catch (IOException e) {
            e.printStackTrace();
            spm("send IOException:" + e.toString());
            //写的io异常默认就是断开
            isConnect = false;
        }
        isWriting = false;
    }

    public void send(String data) {
        send(data.getBytes());
    }

    public void send(final byte[] bytes, SendDataCallback callback) {
        if (isWriting) {
            return;
        }
        isWriting = true;
        try {
            os.write(bytes);
            os.flush();
            long countTime = 0;
            long intervalCountTime = 0;
            byte[] allContent = new byte[0];
            while (callbackMode == 1) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                int buffSize = 2;
                byte[] buffer = new byte[buffSize];
                int count = 0;
                if (is.available() > 0) {
                    count = is.read(buffer);
                    ByteArrayOutputStream out = new ByteArrayOutputStream(
                            buffSize);
                    out.write(buffer, 0, count);
                    out.flush();
                    byte[] content = out.toByteArray();
                    allContent = mergeBytes(allContent, content);
                    intervalCountTime = 0;
                } else {
                    // 暂停一点时间，免得一直循环造成CPU占用率过高
                    SystemClock.sleep(1);
                    countTime++;
                }
                if (allContent.length > 0) {
                    intervalCountTime++;
                }
                if (intervalCountTime > receiveIntervalTimeOut) {
                    callback.onResponse(address, port, allContent);
                    break;
                }
                if (countTime > receiveTimeOut) {
                    callback.onTimeOut(address, port);
                    break;
                }
            }
            isWriting = false;
        } catch (IOException e) {
            e.printStackTrace();
            spm("e:" + e);
            //写的io异常默认就是断开
            isWriting = false;
            if (callbackMode == 1) {
                callback.onError(address, port);
            }
        }

    }

    public void send(String data, SendDataCallback callback) {
        send(data.getBytes(), callback);
    }

    private byte[] mergeBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

    public void interruptSocket() throws IOException {
        isConnect = false;
        if (is != null) {
            is.close();
        }
        if (os != null) {
            os.close();
        }
        if (socket != null) {
            socket.close();
        }
    }

    public void closeSocket() throws IOException {
//        spm("closeSocket");
        isConnect = false;
        isActiveDisconnection = true;
        if (is != null) {
            is.close();
        }
        if (os != null) {
            os.close();
        }
        if (socket != null) {
            socket.close();
        }
//        if (connectStatusListener != null) {
//            connectStatusListener.onConnectStatus(address, port, 0);
//        }
    }

    /**
     * 开启心跳，在socket连接成功后开启
     *
     * @param ms 单位ms
     */
    public void startHeartTimer(long ms) {
        cancelHeartTimer();
        if (!isConnect) {
            return;
        }
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                if (heartListener != null && isConnect) {
                    heartListener.onHeart(address, port);
                }
            }
        };
        timer.schedule(task, 0, ms);
    }

    public void cancelHeartTimer() {
        if (timer != null) {
            task.cancel();
            timer.cancel();
            task = null;
            timer = null;
        }
    }

    public byte[][] getFilterDatas(byte[] head, int wordByteLength, byte[] originData) {
        if (originData == null || originData.length < head.length + wordByteLength) {
            return new byte[0][0];
        }
        byte[] lengthBytes = new byte[wordByteLength];
        for (int i = 0; i < wordByteLength; i++) {
            lengthBytes[i] = originData[head.length + i];
        }
        spm("lengthBytes:" + Integer.parseInt(new String(lengthBytes)));
        int dataLength = head.length + wordByteLength + Integer.parseInt(new String(lengthBytes));
        spm("dataLength:" + dataLength);
        byte[][] datas = new byte[3][4];
        return datas;
    }

    private void spm(String msg) {
//        Log.d("spm", msg);
        if (logListener != null) {
            logListener.onLog(msg);
        }
    }

    public interface LogListener {
        void onLog(String msg);
    }

    private LogListener logListener;

    public void setLogListener(LogListener logListener) {
        this.logListener = logListener;
    }
}
