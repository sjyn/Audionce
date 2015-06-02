package ai.com.audionce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private ArrayList<T> representation;

    public PrioritizedQueue(){
        representation = new ArrayList<>();
    }

    public T dequeue(){
        T elem = representation.get(0);
        representation.remove(0);
        return elem;
    }

    public void enqueue(T element){
        representation.add(element);
    }

    public T dequeueByPriority(){
        T ret = representation.get(0);
        for(T elem : representation){
            if(elem.getPriority() > ret.getPriority())
                ret = elem;
        }
        representation.remove(ret);
        return ret;
    }

    public boolean isEmpty(){
        return representation.isEmpty();
    }

    public boolean contains(T element){
        return representation.contains(element);
    }

    public void clearAllBut(T... elementsToKeep){
        representation.retainAll(Arrays.asList(elementsToKeep));
    }

    public void clear(){
        representation.clear();
    }
}
