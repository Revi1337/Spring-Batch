package com.example.springbatchtutorial.job.MultipleStep;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * https://docs.spring.io/spring-batch/docs/current/reference/html/index-single.html#simpleDelimitedFileReadingExample
 *
 * desc: 다중 step을 사용하기 및 step to step 데이터 전달
 * run param: --job.name=multipleStepJob
 */
@RequiredArgsConstructor
@Slf4j
@Configuration
public class MultipleStepJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    @Bean
    public Job multipleStepJob(Step multipleStep1, Step multipleStep2, Step multipleStep3) {
        return new JobBuilder("multipleStepJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(multipleStep1)
                .next(multipleStep2)
                .next(multipleStep3)
                .build();
    }

    @Bean
    @JobScope
    public Step multipleStep1() {
        return new StepBuilder("multipleStep1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("step1");
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

    /**
     * 다음 Step 단계에서 사용할 값을 넘겨주기위해서는 ExecutionContext 를 사용한다.
     * @return
     */
    @Bean
    @JobScope
    public Step multipleStep2() {
        return new StepBuilder("multipleStep2", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("step2");

                    ExecutionContext executionContext = chunkContext
                            .getStepContext()
                            .getStepExecution()
                            .getJobExecution()
                            .getExecutionContext();

                    executionContext.put("someKey", "hello!!");

                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

    /**
     * 이전 Step 단계에서 넣어준 값을 꺼내올때도 ExecutionContext 를 사용한다.
     * @return
     */
    @Bean
    @JobScope
    public Step multipleStep3() {
        return new StepBuilder("multipleStep3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("step3");

                    ExecutionContext executionContext = chunkContext
                            .getStepContext()
                            .getStepExecution()
                            .getJobExecution()
                            .getExecutionContext();

                    log.info("이전 스텝에서 넣어준 값을 꺼내온다 : {}", executionContext.get("someKey"));

                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

}
