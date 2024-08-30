package gov.noaa.pfel.erddap.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ThreadedWorkManager<T> {
  ExecutorService executorService = null;
  List<FutureTask<T>> taskList = new ArrayList<>();
  WorkConsumer<T> processor;

  int completed = 0;

  public ThreadedWorkManager(int nThreads, WorkConsumer<T> processResult) {
    if (nThreads > 1) {
      executorService = Executors.newFixedThreadPool(nThreads);
    }
    processor = processResult;
  }

  public void addTask(Callable<T> callable) throws Exception, Throwable {
    // If we're threaded add the work to the thread.
    if (executorService != null) {
      FutureTask<T> task = new FutureTask<T>(callable);
      taskList.add(task);
      if (executorService != null) {
        executorService.submit(task);
      }
    } else {
      // No threading here, just do the work and process it.
      processor.accept(callable.call());
    }
  }

  public boolean hasNext() {
    return taskList.size() > completed;
  }

  public T getNextTaskResult() throws InterruptedException, ExecutionException {
    // get results table from a futureTask
    // Put null in that position in futureTasks so it can be gc'd after this method
    FutureTask<T> task = taskList.set(completed++, null);
    return task.get();
  }

  public void finishedEnqueing() {
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  public void forceShutdown() {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }

  public void processResults() throws InterruptedException, ExecutionException, Throwable {
    while (hasNext()) {
      processor.accept(getNextTaskResult());
    }
  }
}
