package com.lotuslabs.actors.support;

import static com.lotuslabs.actors.support.MessageChannels.ROUTER1_CHANNEL;
import static com.lotuslabs.actors.support.MessageChannels.ROUTER2_CHANNEL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import com.lotuslabs.actors.PollableHandler;

public class QueueConsumerActor extends AbstractActor<String,Void> implements PollableHandler, MessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(QueueConsumerActor.class);

	private static volatile int i = 1;

	private class RxChannelHandler implements MessageHandler {
		QueueConsumerActor actor;
		String name;
		public RxChannelHandler(QueueConsumerActor actor, String name) {
			this.actor = actor;
			this.name = name;
			logger.info( "new.rx.handler: " + name + " " + this.hashCode());
		}
		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			logger.info( "On CHANNEL:" + name);
			actor.handleMessage(message);
		}
	}

	/**
	 * Register Channels in the constructor
	 * @param director
	 */
	public QueueConsumerActor(Director director) {
		super(director);
		director.registerSubscriberChannel(ROUTER1_CHANNEL);
		director.registerSubscriberChannel(ROUTER2_CHANNEL);
	}

	/**
	 * Subscribe channels in the start calls
	 */
	@Override
	public void start() {
		super.start();
		if (i == 1) {
			subscribe(ROUTER1_CHANNEL, new RxChannelHandler(this, ROUTER1_CHANNEL));
			i++;
			logger.info("Subscribed on " + ROUTER1_CHANNEL);
		} else {
			subscribe(ROUTER2_CHANNEL, new RxChannelHandler(this, ROUTER2_CHANNEL));
			logger.info("Subscribed on " + ROUTER2_CHANNEL);
		}
		subscribe(this); //last stmt for safer

	}

	@Override
	public void stop() {
		super.stop();
		unsubscribe(this);
	}

	@Override
	public Void execute(String item) {
		Message<?> msg;
		while ((msg = receive()) != null) {
			logger.info( "Received Payload: {} ", msg.getPayload() );
			if (msg instanceof EOSEvent) {
				logger.info("Received EOSEvent - flush & break-off-from-loop");
				break;
			}
			String val = (String) msg.getPayload();
			logger.info("Consumed {}", val);
		}
		logger.info( "Exiting 'receive' thread-loop");
		return null;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		if (message instanceof EOSEvent) {
			for (int i = 0; i < getNThreads(); i++)
				put(EOS_EVENT);
		} else  {

			logger.info( "message rvcd: " + message.getPayload() + " " + this.hashCode());
		}
	}

}
