package com.example.ccaggregator.service;

import org.apache.flink.streaming.api.TimeCharacteristic;

import com.example.ccaggregator.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.table.api.Table;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.apache.flink.table.api.Expressions;
import org.apache.flink.util.function.ThrowingConsumer;
import org.apache.flink.table.expressions.Expression;
import java.util.*;
import java.util.stream.Stream;
import org.apache.flink.table.api.Expressions;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import static org.apache.flink.table.api.Expressions.$;

@Service
public class FlinkQueryService {

  private static final Logger logger = LoggerFactory.getLogger(FlinkQueryService.class);

  private final String bootstrapServers;
  private final String topic;
  private final WebSocketService webSocketService;
  private final QueryClusterManager clusterManager;
  private final ExecutorService queryExecutor;
  private final Map<String, Future<?>> runningQueries = new ConcurrentHashMap<>();

  public FlinkQueryService(
      @Value("${kafka.bootstrap-servers}") String bootstrapServers,
      @Value("${kafka.topic.transactions}") String topic,
      WebSocketService webSocketService,
      QueryClusterManager clusterManager,
      @Value("${query.executor.pool.size:4}") int poolSize) {
    this.bootstrapServers = bootstrapServers;
    this.topic = topic;
    this.webSocketService = webSocketService;
    this.clusterManager = clusterManager;
    this.queryExecutor = Executors.newFixedThreadPool(poolSize);
  }

  @PreDestroy
  public void shutdown() {
    queryExecutor.shutdown();
    try {
      if (!queryExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
        queryExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      queryExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  // Serializable map function for JSON to Transaction conversion
  private static class JsonToTransactionMapper
      implements MapFunction<String, Transaction>, Serializable {
    private static final long serialVersionUID = 1L;
    private transient ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(JsonToTransactionMapper.class);

    private ObjectMapper getObjectMapper() {
      if (objectMapper == null) {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
      }
      return objectMapper;
    }

    @Override
    public Transaction map(String json) throws Exception {
      try {
        return getObjectMapper().readValue(json, Transaction.class);
      } catch (Exception e) {
        logger.error("Failed to parse JSON: {}", json, e);
        return null;
      }
    }
  }

  public void processTransaction(String jsonTransaction) {
    try {
      JsonToTransactionMapper mapper = new JsonToTransactionMapper();
      Transaction transaction = mapper.map(jsonTransaction);

    } catch (Exception e) {
      logger.error("Failed to process transaction: {}", jsonTransaction, e);
    }
  }

  public Future<Void> executeQuery(WebSocketSession session, String query, String queryId) {
    Future<Void> future = queryExecutor.submit(() -> {
      QueryClusterManager.QueryClusterContext context = null;
      try {
        // Create a new cluster for this query, passing the queryId
        context = clusterManager.createQueryCluster(queryId);

        logger.info("Initializing Kafka consumer for query: {}", queryId);

      // Configure Kafka consumer properties
      Properties properties = new Properties();
      properties.setProperty("bootstrap.servers", bootstrapServers);
      // Use a single consumer group for all queries
      properties.setProperty("group.id", queryId);
      properties.setProperty("auto.offset.reset", "earliest");
      properties.setProperty("enable.auto.commit", "false");
      properties.setProperty("auto.commit.interval.ms", "1000");
      properties.setProperty("session.timeout.ms", "45000");
      properties.setProperty("heartbeat.interval.ms", "15000");
      properties.setProperty("max.poll.interval.ms", "300000");
      properties.setProperty("max.poll.records", "500");
      properties.setProperty("fetch.min.bytes", "1");
      properties.setProperty("fetch.max.wait.ms", "500");

      // Create Kafka source
      KafkaSource<String> source = KafkaSource.<String>builder()
          .setBootstrapServers(bootstrapServers)
          .setTopics(topic)
          .setGroupId("flink-query-group")
          .setStartingOffsets(OffsetsInitializer.earliest())
          .setValueOnlyDeserializer(new SimpleStringSchema())
          .setProperties(properties)
          .build();

      logger.info("Kafka consumer configuration completed");

      //TypeInformation<Transaction> inTypeInfo= TypeInformation.of(new TypeHint<Transaction>() {});

      // Create DataStream from Kafka consumer
      DataStream<String> kafkaStream = context.env.fromSource(source, 
          org.apache.flink.api.common.eventtime.WatermarkStrategy.noWatermarks(), 
          "Kafka Source");

      // Convert JSON string to Transaction objects with error handling
      DataStream<Transaction> transactionStream =
          kafkaStream
              .map(json -> {
                try {
                  
                  JsonToTransactionMapper mapper = new JsonToTransactionMapper();
                  Transaction transaction = mapper.map(json);
                  return transaction;
                } catch (Exception e) {
                  logger.error("Failed to parse JSON: {}", json, e);
                  return null;
                }
              })
              .filter(transaction -> transaction != null)
              .name("JSON to Transaction")
              //.returns(inTypeInfo)
              .assignTimestampsAndWatermarks(
                  org.apache.flink.api.common.eventtime.WatermarkStrategy
                      .<Transaction>forMonotonousTimestamps()
                      //.<Transaction>forBoundedOutOfOrderness(Duration.ofSeconds(3));
                      .withTimestampAssigner((event, timestamp) -> event.getTxTimestamp()));

      // Create table from DataStream
      logger.info("Creating Flink table from DataStream");

      
    // Expressions for table schema
    List<String> expressions= Arrays.asList(
                "txNumber",
                "ccy",
                "amount",
                "txDate",
                "ccType",
                "merchantId",
                "txTimestamp",
                "ccNumber",
                "merchantType",
                "txLocation");

    // add rowtime attribute
    Expression[] defaultExpressions = {$("r_time").rowtime()};

    Table transactions =
        context.tableEnv.fromDataStream(
            transactionStream,
            Stream.concat(
                    expressions.stream().map(Expressions::$), Arrays.stream(defaultExpressions))
                .toArray(Expression[]::new));

      context.tableEnv.createTemporaryView("Transactions", transactions);

      logger.info("Flink table created successfully");

      Table resultTable = context.tableEnv.sqlQuery(query);

      String[] fieldNames = resultTable.getSchema().getFieldNames();

      // Stream results to WebSocket using toRetractStream for handling updates
      context.tableEnv
          .toRetractStream(resultTable, Row.class)
          .map(tuple -> {
            boolean isAdd = tuple.f0;
            Row row = tuple.f1;
            Object[] data = new Object[row.getArity()];
            for (int i = 0; i < row.getArity(); i++) {
              data[i] = row.getField(i);
            }
            return new ObjectMapper()
                .writeValueAsString(Map.of("type", isAdd ? "add" : "retract", "data", data, "fieldNames", fieldNames));
          })
          .addSink(new WebSocketSink(session.getId(), queryId,fieldNames))
          .name("WebSocket Sink");

        // Execute the Flink job
        JobClient client = context.env.executeAsync("Kafka to WebSocket - " + queryId);

        // Use JobClient.getJobStatus() to wait for the JobID to be available
        client.getJobStatus().thenAccept(jobStatus -> {
            if (jobStatus != null && !jobStatus.isGloballyTerminalState()) {
                JobID jobId = client.getJobID();
                clusterManager.setCurrentJobIdClient(queryId, client);
                logger.info("Kafka to WebSocket streaming started for query: {} with JobID: {}", queryId, jobId);
            } else {
                logger.warn("Job finished too quickly: {}", queryId);
            }
        });


      } catch (Exception e) {
      logger.error("Query execution failed for query: {}", queryId, e);
      try {
        webSocketService.sendMessageToSession(
            session,
            new ObjectMapper()
                .writeValueAsString(
                    Map.of(
                        "type",
                        "query_error",
                        "message",
                        "Query execution failed: " + e.getMessage())));
      } catch (Exception ex) {
        logger.error("Failed to send error message for query: {}", queryId, ex);
      }

        // Clean up the cluster if creation was successful but execution failed
        if (context != null) {
          clusterManager.terminateQueryCluster(queryId);
        }

        throw new RuntimeException("Query execution failed", e);
    }
    return null;
  });
  runningQueries.put(queryId, future);
  return future;
}

  public void cancelQuery(String queryId) {
    Future<?> future = runningQueries.remove(queryId);
    if (future != null) {
      future.cancel(true);
    }

    QueryClusterManager.QueryClusterContext context = clusterManager.getQueryCluster(queryId);
    if (context == null) {
      logger.warn("No cluster found for query: {}", queryId);
      return;
    }

    JobClient jobId = clusterManager.getCurrentJobIdClient(queryId);
    if (jobId == null) {
      logger.warn("No job running for query: {}", queryId);
      return;
    }

    try {
      jobId.cancel().get(30, TimeUnit.SECONDS);
      logger.info("Attempting to cancel job {} for query: {}", jobId, queryId);

      

    } catch (Exception e) {
      logger.error("Error during job cancellation for query {}: {}", queryId, e.getMessage());
    } finally {
      try {
        // Terminate cluster with proper error handling
        clusterManager.terminateQueryCluster(queryId);
        logger.info("Terminated cluster for query: {}", queryId);
      } catch (Exception termError) {
        logger.error("Failed to terminate cluster for query {}: {}", queryId, termError.getMessage());
        throw termError;
      }
    }
  }
  
  }
