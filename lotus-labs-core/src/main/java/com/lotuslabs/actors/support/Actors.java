/*
 * Licensed to surti-labs under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Surti-labs licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.lotuslabs.actors.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.integration.channel.AbstractPollableChannel;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.RendezvousChannel;

import com.lotuslabs.actors.PollableHandler;

/**
 * Communication Channels for all Actors
 *
 * @author psurti
 * queue:default?size=200
 */
public class Actors {

	public static final String DEFAULT_CHANNEL_NAME = "default";

	private static final String INVALID_CHANNEL_NAME = "Invalid channelName";
	private final Map<String,AbstractSubscribableChannel> allSubscriberChannels;
	private final Map<String,AbstractPollableChannel> allPollableChannels;
	private final Set<PollableHandler> pollableHandlers;

	/**
	 * Constructor
	 */
	public Actors() {
		this.pollableHandlers = new HashSet<>();
		this.allSubscriberChannels = new HashMap<>();
		this.allPollableChannels = new HashMap<>();
		this.registerSubscriberChannel(DEFAULT_CHANNEL_NAME);
		this.registerPollableChannel(DEFAULT_CHANNEL_NAME);
	}

	/**
	 * Check Actor for PollableHandlers
	 * @param actor
	 */
	void checkActor(AbstractActor<?,?> actor) {
		if (actor instanceof PollableHandler)
			pollableHandlers.add((PollableHandler)actor);
	}

	/**
	 * Register a subscriber channel
	 *
	 * @param channelName - name of the channel
	 */
	void registerSubscriberChannel(String channelName) {
		if (channelName == null)
			throw new IllegalArgumentException(INVALID_CHANNEL_NAME);
		AbstractSubscribableChannel publishSubscriberChannel = new PublishSubscribeChannel();
		publishSubscriberChannel.setComponentName(channelName);
		this.allSubscriberChannels.put(channelName, publishSubscriberChannel);
	}

	/**
	 * Register a different default subscriber channel
	 *
	 * @param channel - a configured subscriber channel
	 */
	void registerDefaultSubscriberChannel(AbstractSubscribableChannel channel) {
		if (channel != null)
			this.allSubscriberChannels.put(DEFAULT_CHANNEL_NAME, channel);
	}

	/**
	 * Register a different default pollable channel
	 *
	 * @param channel - configured pollable channel
	 */
	void registerDefaultPollableChannel(	AbstractPollableChannel channel ) {
		if (channel != null)
			this.allPollableChannels.put(DEFAULT_CHANNEL_NAME, channel);
	}

	/**
	 * Register a custom named Pollable channel
	 *
	 * @param channelName - name of the channel
	 */
	void registerPollableChannel(String channelName) {
		if (channelName == null)
			throw new IllegalArgumentException(INVALID_CHANNEL_NAME);

		AbstractPollableChannel pollableChannel = new RendezvousChannel();
		pollableChannel.setComponentName(channelName);
		this.allPollableChannels.put(channelName, pollableChannel);
	}

	/**
	 * Return all the pollable receiver handlers
	 *
	 * @return a set of pollable handlers
	 */
	Set<PollableHandler> getPollableHandlers() {
		return Collections.unmodifiableSet(this.pollableHandlers);
	}

	/**
	 * Return the default subscriber channel
	 *
	 * @return default subscriber channel
	 */
	AbstractSubscribableChannel getDefaultSubscriberChannel() {
		return getSubscriberChannel(DEFAULT_CHANNEL_NAME);
	}

	/**
	 * Return the default pollable channel
	 *
	 * @return default pollable channel
	 */
	AbstractPollableChannel getDefaultPollableChannel() {
		return getPollableChannel(DEFAULT_CHANNEL_NAME);
	}

	/**
	 * Return a custom-named subscriber channel
	 *
	 * @return subscriberChannel associated to the channel name
	 */
	AbstractSubscribableChannel getSubscriberChannel(String channelName) {
		if (channelName == null)
			throw new IllegalArgumentException(INVALID_CHANNEL_NAME);

		AbstractSubscribableChannel ret = allSubscriberChannels.get(channelName);
		if (ret == null)
			throw new IllegalArgumentException("Unknown channel - Please register subscriber channel:" + channelName);
		return ret;
	}

	/**
	 * Return a custom-named pollable channel
	 *
	 * @return pollableChannel associated to the channel name
	 */
	AbstractPollableChannel getPollableChannel(String channelName) {
		if (channelName == null)
			throw new IllegalArgumentException(INVALID_CHANNEL_NAME);

		//XXX need to check by channel names as opposed to any pollablehandler
		if (getPollableHandlers().isEmpty())
			return null;

		AbstractPollableChannel ret = allPollableChannels.get(channelName);
		if (ret == null)
			throw new IllegalArgumentException("Unknown channel - Please register pollable channel:" + channelName);
		return ret;
	}

}
