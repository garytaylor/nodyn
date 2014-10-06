/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nodyn;

import io.netty.channel.EventLoopGroup;
import io.nodyn.loop.EventLoop;
import io.nodyn.runtime.Config;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.VertxInternal;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;

/**
 * @author Lance Ball
 */
public abstract class Nodyn {
    protected static final String NODE_JS = "node.js";
    protected static final String PROCESS = "nodyn/process.js";
    protected static final String ES6_POLYFILL = "nodyn/polyfill.js";
    protected static final String VERSION = "0.1.1-SNAPSHOT"; // TODO: This should come from pom.xml

    abstract public Object loadBinding(String name);

    abstract public void handleThrowable(Throwable t);

    protected abstract NodeProcess initialize();

    abstract protected Object runScript(String script);

    // The following methods are used in contextify.js
    abstract public Object getGlobalContext();

    abstract public void makeContext(Object global);

    abstract public boolean isContext(Object global);

    abstract public ScriptEngine getScriptEngine();

    public CompiledScript compile(String source, String fileName, boolean displayErrors) throws Throwable {
        try {
            if (getScriptEngine() instanceof Compilable) {
                return ((Compilable)getScriptEngine()).compile(source);
            }
        } catch (Throwable t) {
            if ( displayErrors ) {
                t.printStackTrace();
            }
            throw t;
        }
        return null;
    }



    protected Nodyn(Config config, Vertx vertx, boolean controlLifeCycle) {
        EventLoopGroup elg = ((VertxInternal) vertx).getEventLoopGroup();
        this.eventLoop = new EventLoop(elg, controlLifeCycle);
        this.vertx = vertx;
        this.config = config;
        this.completionHandler = new CompletionHandler();
    }

    public int run() throws Throwable {
        start();
        return await();
    }

    public Config getConfiguration() {
        return this.config;
    }

    public EventLoop getEventLoop() {
        return this.eventLoop;
    }

    public Vertx getVertx() {
        return this.vertx;
    }

    public void setExitHandler(ExitHandler handle) {
        this.exitHandler = handle;
    }

    public void reallyExit(int exitCode) {
        this.eventLoop.shutdown();
        if (this.exitHandler != null) {
            this.exitHandler.reallyExit(exitCode);
        } else {
            System.exit(exitCode);
        }
    }

    private int await() throws Throwable {
        this.eventLoop.await();

        if (this.completionHandler.error != null) {
            throw completionHandler.error;
        }

        if (this.completionHandler.process == null) {
            return -255;
        }

        return this.completionHandler.process.getExitCode();
    }

    private void start() {
        this.eventLoop.submitUserTask(new Runnable() {
            @Override
            public void run() {
                try {
                    Nodyn.this.completionHandler.process = initialize();
                } catch (Throwable t) {
                    Nodyn.this.completionHandler.error = t;
                }
            }
        }, "init");
    }

    private final EventLoop eventLoop;
    private final CompletionHandler completionHandler;
    private final Vertx vertx;
    private final Config config;
    private ExitHandler exitHandler;

    private static class CompletionHandler {
        public NodeProcess process;
        public Throwable error;
    }


}
