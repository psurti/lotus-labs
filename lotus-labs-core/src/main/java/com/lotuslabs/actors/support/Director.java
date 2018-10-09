package com.lotuslabs.actors.support;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lotuslabs.actors.PollableHandler;

/**
 * Directs all the Actors to perform their actions
 *
 * @author psurti
 */
public class Director {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(Director.class);

	private final Actors actors;

	public Director() {
		this.actors = new Actors();
	}


	Actors getActors() {
		return actors;
	}

	public void registerSubscriberChannel(String channelName) {
		getActors().registerSubscriberChannel(channelName);
	}



	public void registerPollableChannel(String channelName) {
		getActors().registerSubscriberChannel(channelName);
	}


	public Set<PollableHandler> getPollableHandlers() {
		return getActors().getPollableHandlers();
	}


	private void initAll(AbstractActor<?,?>... actors) {
		for (AbstractActor<?,?> actor : actors) {
			actor.init();
		}
	}

	public void startAll(AbstractActor<?,?>... actors) {
		initAll(actors);
		for (AbstractActor<?,?> actor : actors) {
			actor.start();
		}
	}

	public void stopAll(AbstractActor<?,?>... actors) {
		for (AbstractActor<?,?> actor : actors) {
			actor.stop();
		}
	}
}