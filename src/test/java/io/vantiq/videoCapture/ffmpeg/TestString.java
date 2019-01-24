package io.vantiq.videoCapture.ffmpeg;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TestString {

    private static final Logger LOG = LoggerFactory.getLogger(TestString.class);

    @Test
    public void test1() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String sourceName = "VideoCapture";
        String jobName = "video";
        long frame = 12;

        String fileName = String.format("%s_%06d.jpg", jobName, frame);
        String targetFileName = sourceName + "/" + jobName + "_" + df.format(LocalDateTime.now());

        LOG.debug("File name:{}", fileName);
        LOG.debug("targetFileName name:{}", targetFileName);
    }

    @Test
    public void testTime() {
        Instant now = Instant.now();

        LocalDateTime time = LocalDateTime.ofInstant(now, ZoneId.systemDefault());

        LOG.debug("Mili second time:{}", now.toEpochMilli());
        LOG.debug("Local date time:{}", time.toString());
    }
}
