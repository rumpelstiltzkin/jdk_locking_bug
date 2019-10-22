package com.hammerspace.jdk.locking;

import javax.xml.ws.Holder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

/**
 * @author Anand Ganesh
 * @since 2019-10-21
 */
public class Cache {

    private static final long THRESHOLD_INCREMENT = 100;
    private static final long EVICT_THRESHOLD = 1_000_000;

    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Lock readLock = rwLock.readLock();
    private Lock writeLock = rwLock.writeLock();
    private ConcurrentMap<String, Long> theMap = new ConcurrentHashMap<>(); // String => timestamp
    private long threshold = THRESHOLD_INCREMENT;

    public Long update(String key, BiFunction<String, Long, Long> updateFunction) {
        Long newValue;
        Holder<Integer> lockCount = new Holder<>(0);
        try {
            newValue = theMap.compute(key, (strKey, currVal) -> {
                readLock.lock();
                // the lock might be acquired again if ConcurrentHashMap::compute() is applying my lambda again
                // internally. so count it and print a NOTE! below when this scenario is detected.
                lockCount.value++;
                return updateFunction.apply(key, currVal);
            });
        } finally {
            int count = lockCount.value;
            if (count > 1) {
                // this means our lambda was re-applied by the ConcurrentHashMap implementation?
                System.out.println("NOTE! lock acquired " + count + " times while processing cache update!");
            }
            while (count-- > 0) {
                // if locked, then unlock. The unlock is outside the compute to ensure the lock is released
                // only after the modification is visible in the map.
                readLock.unlock();
            }

        }

        return newValue;
    }

    public Long getTime() {
        long now, mapSize;
        writeLock.lock();
        now = System.nanoTime();
        mapSize = theMap.size();
        writeLock.unlock();

        if (mapSize > threshold) {
            System.out.println("cache size is " + mapSize);
            threshold += THRESHOLD_INCREMENT;
        }
        if (mapSize > EVICT_THRESHOLD) {
            long num_evictions = mapSize - EVICT_THRESHOLD;
            System.out.println("evicting " + num_evictions + " entries");
            for (String keyToEvict : theMap.keySet()) {
                if (num_evictions-- <= 0) {
                    break;
                }
                theMap.remove(keyToEvict);
            }
            System.out.println("evicted");
        }
        return now;
    }
}
