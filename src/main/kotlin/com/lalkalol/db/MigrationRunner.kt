package com.lalkalol.db

import org.flywaydb.core.Flyway
import javax.sql.DataSource

object MigrationRunner {
    fun migrate(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
