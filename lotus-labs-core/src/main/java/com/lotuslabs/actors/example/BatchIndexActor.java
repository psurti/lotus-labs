package com.lotuslabs.actors.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import com.lotuslabs.actors.PollableHandler;
import com.lotuslabs.actors.support.AbstractActor;
import com.lotuslabs.actors.support.Director;

/**
 *
 * @author psurti
 *
 */
public class BatchIndexActor extends AbstractActor<String,String> implements PollableHandler {

	private static final Logger logger = LoggerFactory.getLogger(BatchIndexActor.class);

	private Collection<String> payloads;

	public BatchIndexActor(Director director) {
		super(director);
		payloads = new ArrayList<>();
		this.setNThreads(1);
	}

	@Override
	public void start() {
		super.start();
		invokeAll(()->execute((String)null));
	}

	@Override
	public String execute(String item) {
		Message<?> msg;
		while ((msg = receive()) != null) {
			logger.info( "Received Payload: {} ", msg.getPayload() );
			if (msg instanceof EOSEvent) {
				logger.info("Received EOSEvent - flush & break-off-from-loop");
				flush();
				break;
			}
			String val = (String) msg.getPayload();
			//ask data and index them in bulk
			payloads.add(val);
			if (payloads.size() % 10 == 0) {
				flush();
			}
		}
		logger.info( "Exiting 'receive' thread-loop");
		return null;
	}



	@Override
	public void flush() {
		String[] batch = payloads.toArray(new String[0]);
		List<String> list = Arrays.asList(batch);
		payloads.removeAll(list);
		logger.info("Index elements: {}", list);
	}

}
