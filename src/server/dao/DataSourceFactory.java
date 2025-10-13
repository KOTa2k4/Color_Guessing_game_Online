package server.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import server.config.DBConfig; // Import file config của bạn
import javax.sql.DataSource;

public class DataSourceFactory {
    private static final DataSource dataSource;

    // Khối static này sẽ chạy một lần duy nhất khi server khởi động
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DBConfig.getJdbcUrl());
        config.setUsername(DBConfig.DB_USER);
        config.setPassword(DBConfig.DB_PASS);

        // --- Các cấu hình quan trọng của HikariCP ---
        config.setMaximumPoolSize(10); // Số kết nối tối đa trong bể (10 là con số tốt để bắt đầu)
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Các cấu hình tối ưu hiệu suất (tùy chọn nhưng nên có)
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");

        // Tạo ra "bể kết nối"
        dataSource = new HikariDataSource(config);
    }

    /**
     * Lấy ra DataSource duy nhất của ứng dụng để "mượn" kết nối.
     */
    public static DataSource getDataSource() {
        return dataSource;
    }
}