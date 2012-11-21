
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import chittoda.CacheKeyIntf;
import chittoda.AbstractThreadPool;

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
	private final int size;
	
	//For round robin scheduling
	private Integer indexer = -1;
	
	//index caching enabled, for faster task assignment
	private final boolean isKeyIndexCacheEnabled;
	
	//Cache to map Key and Index assigned
	private ConcurrentHashMap<Object, Integer> keyCache; 
	
	//BlockingQueue factory
	private final BlockingQueueFactory queueFactory;

	//Task queue array
	//We could have used Map to store queue, but direct index access would be faster
	private final BlockingQueue<Runnable>[] queueArray;
	
	/**
	 * 
	 * @param size
	 * @param queueFactory
	 * @param isKeyIndexCacheEnabled
	 */
	@SuppressWarnings("unchecked")
	public FSMThreadPool(int size, BlockingQueueFactory queueFactory
			, boolean isKeyIndexCacheEnabled) {
		
		this.size = size;
		this.queueFactory = queueFactory;
		this.isKeyIndexCacheEnabled = isKeyIndexCacheEnabled;
		
		queueArray = new BlockingQueue[size];
		init();
	}
	
	/**
	 * Initialise queue and Threads
	 */
	private void init(){
		for (int i = 0; i <= size-1; i++) {
			queueArray[i] = queueFactory.createBlockingQueue();
			Thread thr = new Thread(new Worker(queueArray[i]));
			thr.start();
		}
		
		if(isKeyIndexCacheEnabled)
			keyCache = new ConcurrentHashMap<Object, Integer>();
	}
	
	/**
	 * Assign the Runnable Task to ThreadPool.
	 * @param key Key to maintain the sequence of tasks
	 * @param task Task that is to be executed
	 */
	public Integer assignTask(CacheKeyIntf<? extends Object> key, Runnable task){
		
		
		Integer index = keyCache.get(key.getKey());
		
		//When index is not assigned to Key
		if(index == null)
		{
			//Get round robin index for queue to be used
			index = getRoundRobinIndex();
			keyCache.put(key.getKey(), index);
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
		if(size == 1)
			return 0;
		
		synchronized (indexer) {
			if(indexer >= size-1)
				indexer = -1;
			
			indexer++;
			index = indexer;
		}
		
		return index;
	}
	
}
