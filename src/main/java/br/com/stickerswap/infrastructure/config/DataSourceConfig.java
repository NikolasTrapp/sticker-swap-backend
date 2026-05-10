package br.com.stickerswap.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DataSourceConfig {

    private final AppProperties.DatabaseProperties db;

    public DataSourceConfig(AppProperties props) {
        this.db = props.database();
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        log.info("Connecting to database: {}", db.url());
        config.setJdbcUrl(db.url());
        config.setUsername(db.username());
        config.setPassword(db.password());
        config.setDriverClassName(db.driverClassName());
        config.setPoolName(db.poolName());
        config.setMaximumPoolSize(db.maximumPoolSize());
        config.setMinimumIdle(db.minimumIdle());
        config.setConnectionTimeout(db.connectionTimeout().toMillis());
        config.setIdleTimeout(db.idleTimeout().toMillis());
        config.setMaxLifetime(db.maxLifetime().toMillis());
        config.setKeepaliveTime(db.keepAliveTime().toMillis());
        return new HikariDataSource(config);
    }
}
