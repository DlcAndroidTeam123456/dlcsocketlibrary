package com.dlc.socketLibrary;

/**
 * Created by Administrator on 2018\9\25 0025.
 */

public interface SendDataCallback {
    void onResponse(String address, int port,byte[] data);
    void onTimeOut(String address, int port);
    void onError(String address, int port);
}
