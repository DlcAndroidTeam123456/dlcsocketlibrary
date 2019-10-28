package com.dlc.socketLibrary;

/**
 * Created by acer on 2017/6/21.
 */

public interface SocketDataReceiveListener {
    void onSocketDataReceive(String address, int port,byte[] data);
}
