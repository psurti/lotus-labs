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

package com.lotuslabs.actors.actions;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.context.Lifecycle;

/**
 * @author psurti
 *
 */
public abstract class Action<I,O> implements Actionable<I, O>, Lifecycle
{

	@Override
	public Collection<O> execute(Collection<I> items) {
		Collection<O> ret = null;
		if (items == null) {
			execute((I)null);
		} else {
			ret = new ArrayList<>();
			for (I item : items) {
				ret.add(execute(item));
			}
		}
		return ret;
	}
}
