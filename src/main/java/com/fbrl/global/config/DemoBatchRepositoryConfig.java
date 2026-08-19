package com.fbrl.global.config;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DemoBatchRepositoryConfig {

  @Bean
  public JobRepository demoJobRepository(
      @Qualifier("demo") DataSource demoDataSource,
      @Qualifier("demoTransactionManager") PlatformTransactionManager demoTransactionManager) {
    JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
    factory.setDataSource(demoDataSource);
    factory.setTransactionManager(demoTransactionManager);
    try {
      factory.afterPropertiesSet();
      return factory.getObject();
    } catch (Exception e) {
      throw new BatchConfigurationException("데모 JDBC 기반 JobRepository 초기화에 실패했습니다.", e);
    }
  }

  @Bean
  public JobOperator demoJobOperator(
      @Qualifier("demoJobRepository") JobRepository demoJobRepository,
      @Qualifier("demoTransactionManager") PlatformTransactionManager demoTransactionManager,
      ApplicationContext applicationContext) {
    JobOperatorFactoryBean factory = new JobOperatorFactoryBean();
    factory.setApplicationContext(applicationContext);
    factory.setJobRepository(demoJobRepository);
    factory.setTransactionManager(demoTransactionManager);
    try {
      factory.afterPropertiesSet();
      return factory.getObject();
    } catch (Exception e) {
      throw new BatchConfigurationException("데모 JobOperator 초기화에 실패했습니다.", e);
    }
  }
}
