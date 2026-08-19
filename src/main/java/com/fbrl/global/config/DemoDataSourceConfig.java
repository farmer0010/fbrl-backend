package com.fbrl.global.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.ApplicationDataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.fbrl.adapter.out.persistence.demo",
    entityManagerFactoryRef = "demoEntityManagerFactory",
    transactionManagerRef = "demoTransactionManager")
public class DemoDataSourceConfig {

  @Bean
  @ConfigurationProperties("demo.datasource")
  public DataSourceProperties demoDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Qualifier("demo")
  public DataSource demoDataSource(
      @Qualifier("demoDataSourceProperties") DataSourceProperties demoDataSourceProperties) {
    return demoDataSourceProperties.initializeDataSourceBuilder().build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean demoEntityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("demo") DataSource demoDataSource) {
    return builder
        .dataSource(demoDataSource)
        .packages("com.fbrl.adapter.out.persistence.demo")
        .persistenceUnit("demo")
        .build();
  }

  @Bean
  public PlatformTransactionManager demoTransactionManager(
      @Qualifier("demoEntityManagerFactory") EntityManagerFactory demoEntityManagerFactory) {
    return new JpaTransactionManager(demoEntityManagerFactory);
  }

  @Bean
  public ApplicationDataSourceScriptDatabaseInitializer demoBatchSchemaInitializer(
      @Qualifier("demo") DataSource demoDataSource, SqlInitializationProperties properties) {
    return new ApplicationDataSourceScriptDatabaseInitializer(demoDataSource, properties);
  }
}
