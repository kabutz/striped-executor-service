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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import chittoda.FSMThreadPool;
import chittoda.CacheKeyIntf;
import static junit.framework.Assert.*;

/**
 * Tester app, to test the FSMThreadPool
 * @author Jitendra Chittoda
 *
 */
public class FSMThreadPoolTest {

	private static FSMThreadPool threadPool;
	private final int SIZE = 3;
	
	/**
	 * Initialize the FSMThreadPool and clients 
	 * 
	 */
	@Before
	public void init() {
		
		threadPool = new FSMThreadPool(SIZE, new BlockingQueueFacImpl(), true);
		
		for (int i = 0; i < SIZE; i++) {
			// Clients that will assign the tasks in FSMThreadPool
			Thread machine = new FSMMachine(new MyCacheKey("Key-" + (i+1)), 50);
			machine.start();
		}
		
		
	}
	
	/**
	 * FSMMachine is acting as a Task creator and assigner.
	 * This is acting as a client app 
	 * @author jitendra
	 *
	 */
	public class FSMMachine extends Thread{
		private final int size;
		private final MyCacheKey key;
		
		/**
		 * Machine created with parameters 
		 * @param key Key for doing the task sequencing
		 * @param events Number of events/tasks to be assigned/queued
		 */
		public FSMMachine(MyCacheKey key, int events) {
			this.key = key;
			this.size = events;
		}
		
		public void run() {
			AtomicInteger atomicInt = new AtomicInteger(0);
			for (int i = 0; i < size; i++) {			
				Task task = new Task(key, atomicInt, i);
				threadPool.assignTask(key, task);
			}
		}
	}
	
	/**
	 * A class that is a type of CacheKeyIntf
	 * So it should implement getKey().
	 * FSMThreadPool will get the key using getKey() method
	 * @author Jitendra Chittoda
	 *
	 */
	class MyCacheKey implements CacheKeyIntf<String> {

		String key = null;
		public MyCacheKey(String key) {
			this.key = key;
		}
		@Override
		public String getKey() {
			
			return key;
		}
		
	}
	
	/**
	 * Plain task that would simply log the statement 
	 * @author Jitendra Chittoda
	 *
	 */
	class Task implements Runnable
	{
		private final int event;
		private final MyCacheKey key;
		private final AtomicInteger atomicInt;
		public Task(MyCacheKey key, AtomicInteger expected, int event) {
			this.key = key;
			this.event = event;
			this.atomicInt = expected;
		}
		@Override
		public void run() {
			System.out.println("Thread["+Thread.currentThread().getId()+"] key[" + key.getKey() + "] expected[" + atomicInt.get() + "] got["+event+"]");
			int actual=atomicInt.getAndIncrement();
			assertEquals("Thread["+Thread.currentThread().getId()+"] expected[" + event + "] got["+actual+"]", event, actual);
		}
		
		public String toString(){
			return "k["+key.getKey()+"] e["+atomicInt.get()+"] a["+event+"]";
		}
	}
	
	@Test
	public void testFSM() {

		FSMThreadPoolTest tester = new FSMThreadPoolTest();
		tester.init();
		
		try {
			Thread.sleep(2000, 1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
