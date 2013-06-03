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

import org.junit.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static junit.framework.Assert.*;

/**
 * @author Heinz Kabutz
 * @author Jitendra Chittoda
 */
public class StripedExecutorServiceTest {
	
	private static AtomicInteger stripeAtomicInteger = new AtomicInteger();
	
    @Before
    public void initialize() {
        TestRunnable.outOfSequence = false;
                //TestUnstripedRunnable.outOfSequence =
//                        TestFastRunnable.outOfSequence = false;
    }

    @Test
    public void testSingleStripeRunnable() throws InterruptedException {
        //ExecutorService pool = new StripedExecutorService();
        FSMThreadPool threadPool = new FSMThreadPool(10, new BlockingQueueFacImpl(), true);
        String stripe = "stripe";
        AtomicInteger actual = new AtomicInteger(0);
        for (int i = 0; i < 100; i++) {
        	TestRunnable r = new TestRunnable(stripe, actual, i);
        	threadPool.assignTask(r);
        }
//        assertFalse(pool.isTerminated());
//        assertFalse(pool.isShutdown());
        threadPool.shutdown();
        assertTrue(threadPool.awaitTermination(1, TimeUnit.HOURS));
        
        assertFalse("Expected no out-of-sequence runnables to execute",
                TestRunnable.outOfSequence);
//        assertTrue(pool.isTerminated());
    }

    @Test
    public void testShutdown() throws InterruptedException {
        ThreadGroup group = new ThreadGroup("stripetestgroup");
        Thread starter = new Thread(group, "starter") {
            public void run() {
                //ExecutorService pool = new StripedExecutorService();
            	FSMThreadPool pool = new FSMThreadPool(10, new BlockingQueueFacImpl(), true);
                String stripe = "stripe";
                AtomicInteger actual = new AtomicInteger(0);
                for (int i = 0; i < 100; i++) {
                	TestRunnable r = new TestRunnable(stripe, actual, i);
                    pool.assignTask(r);
                }
                pool.shutdown();
            }
        };
        starter.start();
        starter.join();

        for (int i = 0; i < 100; i++) {
            if (group.activeCount() == 0) return;
            Thread.sleep(100);
        }

        assertEquals(0, group.activeCount());
    }

    @Test
    public void testShutdownNow() throws InterruptedException {
    	FSMThreadPool pool = new FSMThreadPool(10, new BlockingQueueFacImpl(), true);
        String stripe = "stripe";
        AtomicInteger actual = new AtomicInteger(0);
        for (int i = 0; i < 100; i++) {
            pool.assignTask(new TestRunnable(stripe, actual, i));
        }
        //Thread.sleep(500);
        //assertFalse(pool.isTerminated());
        Collection<Runnable> unfinishedJobs = pool.shutdownNow();

        //assertTrue(pool.isShutdown());
        assertTrue(pool.awaitTermination(1, TimeUnit.MINUTES));
        //assertTrue(pool.isTerminated());

        assertTrue(unfinishedJobs.size() > 0);

        assertEquals(100, unfinishedJobs.size() + actual.intValue());
    }

//    @Test
//    public void testSingleStripeCallableWithCompletionService() throws InterruptedException, ExecutionException {
//        ExecutorService pool = new StripedExecutorService();
//        final CompletionService<Integer> cs = new ExecutorCompletionService<>(
//                pool
//        );
//
//        Thread testSubmitter = new Thread("TestSubmitter") {
//            public void run() {
//                Object stripe = new Object();
//                for (int i = 0; i < 50; i++) {
//                    cs.submit(new TestCallable(stripe, i));
//                }
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    interrupt();
//                }
//                for (int i = 50; i < 100; i++) {
//                    cs.submit(new TestCallable(stripe, i));
//                }
//            }
//        };
//        testSubmitter.start();
//
//        for (int i = 0; i < 100; i++) {
//            int actual = cs.take().get().intValue();
//            System.out.println("Retrieved " + actual);
//            assertEquals(i, actual);
//        }
//        pool.shutdown();
//        assertTrue(pool.awaitTermination(1, TimeUnit.HOURS));
//        testSubmitter.join();
//    }

//    @Test
//    public void testUnstripedRunnable() throws InterruptedException {
//        ExecutorService pool = new StripedExecutorService();
//        AtomicInteger actual = new AtomicInteger(0);
//        for (int i = 0; i < 100; i++) {
//            pool.submit(new TestUnstripedRunnable(actual, i));
//        }
//        pool.shutdown();
//        assertTrue(pool.awaitTermination(1, TimeUnit.HOURS));
//
//        assertTrue("Expected at least some out-of-sequence runnables to execute",
//                TestUnstripedRunnable.outOfSequence);
//    }

    @Test
    public void testMultipleStripes() throws InterruptedException {
    	final FSMThreadPool pool = new FSMThreadPool(10, new BlockingQueueFacImpl(), true);
        ExecutorService producerPool = Executors.newCachedThreadPool();
        stripeAtomicInteger.set(0);
        for (int i = 0; i < 20; i++) {
            producerPool.submit(new Runnable() {
                public void run() {
                    String stripe = "stripe-" + stripeAtomicInteger.getAndIncrement();
                    AtomicInteger actual = new AtomicInteger(0);
                    for (int i = 0; i < 100; i++) {
                    	TestRunnable r = new TestRunnable(stripe, actual, i);
                        pool.assignTask(r);
                    }
                }
            });
        }
        producerPool.shutdown();

        while (!producerPool.awaitTermination(1, TimeUnit.MINUTES)) ;

        pool.shutdown();
        assertTrue(pool.awaitTermination(1, TimeUnit.DAYS));
        
        assertFalse("Expected no out-of-sequence runnables to execute",
                TestRunnable.outOfSequence);
    }


    @Test
    public void testMultipleFastStripes() throws InterruptedException {
    	final FSMThreadPool pool = new FSMThreadPool(10, new BlockingQueueFacImpl(), true);
        ExecutorService producerPool = Executors.newCachedThreadPool();
        stripeAtomicInteger.set(0);
        for (int i = 0; i < 20; i++) {
            producerPool.submit(new Runnable() {
                public void run() {
                	String stripe = "stripe-" + stripeAtomicInteger.getAndIncrement();
                    AtomicInteger actual = new AtomicInteger(0);
                    for (int i = 0; i < 100; i++) {
                    	TestFastRunnable r = new TestFastRunnable(stripe, actual, i);
                        pool.assignTask(r);
                    }
                }
            });
        }
        producerPool.shutdown();

        while (!producerPool.awaitTermination(1, TimeUnit.MINUTES)) ;

        
        pool.shutdown();
        assertTrue(pool.awaitTermination(1, TimeUnit.DAYS));
        assertFalse("Expected no out-of-sequence runnables to execute",
                TestFastRunnable.outOfSequence);
    }


    public static class TestRunnable implements CacheKeyRunnable<Object> {
        private final Object stripe;
        private final AtomicInteger stripeSequence;
        private final int expected;
        private static volatile boolean outOfSequence = false;

        public TestRunnable(Object stripe, AtomicInteger stripeSequence, int expected) {
            this.stripe = stripe;
            this.stripeSequence = stripeSequence;
            this.expected = expected;
        }

		@Override
		public Object getKey() {			
			return stripe;
		}

        public void run() {
            try {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                Thread.sleep(rand.nextInt(10) + 10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int actual = stripeSequence.getAndIncrement();
            if (actual != expected) {
                outOfSequence = true;
            }
            System.out.printf("Execute strip %h %d %d%n", stripe, actual, expected);
            assertEquals("out of sequence", actual, expected);
        }

    }

    public static class TestFastRunnable implements CacheKeyRunnable<Object> {
        private final Object stripe;
        private final AtomicInteger stripeSequence;
        private final int expected;
        private static volatile boolean outOfSequence = false;

        public TestFastRunnable(Object stripe, AtomicInteger stripeSequence, int expected) {
            this.stripe = stripe;
            this.stripeSequence = stripeSequence;
            this.expected = expected;
        }

        public Object getKey() {
            return stripe;
        }

        public void run() {
            int actual = stripeSequence.getAndIncrement();
            if (actual != expected) {
                outOfSequence = true;
            }
            System.out.printf("Execute strip %h %d %d%n", stripe, actual, expected);
            assertEquals("out of sequence", actual, expected);
        }
    }

//    public static class TestCallable implements StripedCallable<Integer> {
//        private final Object stripe;
//        private final int expected;
//
//        public TestCallable(Object stripe, int expected) {
//            this.stripe = stripe;
//            this.expected = expected;
//        }
//
//        public Object getStripe() {
//            return stripe;
//        }
//
//        public Integer call() throws Exception {
//            try {
//                ThreadLocalRandom rand = ThreadLocalRandom.current();
//                Thread.sleep(rand.nextInt(10) + 10);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//            return expected;
//        }
//    }

//    public static class TestUnstripedRunnable implements Runnable {
//        private final AtomicInteger stripeSequence;
//        private final int expected;
//        private static volatile boolean outOfSequence = false;
//
//        public TestUnstripedRunnable(AtomicInteger stripeSequence, int expected) {
//            this.stripeSequence = stripeSequence;
//            this.expected = expected;
//        }
//
//        public void run() {
//            try {
//                ThreadLocalRandom rand = ThreadLocalRandom.current();
//                Thread.sleep(rand.nextInt(10) + 10);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//            int actual = stripeSequence.getAndIncrement();
//            if (actual != expected) {
//                outOfSequence = true;
//            }
//            System.out.println("Execute unstriped " + actual + ", " + expected);
//        }
//    }


}
