/*
 * Copyright (C) 2000-2012 Heinz Max Kabutz

 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Heinz Max Kabutz licenses
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

import heinz.StripedRunnable;

import java.util.concurrent.*;

/**
 * 
 * @author Heinz Kabutz
 * @author Jitendra Chittoda
 *
 */
public class StripedThroughputTest {
    private static final int WORK = 1000;
    private static final int THREADS = 4;
    private static final int STRIPES = 128 / THREADS;
    private static final int REPEATS = 10_000_000 / WORK;
    private static final int DATAPOINTS = 10;


    public static void main(String[] args) throws InterruptedException {
        System.out.println("Fixed ExecutorService");
        for (int i = 0; i < DATAPOINTS; i++) {
            test(Executors.newFixedThreadPool(THREADS));
        }
        System.out.println("StripedExecutorService");
        for (int i = 0; i < DATAPOINTS; i++) {
            testMyImpl(new FSMThreadPool(THREADS, new BlockingQueueFacImpl(), true));
        }
    }

    private static void testMyImpl(FSMThreadPool pool) throws InterruptedException {
        Object[] stripes = new Object[STRIPES];
        for (int i = 0; i < stripes.length; i++) {
            stripes[i] = new Object();
        }

        long time = System.currentTimeMillis();
        for (int i = 0; i < REPEATS; i++) {
            for (final Object stripe : stripes) {
                pool.assignTask(new CacheKeyRunnable<Object>() {
                    public void run() {
                        work(ThreadLocalRandom.current().nextDouble());
                    }

                    @Override
					public Object getKey() {
                    	return stripe;
					}
					
                });
            }
        }
        pool.shutdown();
        while (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("Still waiting ...");
        }
        time = System.currentTimeMillis() - time;
        System.out.println("time = " + time);
    }
    
    private static void test(ExecutorService pool) throws InterruptedException {
        Object[] stripes = new Object[STRIPES];
        for (int i = 0; i < stripes.length; i++) {
            stripes[i] = new Object();
        }

        long time = System.currentTimeMillis();
        for (int i = 0; i < REPEATS; i++) {
            for (final Object stripe : stripes) {
                pool.submit(new CacheKeyRunnable<Object>() {
                    public void run() {
                        work(ThreadLocalRandom.current().nextDouble());
                    }

                    @Override
					public Object getKey() {
                    	return stripe;
					}
					
                });
            }
        }
        pool.shutdown();
        while (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            System.out.println("Still waiting ...");
        }
        time = System.currentTimeMillis() - time;
        System.out.println("time = " + time);
    }


    public static double work(double seed) {
        for (int i = 0; i < WORK; i++) {
            seed = Double.longBitsToDouble(
                    (Double.doubleToLongBits(seed) >> 8) ^ 0xffaabbcc);
        }
        return seed;
    }
}
