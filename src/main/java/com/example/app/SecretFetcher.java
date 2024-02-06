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
            // CLIENT LEVEL TRUST setup
            final String conjurTlsCaPath = "/tmp/conjur-connect/CONJUR_CA";
            logger.info("Loading Conjur TLS certificate from: " + conjurTlsCaPath);
    
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (FileInputStream certIs = new FileInputStream(conjurTlsCaPath)) {
                final Certificate cert = cf.generateCertificate(certIs);
    
                final KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(null, null); // Corrected load call
                ks.setCertificateEntry("conjurTlsCaPath", cert);
    
                final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);
    
                SSLContext conjurSSLContext = SSLContext.getInstance("TLS");
                conjurSSLContext.init(null, tmf.getTrustManagers(), null);
    
                // Attempt to read token file multiple times
                final int maxRetries = 5; // Maximum number of retries
                int currentRetry = 0;
                boolean tokenLoaded = false;
    
                while (currentRetry < maxRetries && !tokenLoaded) {
                    if (Files.exists(Paths.get("/run/conjur/access-token"))) {
                        // Production environment
                        Token token = Token.fromFile(Paths.get("/run/conjur/access-token"));
                        logger.info("Loaded Conjur token");
                        conjur = new Conjur(token, conjurSSLContext);
                        tokenLoaded = true;
                    } else {
                        logger.info("Conjur token not found, retrying... Attempt " + (currentRetry + 1));
                        Thread.sleep(1000); // Wait for 1 second before retrying
                        currentRetry++;
                    }
                }
    
                if (!tokenLoaded) {
                    // Fall back to local development environment initialization
                    logger.info("Falling back to local development environment initialization.");
                    if (System.getenv("CONJUR_ACCOUNT") != null && System.getenv("CONJUR_AUTHN_LOGIN") != null && System.getenv("CONJUR_AUTHN_API_KEY") != null) {
                        conjur = new Conjur(conjurSSLContext); // Assuming environment variables are used internally
                    } else {
                        throw new IllegalStateException("Required environment variables for Conjur authentication are not set.");
                    }
                }
    
                logger.info("Conjur setup completed");
            }
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