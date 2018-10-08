package com.lotuslabs.actors.support;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.AbstractPollableChannel;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;

import com.lotuslabs.actors.PollableHandler;

public class Director {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(Director.class);

	private final Set<PollableHandler> pollableHandlers;

	private final AbstractPollableChannel defaultPollableChannel = new RendezvousChannel();
	private final AbstractSubscribableChannel defaultSubscriberChannel = new AbstractSubscribableChannel() {
		private final MessageDispatcher dispatcher = new BroadcastingDispatcher();
		@Override
		protected MessageDispatcher getDispatcher() {
			return dispatcher;
		}
	};
	private final AbstractSubscribableChannel defaultPublishSubscriberChannel = new PublishSubscribeChannel();

	public Director() {
		pollableHandlers = new HashSet<>();
	}

	public void checkActor(AbstractActor<?,?> actor) {
		if (actor instanceof PollableHandler)
			pollableHandlers.add((PollableHandler)actor);
	}


	public Set<PollableHandler> getPollableHandlers() {
		return Collections.unmodifiableSet(this.pollableHandlers);
	}

	/**
	 * @return the defaultPollableChannel
	 */
	public AbstractPollableChannel getDefaultPollableChannel() {
		return defaultPollableChannel;
	}

	/**
	 * @return the defaultSubscriberChannel
	 */
	public AbstractSubscribableChannel getDefaultSubscriberChannel() {
		return defaultSubscriberChannel;
	}

	public AbstractSubscribableChannel getPublishScubscriberChannel() {
		return this.defaultPublishSubscriberChannel;
	}

	private void initAll(AbstractActor<?,?>... actors) {
		for (AbstractActor<?,?> actor : actors) {
			actor.init(this);
		}
	}

	public void startAll(AbstractActor<?,?>... actors) {
		initAll(actors);
		for (AbstractActor<?,?> actor : actors) {
			start(actor);
		}
	}

	public void stopAll(AbstractActor<?,?>... actors) {
		for (AbstractActor<?,?> actor : actors) {
			actor.stop();
		}
	}


	public void start(AbstractActor<?,?> actor) {
		actor.start();
	}


}