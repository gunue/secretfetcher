package com.example.app;

import java.io.FileInputStream;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import com.cyberark.conjur.api.Conjur;
import com.cyberark.conjur.api.Token;
import java.util.logging.Logger;
import java.nio.file.Files;

public class SecretFetcher {
    private static SecretFetcher instance = null;
    private static final Logger logger = Logger.getLogger(SecretFetcher.class.getName());
    private static Conjur conjur = null;

    private SecretFetcher() {
        try {
            initializeConjurClient();
        } catch (Exception e) {
            logger.severe("Error in SecretFetcher constructor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static SecretFetcher getConjurInstance() {
        if (instance == null) {
            instance = new SecretFetcher();
        }
        return instance;
    }

    private void initializeConjurClient() throws Exception {
        try {
            // CLIENT LEVEL TRUST
            final String conjurTlsCaPath = "/tmp/conjur-connect/CONJUR_CA";
            logger.info("Loading Conjur TLS certificate from: " + conjurTlsCaPath);

            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            final FileInputStream certIs = new FileInputStream(conjurTlsCaPath);
            final Certificate cert = cf.generateCertificate(certIs);

            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null);
            ks.setCertificateEntry("conjurTlsCaPath", cert);

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext conjurSSLContext = SSLContext.getInstance("TLS");
            conjurSSLContext.init(null, tmf.getTrustManagers(), null);

            // Read token
            Token token = null;
            while (token == null) {
                try {
                    if (Files.exists(Paths.get("/run/conjur/access-token"))) {
                        token = Token.fromFile(Paths.get("/run/conjur/access-token"));
                        logger.info("Loaded Conjur token");
                    } else {
                        logger.info("Waiting for Conjur token to be created...");
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    logger.severe("Interrupted while waiting for token to be created: " + e.getMessage());
                    e.printStackTrace();
                }
            }
                        
            // Using custom SSLContext setup as conjurSSLContext variable
            Conjur conjur = new Conjur(token, conjurSSLContext);
            logger.info("Conjur setup completed");

            //logger.info("Secret fetched successfully: " + this.secret);
        } catch (Exception e) {
            logger.severe("Error in initialization: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public String getSecret(String secretPath) {
        try {
            if (conjur == null) {
                logger.severe("Conjur client is not initialized");
                throw new IllegalStateException("Conjur client is not initialized");
            }

            logger.info("Fetching secret from path: " + secretPath);
            return conjur.variables().retrieveSecret(secretPath);
        } catch (Exception e) {
            logger.severe("Error in getSecret: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}