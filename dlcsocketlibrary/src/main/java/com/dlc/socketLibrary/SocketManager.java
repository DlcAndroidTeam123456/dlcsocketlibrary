package com.dlc.socketLibrary;

import android.util.Log;


/**
 * Created by kirawu on 2017/10/10.
 */

public class SocketManager {
    private static SocketManager socketManager;

    public static SocketManager getSocketManager() {
        if (socketManager == null) {
            socketManager = new SocketManager();
        }
        return socketManager;
    }

    public DLCSocket newDlcSocket() {
        return new DLCSocket();
    }

    private void spm(String msg) {
        Log.d("spm", msg);
    }
}
