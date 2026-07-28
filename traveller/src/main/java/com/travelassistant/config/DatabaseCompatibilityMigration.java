package com.travelassistant.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Configuration
public class DatabaseCompatibilityMigration {
    @Bean @Order(Ordered.HIGHEST_PRECEDENCE)
    CommandLineRunner migrateExtensibleWorkflowColumns(DataSource dataSource,JdbcTemplate jdbc){
        return args->{
            String product;
            try(var connection=dataSource.getConnection()){product=connection.getMetaData().getDatabaseProductName();}
            if(product.toLowerCase().contains("mysql")) {
                jdbc.execute("ALTER TABLE travel_transactions MODIFY COLUMN recovery_status VARCHAR(40) NULL");
            }
            // H2 uses VARCHAR columns by default via JPA; no migration needed
        };
    }
}
