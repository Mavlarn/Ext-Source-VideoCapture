package io.vantiq.videoCapture.handler;

import io.vantiq.extjsdk.ExtensionServiceMessage;
import io.vantiq.extjsdk.ExtensionWebSocketClient;
import io.vantiq.extjsdk.Handler;
import io.vantiq.videoCapture.VideoCaptureExtSrcSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ReconnectHandler extends Handler<ExtensionServiceMessage> {

    static final Logger LOG = LoggerFactory.getLogger(PublishHandler.class);

    private VideoCaptureExtSrcSource extension;

    public ReconnectHandler(VideoCaptureExtSrcSource extension) {
        this.extension = extension;
    }

    @Override
    public void handleMessage(ExtensionServiceMessage message) {

        ExtensionWebSocketClient client = extension.getVantiqClient();
        CompletableFuture<Boolean> success = client.connectToSource();

        try {
            if ( !success.get(10, TimeUnit.SECONDS) ) {
                if (!client.isOpen()) {
                    LOG.error("Failed to connect to server url.");
                } else if (!client.isAuthed()) {
                    LOG.error("Failed to authenticate within 10 seconds using the given authentication data.");
                } else {
                    LOG.error("Failed to connect within 10 seconds");
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Could not reconnect to source within 10 seconds: ", e);
        }
    }
}
