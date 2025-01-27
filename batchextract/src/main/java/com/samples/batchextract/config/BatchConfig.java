package com.samples.batchextract.config;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.samples.batchextract.model.Payment;

@Configuration
public class BatchConfig {

	@Autowired
	private DataSource dataSource;

	private static final String QUERY = "SELECT id, check_number, payment_status, amount, payment_date, vehicle_code FROM batch_repo.payment";

	@Bean
	public JdbcCursorItemReader<Payment> jdbcCursorItemReader() {
		return new JdbcCursorItemReaderBuilder<Payment>().name("paymentItemReader").dataSource(dataSource).sql(QUERY)
				.rowMapper((rs, rowNum) -> {
					Payment payment = new Payment();
					payment.setId(rs.getLong("id"));
					payment.setCheckNumber(rs.getString("check_number"));
					payment.setPaymentStatus(rs.getString("payment_status"));
					payment.setAmount(rs.getBigDecimal("amount"));
					payment.setPaymentDate(rs.getDate("payment_date").toLocalDate());
					payment.setVehicleCode(Payment.VehicleCode.valueOf(rs.getString("vehicle_code")));
					return payment;
				}).build();
	}

	@Bean
	@StepScope
	public PaymentItemWriter paymentItemWriter() {
		PaymentItemWriter paymentItemWriter = new PaymentItemWriter();

		paymentItemWriter.setDelegate(flatFileItemWriter());

		return paymentItemWriter;
	}

	@Bean
	@StepScope
	public FlatFileItemWriter<Payment> flatFileItemWriter() {

		return new FlatFileItemWriterBuilder<Payment>().name("paymentItemWriter")
				.resource(new FileSystemResource("payments.csv")).delimited().delimiter(",")
				.names("id", "checkNumber", "paymentStatus", "amount", "paymentDate", "vehicleCode")
				.headerCallback(
						writer -> writer.write("ID,Check Number,Payment Status,Amount,Payment Date,Vehicle Code"))
				.footerCallback(paymentItemWriter())
				.build();

	}

	// 2. Step Configuration
	@Bean
	Step paymentStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			JdbcCursorItemReader<Payment> reader, PaymentItemWriter writer) {
		PaymentFooterCallback footerCallback = new PaymentFooterCallback();
		return new StepBuilder("paymentStep2", jobRepository).<Payment, Payment>chunk(10, transactionManager)
				.reader(reader).writer(paymentItemWriter()) // Print each Payment record to the console
				.build();
	}

	@Bean
	public Job paymentJob(final JobRepository jobRepository, final PlatformTransactionManager transactionManager,
			JdbcCursorItemReader<Payment> reader, PaymentItemWriter writer) throws IOException {
		return new JobBuilder("paymentJob2", jobRepository).incrementer(new RunIdIncrementer())
				.start(paymentStep(jobRepository, transactionManager, reader, writer)).build();
	}

}
