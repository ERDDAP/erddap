# ERDDAP Prometheus Monitoring

This directory contains Docker configurations for monitoring ERDDAP instances with Prometheus and Grafana.

## Quick Start

1. Edit `prometheus.yml` to configure your ERDDAP instance URLs
2. Run `docker-compose up -d`
3. Access Prometheus at http://localhost:9090
4. Access Grafana at http://localhost:3000 (admin/erddapadmin)

## Monitoring Multiple ERDDAP Instances

Edit the `scrape_configs` section in `prometheus.yml` to add additional ERDDAP instances:

  ```yaml
  scrape_configs:
    - job_name: 'erddap-instance1'
      metrics_path: '/erddap/metrics'
      static_configs:
        - targets: ['erddap1.example.com:8080']
          labels:
            instance: 'production'

    - job_name: 'erddap-instance2'
      metrics_path: '/erddap/metrics'
      static_configs:
        - targets: ['erddap2.example.com:8080']
          labels:
            instance: 'development'

  Included Dashboards

  - ERDDAP Overview: General metrics about datasets, requests, and server health
  - ERDDAP Performance: Detailed performance metrics for requests and operations

  Configuration Options

  See https://prometheus.io/docs/prometheus/latest/configuration/configuration/ for additional configuration options.

  To create all the necessary directories, run these commands:

  ```bash
  # Create Java API directories
  mkdir -p "/Users/lareina/Desktop/erddp test/erddap/src/main/java/gov/noaa/pfel/erddap/monitoring/config"
  mkdir -p "/Users/lareina/Desktop/erddp test/erddap/src/main/java/gov/noaa/pfel/erddap/monitoring/servlet"

  # Create Docker configuration directories
  mkdir -p "/Users/lareina/Desktop/erddp test/erddap/docker/prometheus/grafana/provisioning/datasources"
  mkdir -p "/Users/lareina/Desktop/erddp test/erddap/docker/prometheus/grafana/provisioning/dashboards"
  mkdir -p "/Users/lareina/Desktop/erddp test/erddap/docker/prometheus/grafana/dashboards"

  These files form a complete solution that demonstrates:
  1. A clean API design for metrics collection
  2. A Docker-based Prometheus server configuration
  3. Grafana dashboards for visualization
  4. Documentation for ERDDAP administrators
