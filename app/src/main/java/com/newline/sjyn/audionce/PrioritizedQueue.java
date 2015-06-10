package com.newline.sjyn.audionce;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * public class PrioritizedQueue
 *
 * A simple implementation of a priority queue.
 *
 * @param <T> The type of element stored in this queue. It must
 *            extend the abstract class Prioritized so that the queue can
 *            perform the dequeueByPriority method.
 */
public class PrioritizedQueue<T extends Prioritized> {

    /**
     * The list representation of this queue
     */
    private ArrayList<T> representation;

    /**
     * public PrioritizedQueue()
     * <p/>
     * A default constructor for this queue
     */
    public PrioritizedQueue(){
        representation = new ArrayList<>();
    }

    /**
     * public T dequeue
     *
     * Dequeues from the que in the standard way.
     *
     * This operation ignores the priority of the element, and removes and
     * returns the head of the queue. If the queue is empty, an exception will
     * be raised
     *
     * @return T   The element at the head of this queue
     */
    public T dequeue(){
        T elem = representation.get(0);
        representation.remove(0);
        return elem;
    }

    /**
     * public void enqueue(T element)
     *
     * Inserts the provided element into the back of the queue
     *
     * @param element   The element to be inserted into the queue
     */
    public void enqueue(T element){
        representation.add(element);
    }

    /**
     * public T dequeueByPriority()
     *
     * Returns and removes the highest priority item from the queue.
     *
     * Note that if the queue is empty, an exception will be thrown.
     *
     * @return T   The element with the highest priority in the queue
     */
    public T dequeueByPriority(){
        T ret = representation.get(0);
        for(T elem : representation){
            if(elem.getPriority() > ret.getPriority())
                ret = elem;
        }
        representation.remove(ret);
        return ret;
    }

    /**
     * public T reverseDequeueByPriority()
     * <p/>
     * Returns and removes the element with the highest priority in this queue.
     * <p/>
     * Note that if the queue is empty, an exception will be thrown.
     *
     * @return T   The element with the lowest priority in the queue
     */
    public T reverseDequeueByPriority() {
        T ret = representation.get(0);
        for (T elem : representation) {
            if (elem.getPriority() < ret.getPriority())
                ret = elem;
        }
        representation.remove(ret);
        return ret;
    }

    /**
     * public boolean isEmpty()
     *
     * Determines if this queue is empty
     *
     * @return boolean     True if the queue is empty, false otherwise
     */
    public boolean isEmpty(){
        return representation.isEmpty();
    }

    /**
     * public boolean contains(T element)
     *
     * Determines if this queue contains the provided element.
     *
     * It is possible for the queue to contain duplicate values.
     *
     * @param element   The element for which to search.
     *
     * @return boolean     True if the queue contains the provided element,
     *                          false otherwise.
     */
    public boolean contains(T element){
        return representation.contains(element);
    }

    /**
     * public void clearAllBut(T... elementsToKeep)
     *
     * Clears all the elements in the queue except for the provided varargs.
     *
     * Note that the queue must contain all the provided elements.
     *
     * @param elementsToKeep    The elements to retain in the queue
     */
    public void clearAllBut(T... elementsToKeep){
        representation.retainAll(Arrays.asList(elementsToKeep));
    }

    /**
     * public void clear()
     *
     * Empties the queue of all elements.
     */
    public void clear(){
        representation.clear();
    }
}
