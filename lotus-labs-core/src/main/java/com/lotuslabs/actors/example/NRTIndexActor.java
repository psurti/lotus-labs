package com.lotuslabs.actors.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import com.lotuslabs.actors.example.TransformActor.TransformedEvent;
import com.lotuslabs.actors.support.AbstractActor;
import com.lotuslabs.actors.support.Director;

/**
 *
 * @author psurti
 *
 */
public class NRTIndexActor extends AbstractActor<String,String> implements MessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(NRTIndexActor.class);

	public NRTIndexActor(Director director) {
		super(director);
		setNThreads(0); //set 0 if using local invoke call
	}


	@Override
	public void start() {
		subscribe(this);
		super.start();
	}


	@Override
	public void stop() {
		unsubscribe(this);
		super.stop();
	}

	@Override
	public String execute(String item) {
		logger.info("Indexed {}", item);
		return null;
	}

	@Override
	public void handleMessage(Message<?> message)  {
		if (message instanceof TransformedEvent) {
			logger.info("Received TransformedEvent:{}", message.getPayload());
			invoke(()->execute((String) message.getPayload()));
		}
	}

}
