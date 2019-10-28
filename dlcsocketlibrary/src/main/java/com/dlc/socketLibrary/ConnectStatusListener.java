package com.dlc.socketLibrary;

/**
 * Created by Administrator on 2018\6\16 0016.
 */

public interface ConnectStatusListener {
    /**
     * @param address
     * @param port
     * @param status                0未连接，1已连接
     * @param isActiveDisconnection 是否主动断开
     */
    void onConnectStatus(String address, int port, int status, boolean isActiveDisconnection);
}
