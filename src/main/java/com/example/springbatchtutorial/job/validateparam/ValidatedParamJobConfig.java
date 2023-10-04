package com.example.springbatchtutorial.job.validateparam;


import com.example.springbatchtutorial.job.validateparam.validator.FileParamValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;

/**
 * desc: 파일 이름 파라미터 전달 그리고 검증
 * run: --spring.batch.job.names=validatedParamJob fileName=test.csv
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class ValidatedParamJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    /**
     * Spring Batch 에서는 Job 에서 validator() 를 통해 jobParameters 를 검증할 수 있다.
     * 또한 CompositeJobParametersValidator 를 이용해서 다수의 validator 를 등록할 수 있다.
     *
     * @param validatedParamStep
     * @return
     */
    @Bean
    public Job validatedParamJob(Step validatedParamStep) {
        return new JobBuilder("validatedParamJob", jobRepository)
                .incrementer(new RunIdIncrementer())
//                .validator(new FileParamValidator())
                .validator(multipleValidator())
                .start(validatedParamStep)
                .build();
    }

    @Bean
    @JobScope
    public Step validatedParamStep(Tasklet validatedParamTasklet) {
        return new StepBuilder("validatedParamStep", jobRepository)
                .tasklet(validatedParamTasklet, platformTransactionManager)
                .build();
    }

    /**
     * Spring Batch 에서는 jobParameters 를 지원한다.
     * fileName=값 으로 들오온 값은 Job 에서 Tasklet 에서 사용할 수 있다.
     * @param fileName
     * @return
     */
    @Bean
    @StepScope
    public Tasklet validatedParamTasklet(@Value("#{jobParameters['fileName']}") String fileName) {
        return (contribution, chunkContext) -> {
            log.info("tasklet = {}", fileName);
            log.info("ValidatedParam Tasklet");
            return RepeatStatus.FINISHED;
        };
    }

    private CompositeJobParametersValidator multipleValidator() {
        CompositeJobParametersValidator compositeJobParametersValidator = new CompositeJobParametersValidator();
        compositeJobParametersValidator.setValidators(List.of(new FileParamValidator()));
        return compositeJobParametersValidator;
    }

}
