package com.lotuslabs.actors.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import com.lotuslabs.actors.example.DBReadActor.DBEvent;
import com.lotuslabs.actors.support.AbstractActor;
import com.lotuslabs.actors.support.Director;

/**
 *
 * @author psurti
 *
 */
public class TransformActor extends AbstractActor<String,String> implements MessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(TransformActor.class);

	static class TransformedEvent extends GenericMessage<String> {
		private static final long serialVersionUID = 7981796782390064123L;
		public TransformedEvent(String payload) {
			super(payload);
		}
	}

	public TransformActor(Director director) {
		super(director);
		this.setNThreads(0); //disable any threads
		subscribe(this);
	}

	@Override
	public void stop() {
		unsubscribe(this);
		super.stop();
	}

	@Override
	public String execute(String item) {
		/*
		 * convert item and return newItem
		 */
		TransformedEvent transformedEvent = new TransformedEvent(item + "T");
		logger.info("transform: {} -> {}", item , transformedEvent.getPayload());
		put(transformedEvent);
		send(transformedEvent);
		return null;
	}



	@Override
	public void handleMessage(Message<?> message)  {
		if (message instanceof DBEvent) {
			logger.info("Received DBEvent:" + message.getPayload());
			invoke(()->execute((String) message.getPayload()));
		} else if (message instanceof EOSEvent ) {
			logger.info("Received EOSEvent - pass it along");
			put(message);
		}
	}

}
