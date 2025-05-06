package com.ankur.daily.meals.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseMetadataService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataService.class);

  private final Driver driver;

  public DatabaseMetadataService(Driver driver) {
    this.driver = driver;
  }

  public void logDatabaseMetadata() {
    try (Session session = driver.session()) {
      // Log database name
      String dbName = session.run("CALL db.info()").single().get("name").asString();
      logger.info("Connected to Neo4j database: {}", dbName);

      // Log all node labels
      Result labelResult = session.run("CALL db.labels()");
      List<String> labels = labelResult.list(record -> record.get("label").asString());
      logger.info("Available node labels: {}", labels);

      // Log total node count
      int nodeCount = session.run("MATCH (n) RETURN count(n) AS totalNodes")
        .single()
        .get("totalNodes")
        .asInt();
      logger.info("Total nodes in the database: {}", nodeCount);
    } catch (Exception e) {
      logger.error("Error fetching Neo4j database metadata", e);
    }
  }
}
