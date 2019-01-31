package io.vantiq.videoCapture.ffmpeg;

import com.fizzed.jne.ExtractException;
import com.fizzed.jne.JNE;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class FFMpegUtilTest {

    @Test
    public void testJne() throws IOException, ExtractException {
        File exeFile = JNE.findExecutable("ffmpeg");
        System.out.println("File:" + exeFile.getAbsolutePath());
    }

    @Test
    public void test1() throws IOException {
        FFMpegUtil util = new FFMpegUtil();
        util.test1();
    }

    @Test
    public void testRTSP() throws IOException, InterruptedException, ExtractException {
//        String videoFile = "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov";
        String videoFile = "rtsp://222.128.113.190/media/video1";
        String rtmpUrl = "rtmp://localhost:1935/stream/video";
        FFMpegVideoUtil util = new FFMpegVideoUtil();
        VideoProgressListener listener = new VideoProgressListener(null, null, "video", null,false);
        FFmpegJob job = util.processVideoRTSP(videoFile, 5, "video", rtmpUrl, listener);
        Thread thread = new Thread((job));
        thread.start();
        thread.join();
    }

    @Test
    public void testVideoFile() throws IOException, InterruptedException, ExtractException {
        String videoFile = "/Users/mavlarn/mywork/vantiq/docs/aws_kinses.mp4";
        String rtmpUrl = "rtmp://localhost:1935/stream/video";
        FFMpegVideoUtil util = new FFMpegVideoUtil();
        VideoProgressListener listener = new VideoProgressListener(null, null, "video", null,false);
        FFmpegJob job = util.processVideoFile(videoFile, 5, "video", rtmpUrl, listener);
        Thread thread = new Thread((job));
        thread.start();
        thread.join();
    }
}
