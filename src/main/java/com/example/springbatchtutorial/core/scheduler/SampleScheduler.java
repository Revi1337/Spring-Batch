package com.example.springbatchtutorial.core.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 지금까지는 Job 을 실행할 때, jobParmeter 를 줘서 job 을 실행했지만 스케쥴링을 통하여 Job 을 실행할떄는 JobLauncher 를 주입해주어야 한다.
 */
@Component @RequiredArgsConstructor
public class SampleScheduler {

    private final JobLauncher jobLauncher;
    private final Job helloWorldJob;

    @Scheduled(cron = "0 */1 * * * *")
    public void helloWorldJobRun() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters = new JobParameters(
                Collections.singletonMap("requestTime", new JobParameter<>(System.currentTimeMillis(), Long.class))
        );
        jobLauncher.run(helloWorldJob, jobParameters); // JobParameters 값이 없으면 매초마다 파라미터 값이 변경된다.
    }

}
