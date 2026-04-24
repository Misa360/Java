package com.codeanalyzer.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static Properties props = new Properties();

    static {
        try {
            props.load(new FileInputStream("src/main/resources/config.properties"));
        } catch (IOException e) {
            System.err.println("⚠️ Không tìm thấy config.properties, sử dụng mặc định.");
        }
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}
