package com.liferaysavvy.webclient.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Configuration
@Setter
@Getter
public class AccessTokenRequest {
    public String getGithubOrg() {
        return githubOrg;
    }

    public void setGithubOrg(String githubOrg) {
        this.githubOrg = githubOrg;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String githubOrg;
    public String team;
}
