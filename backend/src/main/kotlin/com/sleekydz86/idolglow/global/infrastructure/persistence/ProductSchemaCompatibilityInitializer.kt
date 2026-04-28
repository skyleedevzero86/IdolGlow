package com.sleekydz86.idolglow.global.infrastructure.persistence

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.DatabaseMetaData
import javax.sql.DataSource

@Component
class ProductSchemaCompatibilityInitializer(
    private val dataSource: DataSource,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(ProductSchemaCompatibilityInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        dataSource.connection.use { connection ->
            ensureProductColumn(
                connection = connection,
                columnName = "base_price",
                postgresAlterSql = "ALTER TABLE products ADD COLUMN base_price NUMERIC(19, 2) NOT NULL DEFAULT 0",
                mysqlAlterSql = "ALTER TABLE products ADD COLUMN base_price DECIMAL(19, 2) NOT NULL DEFAULT 0.00",
            )
            ensureProductColumn(
                connection = connection,
                columnName = "tour_attraction_picks_json",
                postgresAlterSql = "ALTER TABLE products ADD COLUMN tour_attraction_picks_json TEXT",
                mysqlAlterSql = "ALTER TABLE products ADD COLUMN tour_attraction_picks_json TEXT NULL",
            )
        }
    }

    private fun ensureProductColumn(
        connection: Connection,
        columnName: String,
        postgresAlterSql: String,
        mysqlAlterSql: String,
    ) {
        if (hasColumn(connection, tableName = "products", columnName = columnName)) {
            return
        }

        val databaseProductName = connection.metaData.databaseProductName.lowercase()
        val alterSql = when {
            databaseProductName.contains("postgresql") -> postgresAlterSql
            databaseProductName.contains("mysql") || databaseProductName.contains("mariadb") -> mysqlAlterSql
            else -> {
                log.warn(
                    "지원하지 않는 DB({})라 products.{} 컬럼 자동 보정을 건너뜁니다.",
                    connection.metaData.databaseProductName,
                    columnName,
                )
                return
            }
        }

        connection.createStatement().use { statement ->
            statement.execute(alterSql)
        }

        if (!connection.autoCommit) {
            connection.commit()
        }

        log.warn("로컬 스키마 호환성을 위해 products.{} 컬럼을 자동 추가했습니다.", columnName)
    }

    private fun hasColumn(
        connection: Connection,
        tableName: String,
        columnName: String,
    ): Boolean {
        val metaData = connection.metaData
        val catalog = connection.catalog
        val schemas = listOf(null, connection.schema, "public").distinct()
        val tableCandidates = listOf(tableName, tableName.lowercase(), tableName.uppercase()).distinct()
        val columnCandidates = listOf(columnName, columnName.lowercase(), columnName.uppercase()).distinct()

        return schemas.any { schema ->
            tableCandidates.any { candidateTable ->
                columnCandidates.any { candidateColumn ->
                    metaData.hasColumn(
                        catalog = catalog,
                        schema = schema,
                        tableName = candidateTable,
                        columnName = candidateColumn,
                    )
                }
            }
        }
    }

    private fun DatabaseMetaData.hasColumn(
        catalog: String?,
        schema: String?,
        tableName: String,
        columnName: String,
    ): Boolean =
        getColumns(catalog, schema, tableName, columnName).use { resultSet ->
            resultSet.next()
        }
}
