package io.vantiq.videoCapture.ffmpeg;

import com.fizzed.jne.ExtractException;
import com.fizzed.jne.JNE;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

public class FFMpegVideoUtil {

    private String mpegFile;
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;

    public FFMpegVideoUtil() throws IOException, ExtractException {
        File mpegFile = JNE.findExecutable("ffmpeg");
//        mpegFile = this.getClass().getClassLoader().getResource("jne/osx/x64/ffmpeg").getFile();
        ffmpeg = new FFmpeg(mpegFile.getAbsolutePath());
    }

    public FFmpegJob processVideoRTSP(String videoUrl, int interval, String jobName, String rtmpUrl, VideoProgressListener progressListener) throws IOException {

        FFmpegBuilder builder = new FFmpegBuilder()
                .readAtNativeFrameRate()
                .setInput(videoUrl)
                .addExtraArgs("-rtsp_transport", "tcp");
        builder.addOutput(jobName + "_%06d.jpg")
               .setVideoFrameRate(1, interval)
               .setFormat("image2");
        // rtmp url: "rtmp://localhost:1935/stream/" + jobName
        if (StringUtils.isNoneBlank(rtmpUrl) && rtmpUrl.startsWith("rtmp://")) {
            FFmpegOutputBuilder videoOutBuilder = builder.addOutput(rtmpUrl)
                                                         .setFormat("flv");
//               .setAudioSampleRate(11025) // 在MAG的视频上会报错: could not find codec parameters
//               .setVideoCodec("copy"); // 在MAG的视频上会报错: could not find codec parameters

//               .setVideoCodec("h264")
//               .addExtraArgs("-hls_time", "30")
//               .addExtraArgs("-vbsf", "h264_mp4toannexb")

//        builder.addExtraArgs("-hls_time", "30");

//        if (!videoUrl.contains("media/video1")) {
            // 在MAG的视频上会报错: could not find codec parameters，所以只有在非MAG的视频上，才加这些参数
            videoOutBuilder.setAudioCodec("copy").setVideoCodec("copy");
        }
//        }
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
        return executor.createJob(builder, progressListener);
    }

    public FFmpegJob processVideoFile(String videoFile, int interval, String jobName, String rtmpUrl, VideoProgressListener progressListener) throws IOException {


        FFmpegBuilder builder = new FFmpegBuilder()
                .readAtNativeFrameRate()
                .setInput(videoFile);
        builder.addOutput(jobName + "_%06d.jpg")
               .setVideoFrameRate(1, interval)
               .setFormat("image2");
        if (StringUtils.isNoneBlank(rtmpUrl) && rtmpUrl.startsWith("rtmp://")) {
            builder.addOutput(rtmpUrl)
                   .setFormat("flv")
                   .setVideoCodec("copy")
                   .done();
        }
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
        return executor.createJob(builder, progressListener);
    }
}
