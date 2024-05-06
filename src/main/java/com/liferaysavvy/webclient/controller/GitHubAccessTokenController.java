package com.liferaysavvy.webclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liferaysavvy.webclient.model.AccessTokenRequest;
import com.liferaysavvy.webclient.model.AccessTokenResponse;
import com.liferaysavvy.webclient.model.GitHubInstallation;
import com.liferaysavvy.webclient.service.GitHubTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/github")
public class GitHubAccessTokenController {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubAccessTokenController.class);

    @Autowired
    GitHubTokenService gitHubTokenService;
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        LOG.info("Request received for health");
        return new ResponseEntity<>("alive", HttpStatus.OK);
    }

    @PostMapping("/get/tokens")
    public ResponseEntity<AccessTokenResponse> getAccessTokens(@RequestBody AccessTokenRequest request) {
        String accessToken = "";
        try {

            List<GitHubInstallation> list = this.getGitHubAppInstallations();
            LOG.info("GitHubInstallation List {}", new Gson().toJson(list));
            List<GitHubInstallation> filteredList = list.stream()
                    .filter(installation -> this.getInstallationIds(request.getGithubOrg(), request.getTeam()).contains(installation.getInstallationId()))
                    .collect(Collectors.toList());
            LOG.info("GitHubInstallation filteredList {}", new Gson().toJson(filteredList));
            String pemFileName = "test.pem";
            String clientId = "clientId";
            String installationId = "installationId";
            String jwtToken = gitHubTokenService.generateJWTToken(clientId, pemFileName);
            accessToken = gitHubTokenService.getGitHubAccessToken(installationId,jwtToken);
            LOG.info("jwtToken {}", jwtToken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        AccessTokenResponse response = new AccessTokenResponse();
        response.setMessage("SUCCESS");
        response.setGitHubNativeResponse(accessToken);
        response.setHttpsStatus(HttpStatus.OK.value());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public List<GitHubInstallation> getGitHubAppInstallations() throws Exception {
        String filePath = "classpath:github-installations.json";
        Type listType = new TypeToken<List<GitHubInstallation>>() {}.getType();
        File githubInstallationsFile = ResourceUtils.getFile(filePath);
        String githubInstallationsJson = new String(Files.readAllBytes(githubInstallationsFile.toPath()), Charset.defaultCharset());
        Gson gson = new Gson();
        List<GitHubInstallation> gitHubInstallationList = gson.fromJson(githubInstallationsJson, listType);
        return gitHubInstallationList;
    }
    private List<String> installationIdFilter() {
        return Arrays.asList("1");
    }

    private List<String> getInstallationIds(String gitHubOrg, String team) {
        List<String> installationIds = new ArrayList<>();
        try{
            String filePath = "classpath:github-installations-groups.json";
            File githubInstallationsFile = ResourceUtils.getFile(filePath);
            String githubInstallationsJson = new String(Files.readAllBytes(githubInstallationsFile.toPath()), Charset.defaultCharset());
            Gson gson = new Gson();
            Map<String, Object> installationGroups = gson.fromJson(githubInstallationsJson, Map.class);
            Map<String, Object> orgGroups = (Map<String, Object>)installationGroups.get(gitHubOrg);
            Map<String, List> temGroups = (Map<String, List>)orgGroups.get(team);
            installationIds = temGroups.get("installation-ids");
            LOG.info("getInstallationIds installationIds {}", installationIds.toArray());
        } catch (Exception e){
            LOG.error("getInstallationIds error", e);
        }

        return installationIds;
    }
}
