package com.app.replant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MariaDB 연결 테스트
 * 환경변수를 통해 DB 접속 정보를 가져옵니다.
 *
 * 실행 전 환경변수 설정 필요:
 * - DB1_URL: JDBC URL
 * - DB1_USERNAME: DB 사용자명
 * - DB1_PASSWORD: DB 비밀번호
 */
public class MariaDBConnectionTest {
    public static void main(String[] args) {
        // 환경변수에서 DB 접속 정보 읽기 (보안 강화)
        String url = System.getenv("DB1_URL");
        String username = System.getenv("DB1_USERNAME");
        String password = System.getenv("DB1_PASSWORD");

        // 환경변수 미설정 시 에러 메시지
        if (url == null || username == null || password == null) {
            System.err.println("ERROR: Database environment variables not set!");
            System.err.println("Please set DB1_URL, DB1_USERNAME, and DB1_PASSWORD");
            System.exit(1);
        }

        System.out.println("=== MariaDB Connection Test ===");
        String passwordParam = "password";
        String maskedUrl = url.replaceAll(passwordParam + "=[^&]*", passwordParam + "=****");
        System.out.println("URL: " + maskedUrl);
        System.out.println("Username: " + username);
        System.out.println("Attempting to connect...");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver loaded successfully");

            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("CONNECTION SUCCESSFUL!");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println("Connection valid: " + connection.isValid(5));
            connection.close();
            System.out.println("Connection closed");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("CONNECTION FAILED!");
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
