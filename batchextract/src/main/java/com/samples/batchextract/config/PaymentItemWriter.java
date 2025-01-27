package com.samples.batchextract.config;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import com.samples.batchextract.model.Payment;

public class PaymentItemWriter implements ItemWriter<Payment>, ItemStream, FlatFileFooterCallback, InitializingBean {

	private ResourceAwareItemWriterItemStream<Payment> delegate;

	private BigDecimal totalAmount = BigDecimal.ZERO;

	// writer.write("Total Amount Processed: " + totalAmount);

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "A delegate ItemWriter must be provided.");
	}

	public void setDelegate(ResourceAwareItemWriterItemStream<Payment> delegate) {
		this.delegate = delegate;
	}

	public void write(Chunk<? extends Payment> chunk) throws Exception {

		BigDecimal chunkTotal = BigDecimal.ZERO; // Tracks the total amount for the current chunk

		for (Payment payment : chunk.getItems()) {

			chunkTotal = chunkTotal.add(payment.getAmount());
			System.out.println("chunkTotal=" + chunkTotal);
		}

		delegate.write(chunk);

		// Update the cumulative total amount after successful writing
		totalAmount = totalAmount.add(chunkTotal);

	}

	@Override
	public void writeFooter(Writer writer) throws IOException {

		writer.write("Total Amount Processed: " + totalAmount);
		/*
		 * for (int i = 0; i < chunkMD5.size(); i++) {
		 * writer.append(String.format("Chunk %03d> %s", i, chunkMD5.get(i)));
		 * writer.append(System.getProperty("line.separator")); }
		 */
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		delegate.close();
	}

	/*
	 * private ItemWriter<Payment> delegate;
	 * 
	 * public void setDelegate(ItemWriter delegate) {
	 * 
	 * this.delegate = delegate; }
	 */

	/*
	 * public void write(Chunk<? extends Payment> chunk) throws Exception {
	 * BigDecimal chunkTotal = BigDecimal.ZERO; for (Payment payment :
	 * chunk.getItems()) { chunkTotal = chunkTotal.add(payment.getAmount());
	 * System.out.println("chunkTotal="+chunkTotal); }
	 * 
	 * this.write(chunk);
	 * 
	 * // After successfully writing all items totalAmount =
	 * totalAmount.add(chunkTotal); }
	 */

	/*
	 * public void writeFooter(Writer writer) throws IOException {
	 * writer.write("Total Amount Processed: " + totalAmount); }
	 */

}