package com.fbrl.global.config;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchRepositoryConfig extends DefaultBatchConfiguration {

  private final DataSource dataSource;
  private final PlatformTransactionManager transactionManager;

  public BatchRepositoryConfig(
      DataSource dataSource, PlatformTransactionManager transactionManager) {
    this.dataSource = dataSource;
    this.transactionManager = transactionManager;
  }

  @Override
  @Bean
  public JobRepository jobRepository() {
    JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
    factory.setDataSource(dataSource);
    factory.setTransactionManager(transactionManager);
    try {
      factory.afterPropertiesSet();
      return factory.getObject();
    } catch (Exception e) {
      throw new BatchConfigurationException("JDBC 기반 JobRepository 초기화에 실패했습니다.", e);
    }
  }

  @Override
  protected PlatformTransactionManager getTransactionManager() {
    return transactionManager;
  }
}
