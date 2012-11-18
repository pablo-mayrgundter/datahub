/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.datahub;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A tasks servlet with helpers for constructing tasks.
 *
 * @author Pablo Mayrgundter <pmy@google.com>
 */
public class Tasks extends AbstractServlet {

  static final long serialVersionUID = 1894092056276869084L;

  static final String PARAM_PROC_ID = "processorId";
  static final String PARAM_ARGS = "arg";

  /**
   * The Processor class defines the operation to perform on task
   * callbacks with given args.
   */
  public abstract static class Processor {
    final String name;
    Processor(String name) {
      this.name = name;
    }
    abstract void process(String [] args);
  }

  /**
   * The ToStringIterator is a wrapper that defines how to map an
   * iterator of some type to an iterator of strings.
   */
  public abstract static class ToStringIterator<T> implements Iterator<String> {

    Iterator<T> innerItr;

    ToStringIterator(Iterator<T> itr) {
      innerItr = itr;
    }

    public boolean hasNext() {
      return innerItr.hasNext();
    }

    /**
     * Subclasses should override this method to convert T to String.
     */
    @Override
    public abstract String next();

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * An iterator which iterates a sequences of iterators.
   */
  public abstract static class IteratorChain<T> implements Iterator<T> {

    /**
     * A reference to the last item returned by next().
     * Implementations may be able to use this as the offset into the
     * collection being iterated.
     */
    protected T lastIterated = null;

    Iterator<T> partialItr = partialIterator();

    /**
     * Subclasses should construct an iterator for the next range of
     * items, typically continuing after {@link #lastIterated}, or
     * from the beginning of the collection if lastIterated is null.
     */
    abstract Iterator<T> partialIterator();

    public boolean hasNext() {
      if (partialItr.hasNext()) {
        return true;
      }
      if (lastIterated == null) {
        return false;
      }
      partialItr = partialIterator();
      lastIterated = null;
      return partialItr.hasNext();
    }

    public T next() {
      return lastIterated = partialItr.next();
    }

    /** @throws UnsupportedOperationException */
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  static Tasks instance = null;

  static Tasks getInstance() {
    return instance;
  }

  final Map<String, Processor> processors;
  final Queue queue;
  final String thisUrl;

  public Tasks() {
    processors = new HashMap<String, Processor>();
    // TODO(pmy): tried to access this programmatically, but
    // servletconfig is null at this point?
    thisUrl = "/_ah/taskshelper"; // npe: getServletConfig().getInitParameter("path");
    queue = QueueFactory.getDefaultQueue();
    instance = this;
  }

  void enqueueProcess(Iterator<String> args, int batchSize, Processor processor) {

    // TODO(pmy): remove finished processes.
    processors.put(processor.name, processor);

    TaskOptions opts = TaskOptions.Builder.withMethod(TaskOptions.Method.POST).url(thisUrl)
        .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
        .param(PARAM_PROC_ID, processor.name);

    while (args.hasNext()) {
      for (int i = 0; i < batchSize - 1 && args.hasNext(); i++) {
        opts.param(PARAM_ARGS, args.next());
      }
      queue.add(opts);
      opts.removeParam(PARAM_ARGS);
    }
  }

  /**
   * Called by taskqueue on tasks created by this class on behalf of
   * the caller of {@link #enqueueProcess}
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {
    String procId = param(PARAM_PROC_ID);
    String [] args = params(PARAM_ARGS);
    if (!paramsOk("request must include a processor id and one or more args.",
                  rsp)) {
      return;
    }

    Processor proc = processors.get(procId);
    if (proc == null) {
      badRequest("No such processor: " + procId, rsp);
      return;
    }

    proc.process(args);
  }
}
