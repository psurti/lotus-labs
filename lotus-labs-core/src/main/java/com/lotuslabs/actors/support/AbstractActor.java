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

import java.io.Flushable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.AbstractPollableChannel;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.support.MutableMessage;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;

import com.lotuslabs.actors.PollableHandler;
import com.lotuslabs.actors.actions.Action;

/**
 * The base class of all actors. An actor is action
 * with additional capabilties like inter-action communications
 * with pollaable, pub-sub channels and ability to execute
 * actions on single or multiple threads.
 * <p>
 * Each actor has a default subscriber and pollable channel as
 * well as a dedicated thread to do its job
 * <p>
 * An actor that consumes events via subscription implments
 * {@link MessageHandler} while one that consumes events via
 * a polling mechanism (calls {@link AbstractActor#receive()}
 * <bold>must</bold> implment {@link PollableHandler}
 *
 * @author psurti
 * @param <I>
 * @param <O>
 */
public abstract class AbstractActor<I,O> extends Action<I,O>
implements Flushable, PollableChannel, SubscribableChannel {

	private static final Logger logger = LoggerFactory.getLogger(AbstractActor.class);

	private boolean isRunning = false;
	private ExecutorService executor;
	private int nThreads;
	private final Actors actors;
	private final Routers routers;

	/*
	 * Queue is ready for poll signal
	 */
	public static class PollReadyEvent extends GenericMessage<Object> {
		private static final long serialVersionUID = 4304015635624712083L;
		public PollReadyEvent() {
			super(new Object());
		}
	}
	/**
	 * A {@code PollReadyEvent} can be used for notifying
	 * a {@link PollableHandler} so that it can receive
	 * call to get the data from a queue
	 */

	public static final PollReadyEvent POLL_READY_EVENT = new PollReadyEvent();

	/*
	 * End of stream is reached signal
	 */
	public static class EOSEvent extends MutableMessage<Object> {
		public static final String POISSON_PILL = "POISSON_PILL";
		private static final long serialVersionUID = 4304015635624712083L;
		public EOSEvent() {
			super(new Object());
			this.getHeaders().put(POISSON_PILL, new Object());
		}
	}
	/**
	 * A actor can be notified that the data stream has
	 * ended
	 */
	public static final EOSEvent EOS_EVENT = new EOSEvent();


	/**
	 * Construct an actor using a director
	 *
	 * @param director
	 */
	public AbstractActor(Director director) {
		nThreads = 1;
		this.actors = director.getActors();
		this.routers = new Routers(director.getActors());
		this.actors.checkActor(this); //safe as last stmt to avoid partial initialized objects
	}

	/**
	 * By default every actor is decicated
	 * a single thread to execute its action.
	 * {@link #nThreads = 1}. If an actor
	 * works on the caller thread than a decicated
	 * thread maybe not be necessary (0)
	 *
	 * @param nThreads 0 for no actor thread 1 is default
	 */
	public void setNThreads(int nThreads) {
		this.nThreads = nThreads;
	}

	/**
	 * Returns the number of dedicated threads
	 * associated with this actor
	 *
	 * @return number of threads
	 */
	public int getNThreads() {
		return this.nThreads;
	}

	/**
	 * Returns the Routers to configure
	 * @return Routers that can be configured
	 */
	protected Routers routers() {
		return this.routers;
	}

	/**
	 * Initializes this actor
	 * Part of the initializes includes
	 * setting up the thread pool associated
	 * with the number of threads {@link #nThreads}
	 */
	void init() {
		final String threadName = getClass().getSimpleName();
		if (nThreads > 0) {
			this.executor = Executors.newFixedThreadPool(nThreads, r -> {
				Thread thr = new Thread(r);
				thr.setName( threadName + thr.getName() );
				return thr;
			});
		}
	}

	/**
	 * Start this actor
	 */
	@Override
	public void start() {
		logger.info("Started {}", getClass().getName());
		if (nThreads > 0) //call execute only if atleast 1 thread is dedicated
			invokeAll(() -> execute((I) null));
	}

	/**
	 * Stop this actor
	 */
	@Override
	public void stop() {
		logger.info("Attempting to stop {}" , getClass().getName());
		if (this.executor != null) {
			this.executor.shutdownNow();
		}
		logger.info("Stopped {} {}",  getClass().getName(), isRunning);
	}

	/**
	 * Is this actor running
	 */
	@Override
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Execute a method call in a {@link Runnable} on all the threads
	 * on this actor.
	 *
	 * <p>
	 * If {@link #nThreads} = 0 then it will execute this action on
	 * the current thread.
	 * <p>
	 * If {@link #nThreads} > 0 then it will start all threads but
	 * execute the action on available thread
	 *
	 * @param r is a runnable performs an execution call
	 */
	protected void invokeAll(Runnable r) {
		if (this.nThreads == 0) {
			r.run();
			return;
		}
		if (this.executor != null) {
			for (int i = 0; i < getNThreads(); i++) {
				this.executor.execute(r);
			}
		}
	}

	/**
	 * Execute a method in a {@link Runnable} on this actor on a
	 * thread pool thread this actor owns. It assumes only 1 thread
	 * is available.
	 *
	 * <p>
	 * If {@link #nThreads} = 0 then it will execute this action on
	 * the current thread.
	 *
	 * @param r is a runnable performs an execution call
	 */
	protected void invoke(Runnable r) {
		if (this.nThreads == 0) {
			r.run();
			return;
		}
		if (this.executor != null) {
			this.executor.execute(r);
		}
	}

	/**
	 * Subscribe the {@link MessageHandler} handler
	 * on the default subscriber channel {@link Actors#DEFAULT_CHANNEL_NAME}
	 */
	@Override
	public boolean subscribe(MessageHandler handler) {
		return subscribe(Actors.DEFAULT_CHANNEL_NAME, handler);
	}

	/**
	 * Subscribe the {@link MessageHandler} handler
	 * on a custom channel name
	 *
	 * @param channelName - the name of the channel
	 * @param handler - the message channel handler
	 * @return true if successful
	 */
	protected boolean subscribe(String channelName,MessageHandler handler) {
		AbstractSubscribableChannel channel = this.actors.getSubscriberChannel(channelName);
		if (channel != null)
			return channel.subscribe(handler);
		return false;
	}

	/**
	 * Unsubscribe the {@link MessageHandler} handler
	 * for the custom subscriber channel name
	 *
	 * @param channelName - the message channel
	 * @param handler - the message channel handler
	 * @return true if successful
	 */
	protected boolean unsubscribe(String channelName, MessageHandler handler) {
		AbstractSubscribableChannel channel = this.actors.getSubscriberChannel(channelName);
		if (channel != null)
			return channel.unsubscribe(handler);
		return false;
	}

	/**
	 * Unsubscribe the {@link MessageHandler} handler
	 * on the default subscriber channel {@link Actors#DEFAULT_CHANNEL_NAME}
	 */
	@Override
	public boolean unsubscribe(MessageHandler handler) {
		return unsubscribe(Actors.DEFAULT_CHANNEL_NAME, handler);
	}

	/**
	 * Send a message to a specific channel name on a subscriber
	 * channel
	 *
	 * @param channelName - the name of the channel
	 * @param message - the message to send
	 * @return true if successful
	 */
	protected boolean send(String channelName, Message<?> message) {
		AbstractSubscribableChannel channel = this.actors.getSubscriberChannel(channelName);
		if (channel != null)
			return channel.send(message);
		return false;
	}

	/**
	 * Send a message to a specific channel name with timeout
	 *
	 * @param channelName - the name of the channel
	 * @param message - the message to send
	 * @param timeout - the time to wait until return
	 * @return true if successful
	 */
	protected boolean send(String channelName, Message<?> message, long timeout) {
		AbstractSubscribableChannel channel = this.actors.getSubscriberChannel(channelName);
		if (channel != null)
			return channel.send(message, timeout);
		return false;
	}

	/**
	 * Send a message to multiple channels based on a router with timeout
	 * @param router - the different types of routers to use
	 * @param message - the message to send
	 * @param timeout - the time to wait until return
	 */
	protected void send(AbstractMessageRouter router, Message<?> message, long timeout) {
		router.setSendTimeout(timeout);
		router.handleMessage(message);
	}

	/**
	 * Send m message on the default subscriber channel
	 * {@link Actors#DEFAULT_CHANNEL_NAME}
	 * @param message - the message to send
	 * @return true if successful
	 */
	@Override
	public boolean send(Message<?> message) {
		return send(Actors.DEFAULT_CHANNEL_NAME, message);
	}

	/**
	 * Send a message on the default subscriber channel
	 * {@link Actors#DEFAULT_CHANNEL_NAME}
	 * @param message - the message to send
	 * @param timeout - the time to wait until return
	 * @return true if successful
	 */
	@Override
	public boolean send(Message<?> message, long timeout) {
		return send(Actors.DEFAULT_CHANNEL_NAME, message, timeout);
	}

	/**
	 * Send a message on a custom channel name on a pollable
	 * channel
	 * @param channelName - the name of the channel
	 * @param message - the message to send
	 * @return true if successful
	 */
	protected boolean put(String channelName, Message<?> message) {
		AbstractPollableChannel channel = this.actors.getPollableChannel(channelName);
		if (channel != null)
			return channel.send(message);
		return false;
	}

	/**
	 * Send a message on a custom channel name on a pollable
	 * channel
	 * @param channelName - the name of the channel
	 * @param message - the message to send
	 * @param timeout - the time to wait until return
	 * @return true if successful
	 */
	protected boolean put(String channelName, Message<?> message, long timeout) {
		AbstractPollableChannel channel = this.actors.getPollableChannel(channelName);
		if (channel != null)
			return channel.send(message, timeout);
		return false;
	}

	/**
	 * Send a message on the default channel name {@link Actors#DEFAULT_CHANNEL_NAME}
	 * on a pollable channel
	 * @param channelName - the name of the channel
	 * @param message - the message to send
	 * @return true if successful
	 */
	protected boolean put(Message<?> message) {
		return put(Actors.DEFAULT_CHANNEL_NAME, message );
	}

	/**
	 * Send a message on the default channel name {@link Actors#DEFAULT_CHANNEL_NAME}
	 * on a pollable channel
	 * @param channelName - the name of the channel
	 * @param message - the message to send
	 * @param timeout - the time to wait until return
	 * @return true if successful
	 */
	protected boolean put(Message<?> message, long timeout) {
		return put(Actors.DEFAULT_CHANNEL_NAME, message, timeout);
	}

	/**
	 * Poll the message from the queue for a custom channel name
	 *
	 * @param channelName - the name of the channel
	 * @param timeout - the time to wait until return
	 * @return the message from the queue
	 */
	@Nullable
	protected Message<?> receive(String channelName, long timeout) {
		AbstractPollableChannel channel = this.actors.getPollableChannel(channelName);
		if (channel != null) {
			if (!(this instanceof PollableHandler)) {
				throw new IllegalStateException("Actor must implement " + PollableHandler.class.getName());
			}
			return channel.receive(timeout);
		}
		return null;
	}

	/**
	 * Poll the message from the queue for a custom channel name
	 *
	 * @param channelName - the name of the channel
	 * @return the message from the queue
	 */
	@Nullable
	protected Message<?> receive(String channelName) {
		return receive(channelName, -1);
	}

	/**
	 * Poll the message from the default queue channel
	 * {@link Actors#DEFAULT_CHANNEL_NAME}
	 *
	 * @param channelName - the name of the channel
	 * @param timeout - the time to wait until return
	 * @return the message from the queue
	 */
	@Override
	public Message<?> receive(long timeout) {
		return receive(Actors.DEFAULT_CHANNEL_NAME, timeout);
	}

	/**
	 * Poll the message from the default queue channel
	 * {@link Actors#DEFAULT_CHANNEL_NAME}
	 *
	 * @param channelName - the name of the channel
	 * @return the message from the queue
	 */
	@Override
	public Message<?> receive() {
		return receive(Actors.DEFAULT_CHANNEL_NAME);
	}

	/**
	 * Flush any remaining pending work
	 */
	@Override
	public void flush() throws IOException {
		//no flusing
	}

}
