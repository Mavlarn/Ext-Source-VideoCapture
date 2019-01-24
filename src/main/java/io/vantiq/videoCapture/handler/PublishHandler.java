package io.vantiq.videoCapture.handler;

import com.fizzed.jne.ExtractException;
import io.vantiq.extjsdk.ExtensionServiceMessage;
import io.vantiq.extjsdk.ExtensionWebSocketClient;
import io.vantiq.extjsdk.Handler;
import io.vantiq.videoCapture.VideoCaptureExtSrcSource;
import io.vantiq.videoCapture.ffmpeg.FFMpegVideoUtil;
import io.vantiq.videoCapture.ffmpeg.VideoProgressListener;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.vantiq.videoCapture.VideoCaptureExtConstants.*;


public class PublishHandler extends Handler<ExtensionServiceMessage> {

    static final Logger LOG = LoggerFactory.getLogger(PublishHandler.class);

    private VideoCaptureExtSrcSource extension;
    private FFMpegVideoUtil ffmpegUtil = new FFMpegVideoUtil();
    private Map<String, Thread> jobs = new HashMap<>();

    public PublishHandler(VideoCaptureExtSrcSource extension) throws IOException, ExtractException {
        this.extension = extension;
    }

    @Override
    public void handleMessage(ExtensionServiceMessage message) {
        LOG.info("Publish called with message " + message.toString());

        String replyAddress = ExtensionServiceMessage.extractReplyAddress(message);
        ExtensionWebSocketClient client = extension.getVantiqClient();

        if ( !(message.getObject() instanceof Map) ) {
            client.sendQueryError(replyAddress, "io.vantiq.videoCapture.handler.PublishHandler",
                    "Request must be a map", null);
        }

        Map<String, String> request = (Map<String, String>) message.getObject();
        startJob(request, replyAddress);

    }

    /**
     * Msg to start a video job:
       {
           "targetUrl": "rtsp://222.128.113.190/media/video1",
           "jobName": "video",
           "targetFile": "/Users/mavlarn/mywork/vantiq/docs/aws_kinses.mp4",
           "interval": "2",
           "isUpload": "false",
           "sendImageUrl": ""
       }
     * @param request
     * @param replyAddress
     */
    private void startJob(Map<String, String> request, String replyAddress) {

        String videoUrl = request.get(EXT_TARGET_VIDEO_URL);
        String videoFile = request.get(EXT_TARGET_VIDEO_FILE);
        String jobName = request.get(EXT_JOB_NAME);
        String rtmpUrl = request.get(EXT_SEND_RTMP_URL);
        Integer interval = 1;
        Boolean isUpload = null;
        String sendImgUrl = null;

        if (request.containsKey(EXT_CAPTURE_INTERVAL)) {
            interval = Integer.valueOf(request.get(EXT_CAPTURE_INTERVAL));
        }
        if (request.containsKey(EXT_IS_UPLOAD_TO_DOCUMENT)) {
            isUpload = Boolean.valueOf(request.get(EXT_IS_UPLOAD_TO_DOCUMENT));
        }
        if (sendImgUrl == null) {
            sendImgUrl = request.get(EXT_SEND_IMAGE_URL);
        }
        ExtensionWebSocketClient client = extension.getVantiqClient();
        if (this.jobs.containsKey(jobName)) {
            client.sendQueryError(replyAddress, "io.vantiq.videoCapture.handler.PublishHandler",
                    "Job is already running.", null);
            return;
        }

        try {
            FFmpegJob job;
            VideoProgressListener listener = new VideoProgressListener(this, extension.getVantiqUtil(), client.getSourceName(), jobName, sendImgUrl, isUpload);
            if (videoFile != null) {
                job = ffmpegUtil.processVideoFile(videoFile, interval, jobName, rtmpUrl, listener);
            } else if (videoUrl != null) {
                job = ffmpegUtil.processVideoRTSP(videoUrl, interval, jobName, rtmpUrl, listener);
            } else {
                client.sendQueryError(replyAddress, "io.vantiq.videoCapture.handler.PublishHandler",
                        "Request must contain videoFile or videoUrl", null);
                return;
            }
            Thread thread = new Thread(job);
            this.jobs.put(jobName, thread);
            thread.start();
            LOG.info("Job:{} started.", jobName);

        } catch (Exception e) {
            LOG.error("ffmpeg error:" + e.getMessage(), e);
            if (this.jobs.containsKey(jobName)) {
                this.jobs.remove(jobName);
            }
            client.sendQueryError(replyAddress, "io.vantiq.videoCapture.handler.PublishHandler",
                    "Process err:" + e.toString(), null);
        }
    }

    public void jobFinished(String jobName) {
        this.jobs.remove(jobName);
        LOG.info("Job:{} finished.", jobName);
    }

    // FFMpeg的Job无法停止，所以目前没有停止的功能
    private void stopJob(Map<String, String> request, String replyAddress) {
        String jobName = request.get(EXT_JOB_NAME);
        LOG.info("To stop the Job:{}.", jobName);
        ExtensionWebSocketClient client = extension.getVantiqClient();
        if (!this.jobs.containsKey(jobName)) {
            client.sendQueryError(replyAddress, "io.vantiq.videoCapture.handler.PublishHandler",
                    "Job is NOT running.", null);
            return;
        }

        try {
            Thread job = this.jobs.get(jobName);
            job.interrupt();
        } catch (Exception e) {
            client.sendQueryError(replyAddress, "io.vantiq.videoCapture.handler.PublishHandler",
                    "Process err:" + e.toString(), null);
        }
    }
}
