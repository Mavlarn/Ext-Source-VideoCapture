package io.vantiq.videoCapture;

import io.vantiq.extjsdk.ExtensionWebSocketClient;

import java.util.Map;

public class VideoCaptureExtClient {

    ExtensionWebSocketClient vantiqClient = null;
    Map configurationDoc = null;

    public void connect(ExtensionWebSocketClient client, Map config)
    {
        this.vantiqClient = client;
        this.configurationDoc = config;
    }

    public void performQuery(Map parameters)
    {

    }

}
