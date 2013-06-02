
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * FSMThreadPool which will maintain the execution order of Task
 * as per their insertion order in ThreadPool.
 * Tasks would be sequentialise based on the Key provided while assigning
 * the Task
 * @author Jitendra Chittoda
 *
 */
public class FSMThreadPool extends AbstractThreadPool {

	//Number of threads to start
	private final int SIZE;
	
	//For round robin scheduling
	private Integer indexer = -1;
	
	//index caching enabled, for faster task assignment
	private final boolean isKeyIndexCacheEnabled;
	
	//Cache to map Key and Index assigned
	private IdentityHashMap<Object, Integer> keyCache; 
	
	//BlockingQueue factory
	private final BlockingQueueFactory queueFactory;

	//Task queue array
	//We could have used Map to store queue, but direct index access would be faster
	private final BlockingQueue<Runnable>[] queueArray;
	
	private final ReentrantLock lock = new ReentrantLock();
	
	private final Condition shutdownCondition = lock.newCondition();
	
	private final List<Worker> workers = new CopyOnWriteArrayList<>(); 
	
	/**
	 * 
	 * @param size
	 * @param queueFactory
	 * @param isKeyIndexCacheEnabled
	 */
	@SuppressWarnings("unchecked")
	public FSMThreadPool(int size, BlockingQueueFactory queueFactory
			, boolean isKeyIndexCacheEnabled) {
		
		this.SIZE = size;
		this.queueFactory = queueFactory;
		this.isKeyIndexCacheEnabled = isKeyIndexCacheEnabled;
		
		queueArray = new BlockingQueue[size];
		init();
	}
	
	/**
	 * Initialise queue and Threads
	 */
	private void init(){
		for (int i = 0; i <= SIZE-1; i++) {
			queueArray[i] = queueFactory.createBlockingQueue();
			Worker worker = new Worker(queueArray[i]);
			workers.add(worker);
			Thread thr = new Thread(worker);
			thr.start();			
		}
		
		if(isKeyIndexCacheEnabled)
			keyCache = new IdentityHashMap<>();
	}
	
	/**
	 * Assign the Runnable Task to ThreadPool.
	 * @param key Key to maintain the sequence of tasks
	 * @param task Task that is to be executed
	 */
	public Integer assignTask(CacheKeyRunnable<? extends Object> task){
		
		
		Integer index = keyCache.get(task.getKey());
		
		//When index is not assigned to Key
		if(index == null)
		{
			//Get round robin index for queue to be used
			index = getRoundRobinIndex();
			keyCache.put(task.getKey(), index);
		}
			
		
		try {
			//queue of that index would be used to assign the task
			queueArray[index].put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Return the index, if uesr directly want to assign the 
		//taks to queue
		return index;
	}
	
	/**
	 * Get Round robin Index of  queue.
	 * @return
	 */
	private int getRoundRobinIndex(){
		
		int index;
		
		//When size is 1, directly return 0 index 
		if(SIZE == 1)
			return 0;
		
		lock.lock();
	
		if(indexer >= SIZE-1)
			indexer = -1;
		
		indexer++;
		index = indexer;
		
		lock.unlock();
		
		return index;
	}

	@Override
	public void shutdown() {
		
		for (Worker worker : workers) {
			worker.shutdown(new Worker.ShutdownListener() {				
				@Override
				public void shutdownComplete(Worker worker) {					
					workers.remove(worker);
					if(workers.size() == 0){
						lock.lock();
						try{ shutdownCondition.signal(); }
						finally{ lock.unlock(); }
					}
				}
			});
			
		}
		
	}
	
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        lock.lock();
        try{
	        
	        long waitUntil = System.nanoTime() + unit.toNanos(timeout);
	        long remainingTime;
	        while ((remainingTime = waitUntil - System.nanoTime()) > 0
	                && !workers.isEmpty()) {
	            shutdownCondition.awaitNanos(remainingTime);
	        }
	        if (remainingTime <= 0) return false;
        } finally {
        	lock.unlock();
        }
        
        return true;        
    }


    //TODO
	@Override
	public void shutdownNow() {
		// TODO Auto-generated method stub
		
	}
	
	
}
