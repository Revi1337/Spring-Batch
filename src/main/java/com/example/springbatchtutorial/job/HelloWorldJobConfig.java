package com.example.springbatchtutorial.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * job -> step -> tasklet
 * 1 대 -> N 대 -> 여러가지 Tasklet.
 * job 이 step 을 step 이 tasklet 을 실행하는 구조.
 */
@RequiredArgsConstructor
@Configuration
public class HelloWorldJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    /**
     * Job 을 생성하는 코드이며 Job 을 생성할떄는 JobBuilder 를 통해 만든다.
     * Job 의 이름을 지정해주고, JobRepository 를 주입받아 JobBuilder 의 생성자로 전달해준다.
     * Job 을 수행할떄 아이디를 부여하는데 Sequence 를 순차적으로 부여할 수 있도록 RunIdIncrementer 를 명시해주면 된다.
     * Job 은 Step 을 실행하기 떄문에, 실행할 Step 을 지정해주면 된다.
     * @return
     */
    @Bean
    public Job helloWorldJob() {
        return new JobBuilder("helloWorldJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(helloWorldStep())
                .build();
    }

    /**
     * Step 을 만들어주는 코드이며, Job 을 통해 실행된다는것을 명시하기위해 @JobScope 를 선언해준다.
     * Step 의 이름을 지정해주고, JobRepository 를 주입받아 JobBuilder 의 생성자로 전달해준다.
     * Step 은 ItemReader, Processor, Writer 등의 읽고쓰는 동작하게하는데 이러한 작업들이 없는 단순한 작업이라면 tasklet 를 사용한다.
     * @return
     */
    @JobScope
    @Bean
    public Step helloWorldStep() {
        return new StepBuilder("helloWorldJob", jobRepository)
                .tasklet(helloWorldTasklet(), platformTransactionManager)
                .build();
    }

    /**
     * Tasklet 을 만들어주는 코드이며, Step 하위. 즉 Step 을 통해 실행된다는것을 명시하기위해 @StepScope 를 선언해준다.
     * new TaskLet() 객체를 리턴해주면된다. (lambda)
     * 또한 우리가 원하는 작업이 끝난 이후에는 이 작업을 어떻게 할 것인가에 따른 Status 를 명시해야한다. (FINISHED 는 Step 을 Finish)
     * @return
     */
    @StepScope
    @Bean
    public Tasklet helloWorldTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("Hello World Spring Batch");
            return RepeatStatus.FINISHED;
        };
    }

}
