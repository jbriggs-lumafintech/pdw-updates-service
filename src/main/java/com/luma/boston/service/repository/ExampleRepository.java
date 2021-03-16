package com.luma.boston.service.repository;

import com.luma.boston.service.model.ExampleModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExampleRepository extends MongoRepository<ExampleModel, String> {

}
