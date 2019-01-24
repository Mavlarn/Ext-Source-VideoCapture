package io.vantiq.videoCapture.handler;

import io.vantiq.extjsdk.ExtensionServiceMessage;
import io.vantiq.extjsdk.Handler;
import io.vantiq.videoCapture.VideoCaptureExtSrcSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigHandler extends Handler<ExtensionServiceMessage> {

    static final Logger LOG = LoggerFactory.getLogger(VideoCaptureExtSrcSource.class);

    private VideoCaptureExtSrcSource extension;

    public ConfigHandler(VideoCaptureExtSrcSource extension) {
        this.extension = extension;
    }

    @Override
    public void handleMessage(ExtensionServiceMessage message) {
        LOG.warn("No configuration need for source:{}", message.getSourceName());
    }
}
