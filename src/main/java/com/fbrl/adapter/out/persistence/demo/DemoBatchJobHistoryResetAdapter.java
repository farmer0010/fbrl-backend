package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.application.port.out.DemoBatchJobHistoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DemoBatchJobHistoryResetAdapter implements DemoBatchJobHistoryPort {

  @PersistenceContext(unitName = "demo")
  private EntityManager entityManager;

  @Override
  public void deleteByJobNames(List<String> jobNames) {
    entityManager
        .createNativeQuery(
            "delete from BATCH_STEP_EXECUTION_CONTEXT where STEP_EXECUTION_ID in ("
                + "select STEP_EXECUTION_ID from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID in ("
                + "select JOB_EXECUTION_ID from BATCH_JOB_EXECUTION where JOB_INSTANCE_ID in ("
                + "select JOB_INSTANCE_ID from BATCH_JOB_INSTANCE where JOB_NAME in (:jobNames))))")
        .setParameter("jobNames", jobNames)
        .executeUpdate();

    entityManager
        .createNativeQuery(
            "delete from BATCH_JOB_EXECUTION_CONTEXT where JOB_EXECUTION_ID in ("
                + "select JOB_EXECUTION_ID from BATCH_JOB_EXECUTION where JOB_INSTANCE_ID in ("
                + "select JOB_INSTANCE_ID from BATCH_JOB_INSTANCE where JOB_NAME in (:jobNames)))")
        .setParameter("jobNames", jobNames)
        .executeUpdate();

    entityManager
        .createNativeQuery(
            "delete from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID in ("
                + "select JOB_EXECUTION_ID from BATCH_JOB_EXECUTION where JOB_INSTANCE_ID in ("
                + "select JOB_INSTANCE_ID from BATCH_JOB_INSTANCE where JOB_NAME in (:jobNames)))")
        .setParameter("jobNames", jobNames)
        .executeUpdate();

    entityManager
        .createNativeQuery(
            "delete from BATCH_JOB_EXECUTION_PARAMS where JOB_EXECUTION_ID in ("
                + "select JOB_EXECUTION_ID from BATCH_JOB_EXECUTION where JOB_INSTANCE_ID in ("
                + "select JOB_INSTANCE_ID from BATCH_JOB_INSTANCE where JOB_NAME in (:jobNames)))")
        .setParameter("jobNames", jobNames)
        .executeUpdate();

    entityManager
        .createNativeQuery(
            "delete from BATCH_JOB_EXECUTION where JOB_INSTANCE_ID in ("
                + "select JOB_INSTANCE_ID from BATCH_JOB_INSTANCE where JOB_NAME in (:jobNames))")
        .setParameter("jobNames", jobNames)
        .executeUpdate();

    entityManager
        .createNativeQuery("delete from BATCH_JOB_INSTANCE where JOB_NAME in (:jobNames)")
        .setParameter("jobNames", jobNames)
        .executeUpdate();
  }
}
