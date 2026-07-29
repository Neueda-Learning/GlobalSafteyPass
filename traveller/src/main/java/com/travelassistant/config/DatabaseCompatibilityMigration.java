package com.travelassistant.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import javax.sql.DataSource;
import java.util.Locale;

@Configuration
public class DatabaseCompatibilityMigration {
    @Bean @Order(Ordered.HIGHEST_PRECEDENCE)
    CommandLineRunner migrateExtensibleWorkflowColumns(DataSource dataSource,JdbcTemplate jdbc){
        return args->{
            String product;
            String schema;
            try(var connection=dataSource.getConnection()){
                product=connection.getMetaData().getDatabaseProductName();
                schema=connection.getCatalog();
            }
            if(product.toLowerCase(Locale.ROOT).contains("mysql")){
                jdbc.execute("ALTER TABLE travel_transactions MODIFY COLUMN recovery_status VARCHAR(40) NULL");
                if(columnExists(jdbc,schema,"trips","currency_check_passed")){
                    jdbc.execute("ALTER TABLE trips MODIFY COLUMN currency_check_passed BIT(1) NOT NULL DEFAULT b'0'");
                }
            }
        };
    }

    private boolean columnExists(JdbcTemplate jdbc,String schema,String table,String column){
        Integer count=jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name=? AND column_name=?",
                Integer.class,schema,table,column);
        return count!=null&&count>0;
    }
}
