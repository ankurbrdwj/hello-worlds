package com.ankur.webcrawler.util;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class UniqueBlockingQueue<E> {

    private final BlockingQueue<E> queue = new LinkedBlockingQueue<>();
    private final Set<E> uniqueSet = ConcurrentHashMap.newKeySet();

    public void put(E e) throws InterruptedException {
        if (uniqueSet.add(e)) { // add returns false if element already present
            queue.put(e);
        }
    }

    public E take() throws InterruptedException {
        E e = queue.take();
        uniqueSet.remove(e); // allow re-adding in the future if needed
        return e;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public boolean contains(E e) {
        return uniqueSet.contains(e);
    }
    public void addAll(Collection<E> elements) throws InterruptedException {
        for (E e : elements) {
            put(e);
        }
    }
}
