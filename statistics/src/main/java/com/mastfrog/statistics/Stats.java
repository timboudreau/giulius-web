/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.statistics;

import com.mastfrog.util.thread.AtomicMaximum;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Tim Boudreau
 */
class Stats implements StatsMBean {
    String name;
    AtomicInteger count = new AtomicInteger();
    AtomicInteger contention = new AtomicInteger();
    AtomicMaximum longestTime = new AtomicMaximum();
    AtomicLong total = new AtomicLong();

    public void reset() {
        count.set(0);
        contention.set(0);
        longestTime.reset();
        total.set(0);
    }
    
    @Override
    public int getInvocationCount() {
        return count.get();
    }

    @Override
    public long getTotalTimeSpent() {
        return total.get();
    }

    @Override
    public void setInvocationCount(int ct) {
        count.lazySet(ct);
    }

    @Override
    public long getAverageMilliseconds() {
        return total.get() / (long) count.get();
    }

    @Override
    public long getLongestInvocationMilliseconds() {
        return longestTime.getMaximum();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCurrentContention() {
        return contention.get();
    }
}
