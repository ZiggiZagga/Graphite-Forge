package com.example.configserver;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Config Server.
 */
@Component
@ConfigurationProperties(prefix = "config-server")
public class ConfigServerProperties {
    
    private String encryptionPassword = "default-encryption-password-change-in-production";
    private boolean enableBusRefresh = true;
    private String messageBroker = "rabbitmq";
    
    // Getters and Setters
    public String getEncryptionPassword() {
        return encryptionPassword;
    }
    
    public void setEncryptionPassword(String encryptionPassword) {
        this.encryptionPassword = encryptionPassword;
    }
    
    public boolean isEnableBusRefresh() {
        return enableBusRefresh;
    }
    
    public void setEnableBusRefresh(boolean enableBusRefresh) {
        this.enableBusRefresh = enableBusRefresh;
    }
    
    public String getMessageBroker() {
        return messageBroker;
    }
    
    public void setMessageBroker(String messageBroker) {
        this.messageBroker = messageBroker;
    }
}
