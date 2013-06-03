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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Blocking worker that will keep on taking the Task from its queue.
 * Worker will block when there is no task in the queue.
 * @author Jitendra Chittoda
 *
 */
class Worker implements Runnable {

	//Task Queue
	private final BlockingQueue<Runnable> taskQueue;
	private boolean isStopped = false;
	private ShutdownListener shutdownListener = null;
	private Thread thread;
	
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
			catch(PoisonPillException | InterruptedException e){
				//ppe.printStackTrace();
				isStopped = true;
				shutdownListener.shutdownComplete(this);
				break;
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}
	
	public void shutdownNow(ShutdownListener shutdownListener){
		this.shutdownListener = shutdownListener;
		this.shutdownNow();
	}
	
	public void shutdownNow() {		
		this.thread.interrupt();		
	}

	public void shutdown(ShutdownListener shutdownListener){
		this.shutdownListener = shutdownListener;
		this.shutdown();
	}
	
	public void shutdown(){
		taskQueue.add(new Runnable() {			
			@Override
			public void run() {
				if (taskQueue.isEmpty()){
					shutdownNow();
				} else {
					shutdown();
				}
			}
		});
	}
	
	public void setThread(Thread thread) {
		this.thread = thread;
	}
	
	public List<Runnable> getUnfinishedTasks(){
		if(isWorkerStopped()){
			List<Runnable> unfinishedTasks = new ArrayList<>();
			taskQueue.drainTo(unfinishedTasks);
			return unfinishedTasks;
		} else {
			throw new IllegalStateException("Worker not stopped yet.");
		}
	}
	
	public boolean isWorkerStopped(){
		return isStopped;
	}
	
	public interface ShutdownListener {
		public void shutdownComplete(Worker worker);
	}
	
	private static class PoisonPillException extends RuntimeException {
		
	}
}
