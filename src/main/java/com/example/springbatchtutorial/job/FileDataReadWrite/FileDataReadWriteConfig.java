package com.example.springbatchtutorial.job.FileDataReadWrite;

import com.example.springbatchtutorial.job.FileDataReadWrite.dto.Player;
import com.example.springbatchtutorial.job.FileDataReadWrite.dto.PlayerYears;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * https://docs.spring.io/spring-batch/docs/current/reference/html/index-single.html#simpleDelimitedFileReadingExample
 * --job.name=fileReadWriteJob
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class FileDataReadWriteConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    @Bean
    public Job fileReadWriteJob(Step fileReadWriteStep) {
        return new JobBuilder("fileReadWriteJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fileReadWriteStep)
                .build();
    }

    /**
     * .<read, write>chunk(chunkSize) : 몇개의 단위로 데이터를 처리할 것인지 명시. (read 에는 읽어들이려는 type, write 에는 쓰려는 타입)
     * .reader() : ItemReader 명시 --> 이전에는 DB 에서 값을 읽어들이기때문에 RepositoryItemReader 를 사용했지만
     *             이번에는 파일을 읽어들이기 때문에 FlatFileItemReader 를 사용한다.
     * .writer() : ItemWriter 명시 --> 단순히 Writer 를 통해 값을 출력하는것이라면 ItemProcessor 는 필요없음.
     *             하지만 새롭게 객체에 쓰거나 DB 에 쓰려는 경우에는 ItemProcessor 가 꼭 필요함.
     * @return
     */
    @Bean
    @JobScope
    public Step fileReadWriteStep(ItemReader<Player> playerFlatFileItemReader,
                                  ItemProcessor<Player, PlayerYears> playerItemProcessor,
                                  ItemWriter<PlayerYears> playerFlatFileItemWriter) {
        return new StepBuilder("fileReadWriteStep", jobRepository)
                .<Player, PlayerYears>chunk(5, platformTransactionManager)
                .reader(playerFlatFileItemReader)
//                .writer(chunk -> chunk.getItems().forEach(System.out::println))
                .processor(playerItemProcessor)
                .writer(playerFlatFileItemWriter)
                .build();
    }


    /**
     * 파일을 읽어들이기 위해서는 FlatFileItemReader<읽어드리려는타입> 을 사용한다.
     * .name() : itemReader 의 이름을 명시
     * .resouce() : 읽어올 파일의 위치를 지정
     * .lineTokenizer() : 데이터를 어떤 기준으로 나누어 줄지 기준을 정하기 위해서 사용.
     * .fieldSetMapper() : 읽어온 데이터를 객체로 변경할 수 있도록 mapper 가 필요함. 이 mapper 는 만들어주어야하고, 이것을 매핑하면 됨.
     * .linesToSkip() : n 번쨰 줄은 스킵을 하겠다고 명시할 수 있음.
     * @return
     */
    @Bean
    @StepScope
    public FlatFileItemReader<Player> playerFlatFileItemReader() {
         return new FlatFileItemReaderBuilder<Player>()
                 .name("playerItemReader")
                 .resource(new FileSystemResource("Players.csv"))
                 .lineTokenizer(new DelimitedLineTokenizer())
                 .fieldSetMapper(new PlayerFieldSetMapper())
                 .linesToSkip(1)
                 .build();
    }

    /**
     * ItemProcessor<Player, PlayerYears> : Player 를 PlayerYears 로 변경할 수 있도록 제네릭 명시.
     * ItemProcessor 는 특정 객체로 변환 및 가공해주는 역할을 함.
     */
    @Bean
    @StepScope
    public ItemProcessor<Player, PlayerYears> playerItemProcessor() {
        return PlayerYears::new;
    }

    /**
     * FlatFileItemWriter<?> 는 Processor 로 부터 넘어온 가공된 데이터를 쓰는 역할을 한다. 제네릭에는 쓰려는(사용하려는) 객체의 타입을 명시한다.
     * 1. 어떤 필드를 사용할지 명시하기위해서 BeanWrapperFieldExtractor 가 사용된다.
     * 2. BeanWrapperFieldExtractor 의 setNames() 메서드로 추출할 필드이름을 배열로 명시해주면 된다.
     *
     * 3. 어떤 기준으로 파일을 만들어주는지 알려주기 위해 DelimitedLineAggregator 객체 사용되며 setDelimiter() 메서드로
     *    어떤 기준으로 파일을 만들어주는지 구분자를 지정할 수 있다.
     * 4. 또한, 필드를 추출할 수 있도록 setFieldExtractor() 메셔드로 이전의 BeanWrapperFieldExtractor 인스턴스를 넘겨준다.
     *
     * 5. 파일을 어디다가 쓸 것인지 FileResource 가 필요하다.
     * @return
     */
    @Bean
    @StepScope
    public FlatFileItemWriter<PlayerYears> playerFlatFileItemWriter() {
        BeanWrapperFieldExtractor<PlayerYears> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
        beanWrapperFieldExtractor.setNames(new String[]{"ID", "lastName", "position", "yearsExperience"});
        beanWrapperFieldExtractor.afterPropertiesSet();

        DelimitedLineAggregator<PlayerYears> delimitedLineAggregator = new DelimitedLineAggregator<>();
        delimitedLineAggregator.setDelimiter(",");
        delimitedLineAggregator.setFieldExtractor(beanWrapperFieldExtractor);

        FileSystemResource fileSystemResource = new FileSystemResource("players_output.txt");

        return new FlatFileItemWriterBuilder<PlayerYears>()
                .name("playerItemWriter")
                .resource(fileSystemResource)
                .lineAggregator(delimitedLineAggregator)
                .build();
    }

}
