package com.bluetooth.mwoolley.microbitbledemo.bluetooth;

public interface ConnectionStatusListener {

    public void connectionStatusChanged(boolean new_state);
    public void serviceDiscoveryStatusChanged(boolean new_state);

}
