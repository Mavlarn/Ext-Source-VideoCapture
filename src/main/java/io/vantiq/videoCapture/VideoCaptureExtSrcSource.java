package io.vantiq.videoCapture;

import com.fizzed.jne.ExtractException;
import io.vantiq.extjsdk.ExtensionWebSocketClient;
import io.vantiq.videoCapture.handler.ConfigHandler;
import io.vantiq.videoCapture.handler.PublishHandler;
import io.vantiq.videoCapture.handler.QueryHandler;
import io.vantiq.videoCapture.handler.ReconnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public class VideoCaptureExtSrcSource {

    static final Logger LOG = LoggerFactory.getLogger(VideoCaptureExtSrcSource.class);
    ExtensionWebSocketClient vantiqClient = null;
    String sourceName = null;

    private VantiqUtil vantiqUtil = new VantiqUtil();

    private static Map<String, Map> configurations = new ConcurrentHashMap<String, Map>();

    public boolean connectToVantiq(String sourceName, Map<String, String> connectionInfo) throws IOException, ExtractException {
        if (connectionInfo == null) {
            LOG.error("No VANTIQ connection information provided.");
            return false;
        }

        if (sourceName == null) {
            LOG.error("No source name provided.");
            return false;
        }

        String url = connectionInfo.get(VideoCaptureExtConstants.VANTIQ_URL);
        String token = connectionInfo.get(VideoCaptureExtConstants.VANTIQ_TOKEN);

        this.vantiqUtil.init(url, token);

        // If we get here, then we have sufficient information.  Let's see if we can get it to work...
        ExtensionWebSocketClient localClient = new ExtensionWebSocketClient(sourceName);

        // Set the handlers for the client
        localClient.setPublishHandler(new PublishHandler(this));
        localClient.setQueryHandler(new QueryHandler(this));
        localClient.setConfigHandler(new ConfigHandler(this));
        localClient.setReconnectHandler(new ReconnectHandler(this));
        localClient.setAutoReconnect(true);

        CompletableFuture<Boolean> connector = localClient.initiateFullConnection(url, token);
        CompletableFuture<Boolean> connectionManager = CompletableFuture.completedFuture(true);

        // Add the result of the source connection to the chain
        connectionManager = connectionManager.thenCombine(connector, (prevSucceeded, succeeded) -> prevSucceeded && succeeded);

        boolean sourcesSucceeded = false;
        try {
            sourcesSucceeded = connectionManager.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOG.error("Timeout: not all VANTIQ sources succeeded within 10 seconds.");
        } catch (Exception e) {
            LOG.error("Exception occurred while waiting for VANTIQ source connection", e);
        }

        if (sourcesSucceeded) {
            // If we're here, we've succeeded.  Save the client and return
            LOG.info("Connection succeeded:  We're up for source: {}", sourceName);
            vantiqClient = localClient;
            this.sourceName = sourceName;
        } else {
            LOG.error("Failure to make connection to source {} at {}", sourceName, url);
        }


        if (sourcesSucceeded && localClient.isOpen()) {
            try {
                CountDownLatch latch = new CountDownLatch(1);

                Runtime.getRuntime().addShutdownHook(new Thread() {

                    /** This handler will be called on Control-C pressed */
                    @Override
                    public void run() {
                        // Decrement counter.
                        // It will became 0 and main thread who waits for this barrier could continue run (and fulfill all proper shutdown steps)
                        latch.countDown();
                    }
                });

                latch.await();
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return sourcesSucceeded;
    }

    public ExtensionWebSocketClient getVantiqClient() {
        return vantiqClient;
    }

    public VantiqUtil getVantiqUtil() {
        return this.vantiqUtil;
    }
}
