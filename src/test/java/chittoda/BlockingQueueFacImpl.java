/*
 * Copyright (C) 2011-2012 Jitendra Chittoda
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. Jitendra Chittoda licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package chittoda;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import chittoda.BlockingQueueFactory;

/**
 * Blocking queue factory implementation
 * @author Jitendra Chittoda
 *
 */
public class BlockingQueueFacImpl implements BlockingQueueFactory {

	
	@Override
	public BlockingQueue<Runnable> createBlockingQueue() {
		//return new ArrayBlockingQueue<Runnable>(1024);
		return new LinkedBlockingQueue<>();
	}

	
}
