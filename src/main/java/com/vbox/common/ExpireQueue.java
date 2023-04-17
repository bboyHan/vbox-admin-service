package com.vbox.common;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpireQueue<E> {
    private final ConcurrentLinkedQueue<Node<E>> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void add(E element) {
        Node<E> node = new Node<>(element);
        queue.add(node);
        scheduler.schedule(() -> queue.remove(node), 60, TimeUnit.SECONDS);
        if (queue.size() > 60) {
            queue.poll();
        }
    }

    public E poll() {
        Node<E> node = queue.poll();
        return node == null ? null : node.element;
    }

    public int size() {
        return queue.size();
    }

    private static class Node<E> {
        final E element;
        final long expireTime;

        Node(E element) {
            this.element = element;
            this.expireTime = System.currentTimeMillis() + 60_000;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}