package com.samples.batchextract.config;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.file.FlatFileFooterCallback;

import com.samples.batchextract.model.Payment;

// Define a footer callback with record-tracking logic
public class PaymentFooterCallback extends StepExecutionListenerSupport implements FlatFileFooterCallback {
	
	 private StepExecution stepExecution;
	
	 private long totalRecords = 0;
     private BigDecimal totalAmount = BigDecimal.ZERO;
    

    @Override
    public void writeFooter(Writer writer) throws IOException {
        writer.write(String.format("Total Records: %d", + stepExecution.getWriteCount()));
       
    }

    public void updateFooter(List<? extends Payment> items) {
        for (Payment payment : items) {
            totalRecords++;
            if (payment.getAmount() != null) {
                totalAmount = totalAmount.add(payment.getAmount());
            }
            System.out.println("totalRecords="+totalRecords);
            System.out.println("totalAmount="+totalAmount);
        }
    }
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }
}
