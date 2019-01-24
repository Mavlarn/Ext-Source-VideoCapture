package io.vantiq.videoCapture;

import io.vantiq.client.BaseResponseHandler;
import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqError;
import io.vantiq.videoCapture.handler.PublishHandler;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class VantiqUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PublishHandler.class);
    private Vantiq vantiq;

    public void init(String vantiqUrl, String accessToken) {
        this.vantiq = new Vantiq(vantiqUrl);
        vantiq.setAccessToken(accessToken);
    }

    public void uploadImage(File imgFile, String imageFullName) {
        vantiq.upload(imgFile,"image/jpeg",
                "public/images/" + imageFullName,
                new BaseResponseHandler() {
                    @Override public void onSuccess(Object body, Response response) {
                        super.onSuccess(body, response);
                        LOG.debug("Content Location = " + this.getBodyAsJsonObject().get("content"));
                    }

                    @Override public void onError(List<VantiqError> errors, Response response) {
                        super.onError(errors, response);
                        LOG.error("Errors uploading image with VANTIQ SDK: " + errors);
                    }
                });


    }
}
