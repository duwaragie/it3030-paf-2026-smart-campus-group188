package com.smartcampus.api.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

// VAPID identity for Web Push. Generates + persists a keypair on first boot
// if none is configured; reusing them across restarts is critical because
// swapping keys invalidates every existing browser subscription.
@Slf4j
@Configuration
@Getter
public class VapidConfig {

    private static final File LOCAL_FILE = new File("./.vapid-keys.properties");

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${app.push.subject:mailto:admin@smartcampus.local}")
    private String subject;

    @Value("${app.push.public-key:}")
    private String publicKeyProp;

    @Value("${app.push.private-key:}")
    private String privateKeyProp;

    private String publicKey;
    private String privateKey;

    @PostConstruct
    void init() throws Exception {
        if (notBlank(publicKeyProp) && notBlank(privateKeyProp)) {
            this.publicKey = publicKeyProp;
            this.privateKey = privateKeyProp;
            log.info("VAPID keys loaded from configuration");
            return;
        }

        if (LOCAL_FILE.exists()) {
            java.util.Properties props = new java.util.Properties();
            try (var in = Files.newBufferedReader(LOCAL_FILE.toPath(), StandardCharsets.UTF_8)) {
                props.load(in);
            }
            String pub = props.getProperty("publicKey");
            String priv = props.getProperty("privateKey");
            if (notBlank(pub) && notBlank(priv)) {
                this.publicKey = pub;
                this.privateKey = priv;
                log.info("VAPID keys loaded from {}", LOCAL_FILE.getAbsolutePath());
                return;
            }
        }

        log.warn("No VAPID keys configured — generating a new keypair. Existing browser subscriptions will be invalid.");
        KeyPair kp = generateKeypair();
        this.publicKey = encodePublicKey((ECPublicKey) kp.getPublic());
        this.privateKey = encodePrivateKey((ECPrivateKey) kp.getPrivate());

        String out = "publicKey=" + this.publicKey + "\n" +
                "privateKey=" + this.privateKey + "\n";
        Files.writeString(LOCAL_FILE.toPath(), out, StandardCharsets.UTF_8);
        log.info("VAPID keys generated and saved to {}", LOCAL_FILE.getAbsolutePath());
    }

    @Bean
    public PushService pushService() throws Exception {
        return new PushService(publicKey, privateKey, subject);
    }

    private static KeyPair generateKeypair() throws Exception {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        gen.initialize(spec);
        return gen.generateKeyPair();
    }

    private static String encodePublicKey(ECPublicKey key) {
        byte[] encoded = Utils.encode(key);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
    }

    private static String encodePrivateKey(ECPrivateKey key) {
        // P-256 scalar: strip leading zero / left-pad to fixed 32 bytes.
        byte[] s = key.getD().toByteArray();
        if (s.length > 32) s = java.util.Arrays.copyOfRange(s, s.length - 32, s.length);
        if (s.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(s, 0, padded, 32 - s.length, s.length);
            s = padded;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
