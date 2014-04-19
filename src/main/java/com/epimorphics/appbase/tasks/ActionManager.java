/******************************************************************
 * File:        ActionManager.java
 * Created by:  Dave Reynolds
 * Created on:  18 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.epimorphics.appbase.core.TimerManager;
import com.epimorphics.appbase.monitor.ConfigMonitor;
import com.epimorphics.tasks.SimpleProgressMonitor;
import com.epimorphics.tasks.TaskState;

/**
 * Controller which tracks available actions, executes actions
 * and tracks ongoing and recently completed executions.
 * Monitors a configurable directory - see ConfigMonitor for configuration options.
 */
public class ActionManager extends ConfigMonitor<Action> {
    protected static final int DEFAULT_HISTORY_SIZE = 500;
    private static final int MAX_THREADS = 10;
    private static final int CORE_THREADS = 2;
    private static final int QUEUE_DEPTH = 5;
    private static final int KEEPALIVE = 1000;
    
    // Table of available actions provided by super class
    
    protected Set<ActionExecution> currentExecutions = new HashSet<>();
    protected Map<String, ActionExecution> executionIndex = new HashMap<String, ActionExecution>();
    protected Deque<ActionExecution> executionHistory = new ArrayDeque<>(DEFAULT_HISTORY_SIZE);

    protected ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, KEEPALIVE, TimeUnit.MILLISECONDS, 
            new ArrayBlockingQueue<Runnable>(QUEUE_DEPTH));
    
    // Configuration options beyond base ConfigMonitor
    protected int maxHistory = DEFAULT_HISTORY_SIZE;
    
    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }
    
    @Override
    protected Collection<Action> configure(File file) {
        return ActionFactory.configure(file);
    }

    protected synchronized void recordExecution(ActionExecution ae) {
        currentExecutions.add(ae);
        executionIndex.put(ae.getId(), ae);
        executionHistory.addLast(ae);
        if (executionHistory.size() > maxHistory) {
            ActionExecution discard = executionHistory.removeFirst();
            executionIndex.remove( discard.getId() );
        }
    }
    
    protected synchronized void recordEndOfExecution(ActionExecution ae) {
        currentExecutions.remove(ae);
    }

    /**
     * Return an identified execution, may no longer be active.
     */
    public synchronized ActionExecution getExecution(String id) {
        return executionIndex.get(id);
    }

    /**
     * Return all executions that are still active.
     */
    public synchronized Collection<ActionExecution> listActiveExecutions() {
        return new ArrayList<>( currentExecutions );
    }
    
    /**
     * Start the action running as a background thread
     * @param action the action
     * @param parameters configuration and runtime parameters
     * @return a future which can be used to wait for the action to complete or timeout
     */
    public ActionExecution runAction(Action action, BindingEnv parameters) {
        ActionExecution ae = new ActionExecution(action, parameters);
        recordExecution(ae);
        ae.start();
        return ae;
    }

    /**
     * Start the action running as a background thread
     * @param actionName the name of an action registered with this manager
     * @param parameters configuration and runtime parameters
     * @return a future which can be used to wait for the action to complete or timeout
     */
    public ActionExecution runAction(String actionName, BindingEnv parameters) {
        return runAction( get(actionName),  parameters);
    }
    
    /**
     * Holds the state of the execution of an asynchronous action,
     * it may be still running or terminates.
     */
    public class ActionExecution implements Runnable {
        protected Action action;
        protected BindingEnv parameters;
        protected long startTime;
        protected long finishTime = 0;
        protected SimpleProgressMonitor monitor;
        protected String id = UUID.randomUUID().toString();
        protected Future<?> future;
        
        public ActionExecution(Action action, BindingEnv parameters) {
            this.parameters = parameters;
            monitor = new SimpleProgressMonitor();
            this.action = action;
        }
        
        public Action getAction() {
            return action;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getFinishTime() {
            return finishTime;
        }

        public SimpleProgressMonitor getMonitor() {
            return monitor;
        }

        public String getId() {
            return id;
        }
        
        public long getDuration() {
            if (finishTime > 0) {
                return finishTime - startTime;
            } else {
                return -1;
            }
        }
        
        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            startTimeout();
            try {
                action.run(parameters, monitor);
                if (monitor.getState() != TaskState.Terminated) {
                    monitor.setSucceeded();
                }
            } catch (Exception e) {
                condMarkTerminated("Exception: " + e);
            }
            finishTime = System.currentTimeMillis();
            recordEndOfExecution(this);
            condMarkTerminated("Thread died before completion, cause unknown");
        }
        
        private Future<?> start() {
            future = executor.submit(this);
            return future;
        }

        private void startTimeout() {
            int timeout = getAction().getTimeout();
            if (timeout != -1) {
                TimerManager.get().schedule(new Runnable() {
                    @Override  public void run() { 
                        timeout();
                    }
                }, timeout, TimeUnit.MILLISECONDS);
            }
        }
        
        /**
         * Cancel the execution, recording the given message on the progress monitor
         */
        public void cancel(String message) {
            if ( ! future.isDone() ) {
                future.cancel(true);
                condMarkTerminated(message);
            }
        }
        
        public void timeout() {
            cancel("Terminated due to timeout");
        }
        
        public void waitForCompletion() {
            try {
                future.get();
            } catch(Exception e) {
                // Ignore interruption exception
            }
        }
        
        protected void condMarkTerminated(String message) {
            SimpleProgressMonitor monitor = getMonitor();
            if (monitor.getState() != TaskState.Terminated) {
                monitor.report(message);
                monitor.failed();
            }
        }
        
    }
    
}
