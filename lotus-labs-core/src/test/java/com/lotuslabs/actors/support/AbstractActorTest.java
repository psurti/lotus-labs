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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lotuslabs.log.Log4J2;

/**
 * @author psurti
 *
 */
public class AbstractActorTest {
	static {
		Log4J2.init();
	}
	private static final Logger logger = LoggerFactory.getLogger(AbstractActorTest.class);

	private final Director director = new Director();
	private final QueueConsumerActor act1 = new QueueConsumerActor(director);
	private final QueueConsumerActor act2 = new QueueConsumerActor(director);
	private final QueueProducerActor act3 = new QueueProducerActor(director); // Important last on the list


	public AbstractActorTest() {
		super();

	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		act1.setNThreads(1);
		act2.setNThreads(1);
		act3.setNThreads(1);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		System.in.read();
		director.stopAll(act1,act2,act3);
	}

	@Test
	public void test_TwoPollableChannels() {
		director.startAll(act1,act2,act3);
		logger.info("Started All Actors - #pollableHandlers:{}", director.getPollableHandlers().size());
	}

}
