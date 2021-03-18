package com.luma.boston.pdw.updates.service;

import com.luma.boston.pdw.updates.service.service.CsvService;
import com.luma.boston.pdw.updates.service.service.ProductsService;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class })
public class PdwUpdatesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdwUpdatesServiceApplication.class, args);
	}

	@Component
	static class Runner implements ApplicationListener<ApplicationReadyEvent> {

		private final ProductsService productsService;
		private final CsvService csvService;
		private final String relativeCsvFilePath;

		public Runner(ProductsService productsService, CsvService csvService, @Value("${relative-csv-file.path}") String relativeCsvFilePath) {
			this.productsService = productsService;
			this.csvService = csvService;
			this.relativeCsvFilePath = relativeCsvFilePath;
		}

		@Override
		@SneakyThrows
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
			// Parse CSV
			var classpathResource = new ClassPathResource(relativeCsvFilePath);
			var csvHeaders = new LinkedList<>(csvService.readHeaders(classpathResource.getFile()));
			var csvRecords = new LinkedList<>(csvService.readCsv(classpathResource.getFile()));
			// Get headers to find fields that need to be updated
			var idHeader = csvHeaders.get(0);
			csvHeaders.remove(0);
			// Get all products by id/cusip/isin /v2/searchCriteria
			var recordsIdFieldsValuesMap  = new LinkedHashMap<String, Map<String, Object>>();
			csvRecords.forEach(record -> {
				recordsIdFieldsValuesMap.put(record.get(idHeader), buildFieldsValuesMap(csvHeaders, record));
			});
			var products = productsService.retrieveProducts(recordsIdFieldsValuesMap.keySet());
			// Make updates for field in csv
			products.forEach(product -> {
				var recordFieldsValuesMap = Optional.ofNullable(
						recordsIdFieldsValuesMap.get(product.getProductGeneral().getCusip())
				).orElseGet(() -> recordsIdFieldsValuesMap.get(product.getProductGeneral().getIsin()));
				// Update fields - this will need to change based on the field(s) in the CSV
				var structureNameInteral = String.valueOf(recordFieldsValuesMap.get("productGeneral.structureNameInternal"));
				product.getProductGeneral().setStructureNameInternal(structureNameInteral);
				// Save back to PDW
				productsService.save(product);
			});
		}

		// Assumes record header is row 1 AND id column is column 1
		private Map<String, Object> buildFieldsValuesMap(List<String> headers, CSVRecord record) {
			var fieldsValuesMap = new LinkedHashMap<String, Object>();
			headers.forEach(header -> {
				fieldsValuesMap.put(header, record.get(header));
			});
			return fieldsValuesMap;
		}
	}
}
