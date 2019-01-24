package io.vantiq.videoCapture.ffmpeg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vantiq.videoCapture.VantiqUtil;
import io.vantiq.videoCapture.handler.PublishHandler;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VideoProgressListener implements ProgressListener {

    private static final Logger LOG = LoggerFactory.getLogger(VideoProgressListener.class);

    private ObjectMapper om = new ObjectMapper();
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    private PublishHandler handler;
    private VantiqUtil vantiqUtil;
    private String sendImgUrl;
    private String sourceName;
    private String jobName;
    private boolean isUpload;

    private long lastFrame = 0;

    private DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


    public VideoProgressListener(PublishHandler handler, VantiqUtil vantiqUtil, String sourceName, String jobName, String sendImgUrl, boolean isUpload) {
        this.handler = handler;
        this.vantiqUtil = vantiqUtil;
        this.sendImgUrl = sendImgUrl;
        this.sourceName = sourceName;
        this.jobName = jobName;
        this.isUpload = isUpload;
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

            String fileName = String.format("%s_%06d.jpg", jobName, progress.frame);
            String targetFileName = jobName + "_" + df.format(time) + ".jpg";
            File imgFile = new File(fileName);
            if (!imgFile.exists()) {
                LOG.error("File:{} not exist.", imgFile.getAbsolutePath());
                return;
            }
            LOG.debug("Processing image:{}, Upload to:{}", imgFile.getAbsolutePath(), targetFileName);
            if (isUpload) {
                String uploadFullName = sourceName + "/" + targetFileName;
                vantiqUtil.uploadImage(imgFile, uploadFullName);
            }
            if (StringUtils.isNoneBlank(sendImgUrl)) {
                LOG.info("NEED to post image to web service:{}", imgFile.getAbsolutePath());

                try {
                    String frameId = targetFileName;
                    long timestamp = now.toEpochMilli();
                    byte[] fileContent = FileUtils.readFileToByteArray(imgFile);
                    String strImage = Base64.getEncoder().encodeToString(fileContent);

                    Map<String, Object> params = new HashMap<>();
                    params.put("frame_id", frameId);
                    params.put("timestamp", timestamp);
                    params.put("strimage", strImage);
                    String paramStr = om.writeValueAsString(params);

                    RequestBody body = RequestBody.create(JSON, paramStr);
                    Request request = new Request.Builder()
                            .url(sendImgUrl)
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        LOG.error("Send image error:" + response.message(), response.body().string());
                    }

                } catch (IOException e) {
                    LOG.error("Read file error:" + e.getMessage(), e);
                }
            }
        }

        if (progress.status == Progress.Status.END && this.handler != null) {
            this.handler.jobFinished(jobName);
            return;
        }
    }
}
