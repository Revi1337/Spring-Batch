package com.example.springbatchtutorial.job.ConditionalStep;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * desc: step 결과의 따른 다음 step 분기 처리
 * run param: --job.name=conditionalStepJob
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ConditionalStepJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    @Bean
    public Job conditionalStepJob(
            Step conditionalStartStep,
            Step conditionalAllStep,
            Step conditionalFailStep,
            Step conditionalCompletedStep) {
        return new JobBuilder("conditionalStepJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                    .start(conditionalStartStep)                                // 첫번쨰 Step 을 실행해서
                    .on("FAILED").to(conditionalFailStep)               // 작업이 FAILED 이면 conditionalFailStep 실행
                .from(conditionalStartStep)                                     // 첫번쨰 Step 으로부터의 결과가
                    .on("COMPLETED").to(conditionalCompletedStep)       // 작업이 COMPLETED 이면 conditionalCompletedStep 실행
                .from(conditionalStartStep)                                     // 첫번쨰 Step 으로부터의 결과가
                    .on("*").to(conditionalAllStep)                     // 작업이 FAILED, COMPLETED 모두 아닌 나머지면 conditionalAllStep 실행
                .end()
                .build();
    }

    @Bean
    @JobScope
    public Step conditionalStartStep() {
        return new StepBuilder("conditionalStartStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
//                    System.out.println("conditional Start Step");
//                    return RepeatStatus.FINISHED;
                    throw new Exception("Exception!!");
                }, platformTransactionManager)
                .build();
    }

    @JobScope
    @Bean
    public Step conditionalAllStep() {
        return new StepBuilder("conditionalAllStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("conditional All Step");
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

    @JobScope
    @Bean
    public Step conditionalFailStep() {
        return new StepBuilder("conditionalFailStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("conditional Fail Step");
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

    @JobScope
    @Bean
    public Step conditionalCompletedStep() {
        return new StepBuilder("conditionalCompletedStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("conditional Completed Step");
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }
}