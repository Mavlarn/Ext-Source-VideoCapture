package io.vantiq.videoCapture.ffmpeg;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class GaozhongAPITest {

    private static final Logger LOG = LoggerFactory.getLogger(VideoProgressListener.class);

    private ObjectMapper om = new ObjectMapper();
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    @Test
    public void testPostImg() throws IOException {
        File imgFile = new File("/Users/mavlarn/mywork/vantiq/dev/ext-proj/demo/demo_000021.jpg");
        long timestamp = Instant.now().toEpochMilli();

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                                                             .addFormDataPart("strimage", imgFile.getName(),
                                                                     RequestBody.create(MediaType.parse("image/jpeg"), imgFile))
                                                             .addFormDataPart("frame_id", "test_frame_" + timestamp)
                                                             .addFormDataPart("timestamp", String.valueOf(timestamp))
                                                             .build();

        Request request = new Request.Builder()
                .url("http://222.128.113.190:8000/granddetectorx/detections")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            LOG.error("Send image error:" + response.message(), response.body().string());
        } else {
            Map result = om.readValue(response.body().string(), Map.class);
            LOG.info("Process image result:" + result);
        }

    }
}
