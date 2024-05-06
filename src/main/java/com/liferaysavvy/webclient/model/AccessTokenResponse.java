package com.liferaysavvy.webclient.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Configuration
@Setter
@Getter
public class AccessTokenResponse {
    public int getHttpsStatus() {
        return httpsStatus;
    }

    public void setHttpsStatus(int httpsStatus) {
        this.httpsStatus = httpsStatus;
    }

    public int httpsStatus;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getGitHubNativeResponse() {
        return gitHubNativeResponse;
    }

    public void setGitHubNativeResponse(Object gitHubNativeResponse) {
        this.gitHubNativeResponse = gitHubNativeResponse;
    }

    public String message;
    public Object gitHubNativeResponse;
}
