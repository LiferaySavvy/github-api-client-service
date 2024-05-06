package com.liferaysavvy.webclient.service;

import com.liferaysavvy.webclient.controller.GitHubAccessTokenController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
@Setter
@Getter
public class GitHubTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubTokenService.class);
    public File getGitHubRSAPemFile(String fileName) throws FileNotFoundException {
        String filePath = "classpath:"+fileName;
        File file = ResourceUtils.getFile(filePath);
        return file;
    }
    public PrivateKey getPemPrivateKey(File pemFile) throws Exception {
        String rawKey = new String(Files.readAllBytes(pemFile.toPath()), Charset.defaultCharset());
        String pemFilePKSE1 = rawKey
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\n", "")
                .replaceAll("\\s", "");
        LOG.info("pemFilePKSE1 {}", pemFilePKSE1);
        byte[] pemFilePKSE1Encoded = Base64.getDecoder().decode(pemFilePKSE1.getBytes());
        AlgorithmIdentifier algId = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
        PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(algId, ASN1Sequence.getInstance(pemFilePKSE1Encoded));
        byte[] pkcs8Encoded = privateKeyInfo.getEncoded();

        /*java.security.spec.KeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(pkcs1Encoded);
        java.security.KeyFactory.getInstance("RSA", new BouncyCastleProvider()).generatePrivate(spec);
        final byte[] encoded = privateKey.getEncoded();
        final PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(encoded);
        final ASN1Encodable asn1Encodable = privateKeyInfo.parsePrivateKey();
        final ASN1Primitive asn1Primitive = asn1Encodable.toASN1Primitive();
        final byte[] privateKeyPKCS8Formatted = asn1Primitive.getEncoded(ASN1Encoding.DER);

        String key = new String(Files.readAllBytes(pemFile.toPath()), Charset.defaultCharset());
        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM.getBytes());*/
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
    public String generateJWTToken(String clientId, String pemFileName){
        try {
            File file = this.getGitHubRSAPemFile(pemFileName);
            PrivateKey privateKye = this.getPemPrivateKey(file);
            Date expiration = new Date(System.currentTimeMillis() + 600000);
            String jwtToken = Jwts.builder()
                    .setIssuer(clientId)
                    .setIssuedAt(new Date())
                    .setExpiration(expiration)
                    .signWith(SignatureAlgorithm.RS256, privateKye)
                    .compact();
            LOG.info("privateKye {}", privateKye);
            return jwtToken;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public String getGitHubAccessToken(String installationId, String jwtToken){
        String authTokenHeader = "Bearer "+jwtToken;
        LOG.info("authTokenHeader {} installationId {}", authTokenHeader, installationId);
        WebClient webClient = WebClient.builder().baseUrl("https://api.github.com").build();
        return webClient
                .post()
                .uri("/app/installations/{installation_id}/access_tokens",installationId)
                .header("Authorization", authTokenHeader)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String getGitHubAccessTokenRateLimit(String installationId, String jwtToken){
        String authTokenHeader = "Bearer "+jwtToken;
        LOG.info("authTokenHeader {} installationId {}", authTokenHeader, installationId);
        WebClient webClient = WebClient.builder().baseUrl("https://api.github.com").build();
        return webClient
                .post()
                .uri("/app/installations/{installation_id}/access_tokens",installationId)
                .header("Authorization", authTokenHeader)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
