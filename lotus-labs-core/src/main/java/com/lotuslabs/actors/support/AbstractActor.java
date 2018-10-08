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
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.channel.AbstractPollableChannel;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.support.MutableMessage;
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
 * with pollaable and pub-sub channels
 *
 * An actor that consumes events via subscription implments
 * {@link MessageHandler} while one that consumes events via
 * a polling mechanism (calls {@link AbstractActor#receive()}
 * <bold>must</bold> implment {@link PollableHandler}
 *
 * @author psurti
 *
 */
public abstract class AbstractActor<I,O> extends Action<I,O>
implements Flushable, PollableChannel, SubscribableChannel {

	private static final Logger logger = LoggerFactory.getLogger(AbstractActor.class);

	private boolean isRunning = false;
	private AbstractSubscribableChannel subscriberChannel;
	private AbstractPollableChannel pollableChannel;
	private ExecutorService executor;
	private int nThreads;


	static class PollReadyEvent extends GenericMessage<Object> {
		private static final long serialVersionUID = 4304015635624712083L;
		public PollReadyEvent() {
			super(new Object());
		}
	}
	public static final PollReadyEvent POLL_READY_EVENT = new PollReadyEvent();
	/*
	 * End-Of-Stream Event
	 */
	public static class EOSEvent extends MutableMessage<Object> {
		public static final String POISSON_PILL = "POISSON_PILL";
		private static final long serialVersionUID = 4304015635624712083L;
		public EOSEvent() {
			super(new Object());
			this.getHeaders().put(POISSON_PILL, new Object());
		}
	}
	public static final EOSEvent EOS_EVENT = new EOSEvent();


	public AbstractActor(Director director) {
		director.checkActor(this);
		nThreads = 1;
	}

	public void setNThreads(int nThreads) {
		this.nThreads = nThreads;
	}

	public int getNThreads() {
		return this.nThreads;
	}

	public void init(Director director) {
		this.subscriberChannel = director.getPublishScubscriberChannel();
		this.pollableChannel = (director.getPollableHandlers().isEmpty()) ? null : director.getDefaultPollableChannel();
		final String threadName = getClass().getSimpleName();
		if (nThreads > 0) {
			this.executor = Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread thr = new Thread(r);
					thr.setName( threadName + thr.getName() );
					return thr;
				}
			});
		}
	}

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


	protected void invoke(Runnable r) {
		if (this.nThreads == 0) {
			r.run();
			return;
		}
		if (this.executor != null) {
			this.executor.execute(r);
		}
	}

	public boolean put(Message<?> message) {
		if (pollableChannel != null)
			return pollableChannel.send(message);
		return false;
	}

	public boolean put(Message<?> message, long timeout) {
		if (pollableChannel != null)
			return pollableChannel.send(message, timeout);
		return false;
	}


	@Override
	public boolean send(Message<?> message) {
		if (subscriberChannel != null)
			return subscriberChannel.send(message);
		return false;
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		if (subscriberChannel != null)
			return subscriberChannel.send(message, timeout);
		return false;
	}

	@Override
	public void start() {
		logger.info("Started {}", getClass().getName());
	}

	@Override
	public void stop() {
		logger.info("Attempting to stop {}" , getClass().getName());
		send(EOS_EVENT);
		if (this.executor != null) {
			this.executor.shutdownNow();
		}
		logger.info("Stopped {} {}",  getClass().getName(), isRunning);
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		if (subscriberChannel != null)
			return subscriberChannel.subscribe(handler);
		return false;
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		if (subscriberChannel != null)
			return subscriberChannel.unsubscribe(handler);
		return false;
	}

	@Override
	public Message<?> receive(long timeout) {
		if (pollableChannel != null) {
			if (!(this instanceof PollableHandler)) {
				throw new IllegalStateException("Actor must implement " + PollableHandler.class.getName());
			}
			return pollableChannel.receive(timeout);
		}
		return null;
	}

	@Override
	public Message<?> receive() {
		return receive(-1);
	}

	@Override
	public void flush() throws IOException {
		//no flusing
	}

}
