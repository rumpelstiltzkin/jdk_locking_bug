// Copyright (c) 2019 Hammerspace, Inc.
// 	  www.hammer.space
//
// Licensed under the Eclipse Public License - v 2.0 ("the License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.hammerspace.jdk.locking;

import java.util.Random;

/**
 * @author aganesh
 * @since 2019-10-21
 */
public class Main {

    private static final int NUM_READERS = 100;
    private static final int NUM_PRINT = 1_000_000_000;
    private static final Random RANDOM = new Random();
    private static final byte[] PRINTABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789".getBytes();

    private static final long A_REPRODUCER_SEED = 1571768802312L;

    public static void main(String args[]) {

        final Cache cache = new Cache();

        final long seed = System.currentTimeMillis();
        System.out.println("seed is " + seed);
        RANDOM.setSeed(A_REPRODUCER_SEED);

        // Spawn many threads that will update the ConcurrentHashMap
        Thread[] manyReadGrabbers = new Thread[NUM_READERS];
        System.out.println("Main spawning " + NUM_READERS + " readers");
        for (int i = 0; i < NUM_READERS; i++) {
            manyReadGrabbers[i] = new Thread(new ReadGrabbingRunnable(i, cache));
            manyReadGrabbers[i].setName("READER_" + i);
            manyReadGrabbers[i].start();
        }
        System.out.println("Main spawned " + NUM_READERS + " readers");

        try {
            Thread.sleep(1L * 1000);
        } catch (InterruptedException ie) {
            System.out.println("Main interrupted while sleeping for 10 seconds");
        }

        System.out.println("Main spawning a writer");
        Thread writeGrabber = new Thread(new WriteGrabbingRunnable(cache));
        writeGrabber.setName("WRITER");
        writeGrabber.start();
        System.out.println("Main spawned a writer");

        System.out.println("Main waiting for threads ...");
        try {
            writeGrabber.join();
        } catch (InterruptedException ie) {
            System.out.println("Main interrupted while join()ing writeGrabber");
            // ignore
        }

        for (int i = 0; i < NUM_READERS; i++) {
            try {
                manyReadGrabbers[i].join();
            } catch (InterruptedException ie) {
                System.out.println("Main interrupted while join()ing readGrabber[" + i + "]");
            }
        }
    }


    private static class ReadGrabbingRunnable implements Runnable {
        private final int id;
        private final Cache cache;

        ReadGrabbingRunnable(int id, Cache cache) {
            this.id = id;
            this.cache = cache;
        }

        private static String getNewRandomString(byte[] workspace) {
            for (int i = 0; i < workspace.length; i++) {
                workspace[i] = PRINTABLE[RANDOM.nextInt(PRINTABLE.length)];
            }
            return new String(workspace);
        }

        @Override
        public void run() {
            int count = 0;
            byte[] keyb = new byte[64];

            while (true) {
                final String key = getNewRandomString(keyb);
                Long newvalue = cache.update(key, (strKey, longVal) -> {
                    long now = System.nanoTime();
                    return now;
                });

                if (++count % NUM_PRINT == 0) {
                    System.out.println("Reader_" + id + " updated the " + count
                            + "th time; key=" + key + ", value=" + newvalue);
                }
            }
        }
    }

    private static class WriteGrabbingRunnable implements Runnable {
        private final Cache cache;

        WriteGrabbingRunnable(Cache cache) {
            this.cache = cache;
        }

        @Override
        public void run() {
            int count = 0;
            while (true) {
                Long time = cache.getTime();
                if (++count % NUM_PRINT == 0) { // print every million
                    System.out.println("Writer grabbed write lock " + count + " times");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    System.out.println("Writer interrupted while sleeping");
                }
            }
        }
    }
}


