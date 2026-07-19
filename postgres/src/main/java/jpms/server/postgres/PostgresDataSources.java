package jpms.server.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class PostgresDataSources {

    static HikariDataSource pooled(PostgresConfig postgres) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.jdbcUrl());
        config.setUsername(postgres.user());
        config.setPassword(postgres.password());
        config.setMaximumPoolSize(postgres.poolSize());
        return new HikariDataSource(config);
    }

    private PostgresDataSources() {}
}
