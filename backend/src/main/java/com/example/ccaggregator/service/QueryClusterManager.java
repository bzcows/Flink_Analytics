package com.example.ccaggregator.service;




import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.core.execution.JobClient;

@Service
public class QueryClusterManager {
    private static final Logger logger = LoggerFactory.getLogger(QueryClusterManager.class);

    private static final int BASE_REST_PORT = 8081;
    
    private final Map<String, QueryClusterContext> queryClusters = new ConcurrentHashMap<>();
  private final AtomicInteger portCounter = new AtomicInteger(0);

  public static class QueryClusterContext {
   
    StreamExecutionEnvironment env;
    StreamTableEnvironment tableEnv;
    volatile JobClient currentJobClient;

    public QueryClusterContext( StreamExecutionEnvironment env,
        StreamTableEnvironment tableEnv) {
      this.env = env;
      this.tableEnv = tableEnv;
    }
  }

  private Configuration getConfig(int portOffset) {
    Configuration config = new Configuration();
    config.setInteger(RestOptions.PORT, BASE_REST_PORT + portOffset);
    config.setString("taskmanager.registration.timeout", "PT10M");
    return config;
  }

  public QueryClusterContext createQueryCluster(String queryId) throws Exception {
    // String queryId = "query-" + System.currentTimeMillis() + "-" + portCounter.get(); // Removed
    int portOffset = portCounter.incrementAndGet();

    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    // In Flink 1.17+, EventTime is the default time characteristic and this setting is deprecated
    env.setParallelism(1);

    EnvironmentSettings settings = EnvironmentSettings.newInstance()
        .inStreamingMode()
        .build();

    StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

    Configuration tableConfig = new Configuration();
    tableConfig.setString("table.exec.mini-batch.enabled", "false");
    tableConfig.setString("table.exec.mini-batch.allow-latency", "1s");
    tableConfig.setString("table.exec.mini-batch.size", "1000");
    tableConfig.setString("table.exec.resource.default-parallelism", "1");

    for (Map.Entry<String, String> entry : tableConfig.toMap().entrySet()) {
      tableEnv.getConfig().getConfiguration().setString(entry.getKey(), entry.getValue());
    }

    QueryClusterContext context = new QueryClusterContext( env, tableEnv);
    queryClusters.put(queryId, context);

    logger.info("Created and started MiniCluster for query: {} on port: {}",
        queryId, BASE_REST_PORT + portOffset);

    return context;
  }

  public QueryClusterContext getQueryCluster(String queryId) {
    return queryClusters.get(queryId);
  }

  private final Lock terminationLock = new ReentrantLock();

  public void terminateQueryCluster(String queryId) {
    terminationLock.lock();
    try {
      QueryClusterContext context = queryClusters.get(queryId);
      if (context == null) return;

      try {

        
        logger.info("Cancelling job {} for query: {}", context.currentJobClient.getJobID(), queryId);
        context.env.close();

        
      } catch (TimeoutException te) {
        logger.error("Cluster shutdown timeout for query: {}", queryId);
        throw new RuntimeException("Shutdown timed out", te);
      } catch (Exception e) {
        logger.error("Error during termination for query {}: {}", queryId, e.getMessage());
        throw new RuntimeException("Termination failed", e);
      } finally {
               
        queryClusters.remove(queryId);
        
      }
    } finally {
      terminationLock.unlock();
    }
  }

  public void setCurrentJobIdClient(String queryId, JobClient JobClient) {
    QueryClusterContext context = queryClusters.get(queryId);
    if (context != null) {
      context.currentJobClient = JobClient;
    }
  }

  public JobClient getCurrentJobIdClient(String queryId) {
    QueryClusterContext context = queryClusters.get(queryId);
    if (context != null) {
      return context.currentJobClient;
    }
    return null;
  }
}
