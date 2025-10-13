package server.config;

public class DBConfig {
    public static final String DB_HOST = "127.0.0.1";
    public static final int DB_PORT = 3306;
    public static final String DB_NAME = "rps_game";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "123456";

    public static String getJdbcUrl() {
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
}