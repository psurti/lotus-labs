package com.lotuslabs.actors.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.GenericMessage;

import com.lotuslabs.actors.support.AbstractActor;
import com.lotuslabs.actors.support.Director;


/**
 *
 * @author psurti
 *
 */
public class DBReadActor extends AbstractActor<String,String> {
	private static final Logger logger = LoggerFactory.getLogger(DBReadActor.class);

	static class DBEvent extends GenericMessage<String> {
		private static final long serialVersionUID = 7981796782390064123L;
		public DBEvent(String payload) {
			super(payload);
		}
	}

	public DBReadActor(Director director) {
		super(director);
		setNThreads(5);
	}

	@Override
	public String execute(String item) {
		logger.info("Executing {}" , DBReadActor.class.getSimpleName());
		//Read database rows and notify/send
		//next action
		char start = 'A';
		char end = 'Z';
		for (int i = start; i <= end; i++) {
			String v = (char)i + String.valueOf(Thread.currentThread().getId());
			logger.info("send message: {}", v);
			send( new DBEvent( v ));
		}
		return null;
	}


}
