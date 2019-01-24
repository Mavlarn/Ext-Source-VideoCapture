package io.vantiq.videoCapture.ffmpeg;

import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

import java.io.IOException;

public class FFMpegUtil {

    public void test1() throws IOException {

//        File libFile = JNE.findLibrary("ffmpeg");
//        System.out.println(libFile.getAbsolutePath());

        String file = this.getClass().getClassLoader().getResource("jne/osx/x64/ffprobe").getFile();
        FFprobe ffprobe = new FFprobe(file);

//        String videoFile = "/Users/mavlarn/mywork/vantiq/docs/aws_kinses.mp4";
        String videoFile = "rtsp://222.128.113.190/media/video1";
        FFmpegProbeResult probeResult = ffprobe.probe(videoFile);

        FFmpegFormat format = probeResult.getFormat();
        System.out.format("%nFile: '%s' ; Format: '%s' ; Duration: %.3fs",
                format.filename,
                format.format_long_name,
                format.duration
        );

        FFmpegStream stream = probeResult.getStreams().get(0);
        System.out.format("%nCodec: '%s' ; Width: %dpx ; Height: %dpx",
                stream.codec_long_name,
                stream.width,
                stream.height
        );

    }
}
