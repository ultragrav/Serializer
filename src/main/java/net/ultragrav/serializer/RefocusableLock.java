package net.ultragrav.serializer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RefocusableLock implements Lock {
    private final ReentrantLock current = new ReentrantLock(); // Only use if parent's parent in null.
    private final JsonMeta parent;

    public RefocusableLock(JsonMeta parent) {
        this.parent = parent;
    }

    public void lock() {
        while (true) {
            JsonMeta parentParent = parent.getParent();
            if (parentParent != null) {
                parentParent.getLock().lock();

                if (parent.getParent() == parentParent) {
                    break;
                }

                // Parent changed. retry.
                parentParent.getLock().unlock();

            } else {
                current.lock();
                if (parent.getParent() == null) {
                    break;
                }
                current.unlock();
            }
        }
    }

    public void unlock() {
        JsonMeta parentParent = parent.getParent();
        if (parentParent != null) {
            parentParent.getLock().unlock();
        } else {
            current.unlock();
        }
    }

    public Lock getCurrentLock() {
        JsonMeta parentParent = parent.getParent();
        if (parentParent != null) {
            return parentParent.getLock();
        } else {
            return current;
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }


//    public void lock() {
//        while (true) {
//            ReentrantLock c = current;
//            c.lock();
//            if (c == current) {
//                break;
//            }
//            c.unlock();
//        }
//    }
//
//    public void unlock() {
//        current.unlock();
//    }
//
//    public void setFocus(ReentrantLock focus) {
//        lock();
//        ReentrantLock old = this.current;
//        this.current = focus; // Our hold on the lock becomes invalid after this line executes.
//        old.unlock(); // Release our hold from the now invalid focus.
//    }
//
//    public void setFocus(RefocusableLock other) {
//        // Lock so that we know it's current focus will not change.
//        other.lock();
//        setFocus(other.current);
//        other.unlock();
//    }

}
