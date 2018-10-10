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

package com.lotuslabs.actors.example;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lotuslabs.actors.support.AbstractActor;
import com.lotuslabs.actors.support.Director;
import com.lotuslabs.log.Log4J2;

/**
 * @author psurti
 *
 */
public class Main {
	static {
		Log4J2.init();
	}
	private static final Logger logger = LoggerFactory.getLogger(Main.class);


	public static void main(String[] args) throws IOException {
		Director director = new Director();
		director.registerSubscriberChannel("foo");
		director.registerPollableChannel("bar");
		AbstractActor<?, ?>[] actors = new AbstractActor<?,?>[] {
			new TransformActor(director),
			new BatchIndexActor(director),
			new NRTIndexActor(director),
			new DBReadActor(director) // Important last on the list
		};
		logger.info("Start All Actors");
		director.startAll(actors);
		logger.info("Started All Actors - #pollableHandlers:{}", director.getPollableHandlers().size());
		logger.info("Waiting on input to EXIT");
		System.in.read();
		director.stopAll(actors);

	}

}
