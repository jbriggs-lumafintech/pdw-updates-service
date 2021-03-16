package com.luma.boston.service.config;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.codecs.GridFSFileCodecProvider;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import org.bson.UuidRepresentation;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.Jsr310CodecProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Arrays;


@Configuration
@EnableMongoRepositories(basePackages = "com.luma.boston.service.repository")
@Profile("!test")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri:}")
    private String mongodbUri;

    @Value("${spring.data.mongodb.database:}")
    private String mongodbDatabase;

    private static final CodecRegistry CODEC_REGISTRY = CodecRegistries.fromProviders(
            Arrays.asList(
                    new UuidCodecProvider(UuidRepresentation.STANDARD),
                    new ValueCodecProvider(),
                    new BsonValueCodecProvider(),
                    new DBRefCodecProvider(),
                    new DBObjectCodecProvider(),
                    new DocumentCodecProvider(new DocumentToDBRefTransformer()),
                    new IterableCodecProvider(new DocumentToDBRefTransformer()),
                    new MapCodecProvider(new DocumentToDBRefTransformer()),
                    new GeoJsonCodecProvider(),
                    new GridFSFileCodecProvider(),
                    new Jsr310CodecProvider(),
                    new BsonValueCodecProvider()));

    @Autowired
    private MongoDbFactory mongoDbFactory;

    @Autowired
    private MongoMappingContext mongoMappingContext;

    @Override
    public MongoClient mongoClient() {

        return MongoClients.create(
                MongoClientSettings
                        .builder()
                        .codecRegistry(CODEC_REGISTRY)
                        .applyConnectionString(new ConnectionString(mongodbUri))
                        .build()
        );
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter() {

        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));

        return converter;
    }

    @Override
    protected String getDatabaseName() {
        return mongodbDatabase;
    }
}
