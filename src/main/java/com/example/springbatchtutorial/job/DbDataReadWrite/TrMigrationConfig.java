package com.example.springbatchtutorial.job.DbDataReadWrite;

import com.example.springbatchtutorial.core.domain.accounts.Accounts;
import com.example.springbatchtutorial.core.domain.accounts.AccountsRepository;
import com.example.springbatchtutorial.core.domain.orders.Orders;
import com.example.springbatchtutorial.core.domain.orders.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.List;

/**
 *  desc: 주문 테이블에서 정산 테이블로 데이터 이관
 *  run : --job.name=trMigrationJob
 */
@RequiredArgsConstructor
@Configuration
public class TrMigrationConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    private final OrdersRepository ordersRepository;
    private final AccountsRepository accountsRepository;

    @Bean
    public Job trMigrationJob(Step trMigrationStep) {
        return new JobBuilder("trMigrationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(trMigrationStep)
                .build();
    }

    /**
     * 간단한 작업이 아닌, DB 에 접근하거나 할때는 Tasklet 이 아닌, ItemProcessor, ItemReader 와 ItemWriter 를 사용한다.
     * .<read, write> : 안에는 어떤 데이터로 읽어와서 어떤 데이터로 쓸 건지에 대한 객체를 선언해주면 된다. 또한, chunk 는 몇개의 단위로 데이터를 처리할 것인지 사이즈를 설정한다.
     * 한마디로 아예의 예시는 5 개의 데이터 단위로 처리를 하고 커밋핡건데 읽어온 데이터의 타입은 Orders 가 될 것이고 데이터를 쓸 타입은 Orders 타입이 되겠다는 의미이다.
     * Spring-Batch 는 Transaction 를 chunk 라는 단위로 제공해준다.
     *
     * .reader() 에는 ItemReader 를 명시해주면 된다.
     * .writer() 에는 ItemWriter 를 명시해주면 된다. (DB 의 내용을 가공해서 Write 하려면 ItemProcessor 가 필요하다.)
     * @return
     */
    @Bean
    @JobScope
    public Step trMigrationStep(ItemReader<Orders> trOrdersReader,
                                ItemProcessor<Orders, Accounts> trOrdersProcessor,
                                ItemWriter<Accounts> toOrdersWriter) {
        return new StepBuilder("trMigrationStep", jobRepository)
                .<Orders, Accounts>chunk(5, platformTransactionManager)
                .reader(trOrdersReader)
//                .writer(chunk -> chunk.getItems().forEach(System.out::println))       // 이친구는 DB 에 Write 하는 것이 아니기 때문에 ItemProcessor 가 필요하지 않다.
                .processor(trOrdersProcessor)
                .writer(toOrdersWriter)
                .build();
    }

    /**
     * DB 에 접근할때는 데이터를 읽어오기위한 ItemReader 를 반환해야한다.
     * RepositoryItemReaderBuilder 로 ItemReader 를 만들고 반환한다. (Orders 객체로 데이터를 읽어온다)
     *
     * .name() : itemReader 의 이름을 설정한다.
     * .repository() : 는 DB 에 접근할 레포지토리를 명시해준다.
     * .methodName() : 는 .repository() 에서 명시한 레포지토리에서 실행한 메서드명을 입력한다.
     *                 만약 레포지토리에 직접 메서드를 작성했거나 파라미터가 존재하면, .arguments() 를 사용해서 파라미터를 넘겨주면 된다.
     * .pageSize() : 는 읽어올 데이터의 사이즈를 의미하며, 통상적으로 이전에 명시한 chunk 사이즈와 같은 값을 명시한다.
     * .sorts() :  는 정렬을 의미하며 Map 에 정렬 타입을 명시하여 정렬을 해줄 수 있다.
     *
     * --> 아래의 ItemReader 는 결과적으로 주문 테이블의 데이터를 읽어올 수 있게 된다.
     * @return
     */
    @Bean
    @StepScope
    public RepositoryItemReader<Orders> trOrdersReader() {
        return new RepositoryItemReaderBuilder<Orders>()
                .name("trOrdersReader")
                .repository(ordersRepository)
                .methodName("findAll")
                .pageSize(5)
                .arguments(List.of())
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    /**
     * ItemProcessor 는 ItemWriter 에게 전달되는 데이터를 가공하는 역할을 한다.
     *
     * 리턴값으로는 ItemWriter 에게 전달되는 데이터타입의 값을 리턴해주면 된다.
     * @return
     */
    @Bean
    @StepScope
    public ItemProcessor<Orders, Accounts> trOrdersProcessor() {
        // ItmeProcessor 익명 클래스 Override
        return Accounts::new;
    }

    /**
     * ItemProcessor 로 부터 가공된 데이터를 DB 에 쓰기 위한 ItemWriter 를 작성하는 로직이다. 제네릭에는 쓰고자하는 데이터타입이 들어간다.
     *
     * .repository() :  쓰고자하는 데이터의 Repository 가 들어감.
     * .methodName() : Repository 에서 사용하려는 메서드 이름
     * @return
     */
    @Bean
    @StepScope
    public RepositoryItemWriter<Accounts> toOrdersWriter() {
        return new RepositoryItemWriterBuilder<Accounts>()
                .repository(accountsRepository)
                .methodName("save")
                .build();
    }

    ///////////////////////////////////// 추가적인 로직 /////////////////////////////////////

    /**
     * RepositoryItemWriter 를 사용하지 않고, 일반적인 ItemWriter 를 구현해서 DB 에 가공된 데이터를 쓰기가 가능하다.
     * 순수한 ItemWriter 를 사용하게되면 .repository() 를 명시해주지않아도 된다.
     *
     * 하지만 데이터가 저장될 수 있도록 직접 repository 를 호출해서 사용해야 한다.
     * @return
     */
//    @Bean
//    @StepScope
//    public ItemWriter<Accounts> toOrdersWriter() {
//        return new ItemWriter<Accounts>() {
//            @Override
//            public void write(Chunk<? extends Accounts> chunk) throws Exception {
//                List<? extends Accounts> items = chunk.getItems();
//                accountsRepository.saveAll(items);
//            }
//        };
//    }

}
