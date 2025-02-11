package gov.noaa.pfel.erddap.util;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class BoundaryCounter {
  private Counter counter;

  public BoundaryCounter(String name, String help) {
    counter = Counter.builder().name(name).help(help).labelNames("status").build();
  }

  public void increment(Metrics.BoundaryRequest status) {
    counter.labelValues(status.name()).inc();
  }

  public long get(Metrics.BoundaryRequest status) {
    return counter.labelValues(status.name()).getLongValue();
  }

  public void register(PrometheusRegistry registry) {
    registry.register(counter);
  }
}
