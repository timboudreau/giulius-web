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

import com.mastfrog.util.perf.Benchmark;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import static com.mastfrog.settings.SettingsBuilder.DEFAULT_NAMESPACE;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class App {

    private App() {
    }

    public static void main(String[] args) throws IOException {
        Settings settings = SettingsBuilder.createDefault().
                add("foo", "bar").buildMutableSettings();

        Dependencies deps = Dependencies.builder()
                .add(settings, DEFAULT_NAMESPACE)
                .add(new JmxAopModule(settings)).build();

        ExecutorService svc = Executors.newCachedThreadPool();
        for (int i = 0; i < 33; i++) {
            Thingy thing = deps.getInstance(Thingy.class);
            svc.submit(thing);
        }
    }

    public static class Thingy implements Runnable {

        private static int ct;
        private int id = ct++;

        @Override
        public void run() {
            System.out.println("Go " + id);
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                doStuff();
            }
        }

        private final Random r = new Random(123);

        int foo;

        @Benchmark("doStuff")
        public void doStuff() {
            foo++;
            try {
                Thread.sleep(r.nextInt(20));
            } catch (InterruptedException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
