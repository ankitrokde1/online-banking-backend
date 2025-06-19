package com.bankingsystem.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.lang.NonNull;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableMongoRepositories(basePackages = "com.bankingsystem.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    private MongoClient mongoClient;

    @Override
    @NonNull
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    @NonNull
    public MongoClient mongoClient() {
        if (mongoClient == null) {
            ConnectionString connectionString = new ConnectionString(mongoUri);

            MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToSocketSettings(builder ->
                            builder.connectTimeout(10, TimeUnit.SECONDS)
                                   .readTimeout(10, TimeUnit.SECONDS))
                    .applyToClusterSettings(builder ->
                            builder.serverSelectionTimeout(10, TimeUnit.SECONDS))
                    .applyToConnectionPoolSettings(builder ->
                            builder.maxWaitTime(5, TimeUnit.SECONDS))
                    .readPreference(ReadPreference.primary())
                    .build();

            mongoClient = MongoClients.create(mongoClientSettings);
        }
        return mongoClient;
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }

    @PreDestroy
    public void cleanUp() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
