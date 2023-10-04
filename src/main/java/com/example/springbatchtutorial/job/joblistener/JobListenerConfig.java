package com.example.springbatchtutorial.job.joblistener;

import com.example.springbatchtutorial.job.joblistener.listener.JobLoggerListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.CompositeJobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * --job.name=jobListenerJob
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JobListenerConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    /**
     * Listener 는 Job 에서 등록해줄 수 있다.
     * 마찬가지로 Multiple 로 등록해줄 수 있다.
     * @return
     */
    @Bean
    public Job jobListenerJob() {
        return new JobBuilder("jobListenerJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobLoggerListener())
                .start(jobListenerStep())
                .build();
    }

    @JobScope
    @Bean
    public Step jobListenerStep() {
        return new StepBuilder("jobListenerStep", jobRepository)
                .tasklet(jobListenerTasklet(), platformTransactionManager)
                .build();
    }

    @StepScope
    @Bean
    public Tasklet jobListenerTasklet() {
        return (contribution, chunkContext) -> {
            log.info("JobListener Tasklet");
            return RepeatStatus.FINISHED;
        };
    }

}
