package io.vantiq.videoCapture.ffmpeg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vantiq.videoCapture.handler.PublishHandler;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VideoProgressListener implements ProgressListener {

    private static final Logger LOG = LoggerFactory.getLogger(VideoProgressListener.class);

    private ObjectMapper om = new ObjectMapper();
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    private PublishHandler handler;
    private String sendImgUrl;
    private String sourceName;
    private String jobName;
    private boolean isUpload;

    private long lastFrame = 0;

    private DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private File jobDirectory;

    public VideoProgressListener(PublishHandler handler, String sourceName, String jobName, String sendImgUrl, boolean isUpload) {
        this.handler = handler;
        this.sendImgUrl = sendImgUrl;
        this.sourceName = sourceName;
        this.jobName = jobName;
        this.isUpload = isUpload;

        this.jobDirectory = new File(jobName);
        this.jobDirectory.mkdir();
    }

    @Override
    public void progress(Progress progress) {

        // Print out interesting information about the progress
        LOG.trace("status:{} frame:{} time:{} ms fps:{} speed:{} x",
                progress.status,
                progress.frame,
                FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
                progress.fps.doubleValue(),
                progress.speed
        );

        if (progress.frame > lastFrame) {
            this.lastFrame = progress.frame;

            Instant now = Instant.now();
            LocalDateTime time = LocalDateTime.ofInstant(now, ZoneId.systemDefault());

            String fileName = String.format("%s/%s_%06d.jpg", jobName, jobName, progress.frame);
            String targetFileName = jobName + "_" + df.format(time) + ".jpg";
            File imgFile = new File(fileName);
            if (!imgFile.exists()) {
                LOG.error("File:{} not exist.", imgFile.getAbsolutePath());
                return;
            }
            try {
                String frameId = targetFileName;
                long timestamp = now.toEpochMilli();
                String uuid = null;
                LOG.debug("Processing image:{}, Upload to:{}", imgFile.getAbsolutePath(), targetFileName);
                if (isUpload) {
                    String uploadFullName = sourceName + "/" + targetFileName;
                    this.handler.uploadImage(imgFile, uploadFullName);
                }
                if (StringUtils.isNoneBlank(sendImgUrl)) {
                    LOG.info("NEED to post image to web service:{}", imgFile.getAbsolutePath());

                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("strimage", imgFile.getName(),
                                    RequestBody.create(MediaType.parse("image/jpeg"), imgFile))
                            .addFormDataPart("frame_id", "test_frame_" + timestamp)
                            .addFormDataPart("timestamp", String.valueOf(timestamp))
                            .build();
                    Request request = new Request.Builder()
                            .url(sendImgUrl)
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        LOG.error("Send image error:" + response.message(), response.body().string());
                    } else {
                        Map result = om.readValue(response.body().string(), Map.class);
                        if (!result.get("code").toString().equals("200")) {
                            LOG.error("Process image error:" + result);
                        } else {
                            Map data = (Map) result.get("data");
                            uuid = (String) data.get("uuid");
                        }
                    }
                }

                // send notification to vantiq
                Map notification = new HashMap();
                notification.put("frame_id", frameId);
                notification.put("timestamp", timestamp);
                notification.put("uuid", uuid);
                notification.put("camera_name", jobName);
                this.handler.sendNotification(notification);

            } catch (Exception e) {
                LOG.error("Error:" + e.getMessage(), e);
            }

        }

        if (progress.status == Progress.Status.END && this.handler != null) {
            this.handler.jobFinished(jobName);
            return;
        }
    }
}
