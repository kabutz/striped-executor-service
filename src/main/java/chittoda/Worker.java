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

import java.util.concurrent.BlockingQueue;

/**
 * Blocking worker that will keep on taking the Task from its queue.
 * Worker will block when there is no task in the queue.
 * @author Jitendra Chittoda
 *
 */
public class Worker implements Runnable {

	//Task Queue
	private final BlockingQueue<Runnable> taskQueue;
	
	public Worker(BlockingQueue<Runnable> taskQueue) {
		this.taskQueue = taskQueue;
	}
	
	@Override
	public void run() {
		
		while(true) {
			
			try {
				Runnable task = taskQueue.take();
				//Calling Runnable.run() as this Worker is already running
				//in a thread
				task.run();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}

	
}
