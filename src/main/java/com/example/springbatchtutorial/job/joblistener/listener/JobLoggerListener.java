package com.example.springbatchtutorial.job.joblistener.listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Job Listener 로 등록하려면 JobExecutionListener 를 Impl 해야한다.
 */
@Slf4j
public class JobLoggerListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("{} Job is Running", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("{} Job is Done. (Status : {})",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus());

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            // email
            log.info("Job is Failed");
        }
    }
}
