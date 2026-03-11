package com.example.localchat.config;


import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.sql.Connection;

//@Component
public class StartupInfoLogger implements ApplicationRunner {

    private final WebServerApplicationContext webServerAppContext;
    private final Environment environment;
    private final DataSource dataSource;

    public StartupInfoLogger(
            WebServerApplicationContext webServerAppContext,
            Environment environment,
            DataSource dataSource
    ) {
        this.webServerAppContext = webServerAppContext;
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        int port = webServerAppContext.getWebServer().getPort();

        String contextPath = environment.getProperty("server.servlet.context-path", "");

        String swaggerUrl = "http://localhost:" + port + contextPath + "/swagger-ui/index.html";
        String serverUrl = "http://localhost:" + port + contextPath;

        String dbName = getDatabaseName();

        System.out.println("\n========================================");
        System.out.println("🚀 DIGITIZEHOME APPLICATION STARTED");
        System.out.println("========================================");
        System.out.println("Server Status   : RUNNING");
        System.out.println("Server URL      : " + serverUrl);
        System.out.println("Swagger URL     : " + swaggerUrl);
        System.out.println("Server Port     : " + port);
        System.out.println("Database Name   : " + dbName);
        System.out.println("Host Address    : " + hostAddress);
        System.out.println("========================================\n");
    }

    private String getDatabaseName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getCatalog();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}