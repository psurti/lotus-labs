package com.lotuslabs.actors.support;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.AbstractPollableChannel;
import org.springframework.integration.channel.AbstractSubscribableChannel;

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

	/**
	 * Constructor
	 */
	public Director() {
		this.actors = new Actors();
	}

	/**
	 * Return Actors
	 *
	 * @return
	 */
	Actors getActors() {
		return actors;
	}

	/**
	 * Register a different default subscriber channel
	 *
	 * @param channel - a configured subscriber channel
	 */
	public void registerDefaultSubscriberChannel(AbstractSubscribableChannel channel) {
		getActors().registerDefaultSubscriberChannel(channel);
	}

	/**
	 * Register a different default pollable channel
	 *
	 * @param channel - configured pollable channel
	 */
	public void registerDefaultPollableChannel(AbstractPollableChannel channel) {
		getActors().registerDefaultPollableChannel(channel);
	}

	/**
	 * Register a subscriber channel
	 *
	 * @param channelName - name of the channel
	 */
	public void registerSubscriberChannel(String channelName) {
		getActors().registerSubscriberChannel(channelName);
	}

	/**
	 * Register a custom named Pollable channel
	 *
	 * @param channelName - name of the channel
	 */
	public void registerPollableChannel(String channelName) {
		getActors().registerPollableChannel(channelName);
	}

	/**
	 * Return all the pollable receiver handlers
	 *
	 * @return a set of pollable handlers
	 */
	public Set<PollableHandler> getPollableHandlers() {
		return getActors().getPollableHandlers();
	}

	/**
	 * Initialize all actors
	 *
	 * @param actors - list of actors to initialize
	 */
	private void initAll(AbstractActor<?,?>... actors) {
		for (AbstractActor<?,?> actor : actors) {
			actor.init();
		}
	}

	/**
	 * Starts all actors
	 *
	 * @param actors - list of actors to start
	 */
	public void startAll(AbstractActor<?,?>... actors) {
		if (actors == null)
			throw new IllegalArgumentException("Invalid actors:" + actors);
		logger.info( "Starting {} actors ", actors.length);
		initAll(actors);
		for (AbstractActor<?,?> actor : actors) {
			actor.start();
		}
	}

	/**
	 * Stop all actors
	 *
	 * @param actors - list of actors to stop
	 */
	public void stopAll(AbstractActor<?,?>... actors) {
		if (actors == null)
			throw new IllegalArgumentException("Invalid actors:" + actors);
		logger.info( "Stopping {} actors ", actors.length);
		for (AbstractActor<?,?> actor : actors) {
			actor.stop();
		}
	}
}