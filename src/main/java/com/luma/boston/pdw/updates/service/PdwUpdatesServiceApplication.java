package com.luma.boston.pdw.updates.service;

import com.google.common.collect.Lists;
import com.luma.api.product.model.ClientSpecific;
import com.luma.api.product.model.ProductGeneral;
import com.luma.boston.pdw.updates.service.service.CsvService;
import com.luma.boston.pdw.updates.service.service.ProductsService;
import com.luma.pdw.model.JunctionOperation;
import com.luma.pdw.model.SearchCriteria;
import com.luma.pdw.model.SearchOperation;
import com.luma.pdw.model.SearchOptions;
import lombok.CustomLog;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@CustomLog
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class })
public class PdwUpdatesServiceApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(PdwUpdatesServiceApplication.class, args);
		} catch (Throwable t) {
			log.error("Fatal Exception:", t);
		}
	}

	@Component
	@CustomLog
	static class Runner implements ApplicationListener<ApplicationReadyEvent> {

		private final ProductsService productsService;
		private final CsvService csvService;
		private final String relativeCsvFilePath;
		private final Boolean rederivePdwProducts;
		private final Boolean pullFromFile;

		private static final Set<String> hardcodedList = Set.of(
		);

		public Runner(ProductsService productsService, CsvService csvService,
					  @Value("${relative-csv-file.path:}") String relativeCsvFilePath,
					  @Value("${rederive-pdw-products:false}") Boolean rederivePdwProducts,
					  @Value("${pull-from-file:false}") Boolean pullFromFile) {
			this.productsService = productsService;
			this.csvService = csvService;
			this.relativeCsvFilePath = relativeCsvFilePath;
			this.rederivePdwProducts = rederivePdwProducts;
			this.pullFromFile = pullFromFile;
		}

		@Override
		@SneakyThrows
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
			// Parse CSV
			if (pullFromFile && relativeCsvFilePath != null) {
				doUpdateFromCsv();
			}
			if (!pullFromFile && hardcodedList != null && !hardcodedList.isEmpty()) {
				doUpdateOnSubsetOfProducts(hardcodedList);
			}

			if (rederivePdwProducts) {
				doUpdateOnPdwProducts();
			}
		}

		private void doUpdateOnSubsetOfProducts(Set<String> hardcodedList) {
			var products = productsService.retrieveProducts(hardcodedList);
			var count = new AtomicInteger(0);
			products.forEach(product -> {
				product.getProductGeneral().setCusip(null);
				productsService.save(product, count.incrementAndGet());
			});
		}

		private void doUpdateOnPdwProducts() {
			var products = productsService.retrieveProducts();
			var count = new AtomicInteger(0);
			var productsMeetingCriteria = products.stream()
					.filter(product -> product.getProductGeneral().getWrapperType() == ProductGeneral.WrapperTypeEnum.CD && product.getProductGeneral().getRegistrationType() == null)
					.collect(Collectors.toList());
// db.getCollection('PdwProductCore').find({'productGeneral.wrapperType': 'CD', 'productGeneral.registrationType': null}).count()
//			var productsMeetingCriteria = products.stream()
//					.filter(product -> product.getProductGeneral().getUnderlierList() != null && product.getProductGeneral().getUnderlierList().size() == 1 && product.getProductGeneral().getBasketType() == null)
//					.collect(Collectors.toList());
// db.getCollection('PdwProductCore').find({'productGeneral.underlierList': {$size: 1}, 'productGeneral.basketType': null}).count()
			var productPartitions = Lists.partition(productsMeetingCriteria, 5);
			log.info("Products meeting criteria {}", productsMeetingCriteria.size());
			productsMeetingCriteria.forEach(product -> {
//					product.getProductGeneral().setBasketType(ProductGeneral.BasketTypeEnum.SINGLE);
					product.getProductGeneral().setRegistrationType(ProductGeneral.RegistrationTypeEnum.CD);
					productsService.save(product, count.incrementAndGet());
			});
//			productPartitions.forEach(partition -> {
//				// Save back to PDW async
//				List<CompletableFuture<Void>> futures = new ArrayList<>();
//				partition.forEach(product -> {
//					futures.add(CompletableFuture.runAsync(() -> {
//						product.getProductGeneral().setRegistrationType(ProductGeneral.RegistrationTypeEnum.CD);
//						productsService.save(product, count.incrementAndGet());
//					}));
//				});
//				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//			});
		}

		private void doUpdateOnPdwProductWithSearchOptions(SearchOptions searchOptions) {
			var products = productsService.retrieveProducts(searchOptions);
			var count = new AtomicInteger(0);
			var productPartitions = Lists.partition(products, 6);

			products.forEach(product -> {
				if (product.getProductGeneral().getRegistrationType() == null) {
					product.getProductGeneral().setRegistrationType(ProductGeneral.RegistrationTypeEnum.CD);
					productsService.save(product, count.incrementAndGet());
				}
			});
			productPartitions.forEach(partition -> {
				// Save back to PDW async
				List<CompletableFuture<Void>> futures = new ArrayList<>();
				partition.forEach(product -> {
					futures.add(CompletableFuture.runAsync(() -> productsService.save(product, count.incrementAndGet())));
				});
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			});
		}

		@SneakyThrows
		private void doUpdateFromCsv() {
			var classpathResource = new ClassPathResource(relativeCsvFilePath);
			var csvHeaders = new LinkedList<>(csvService.readHeaders(classpathResource.getFile()));
			var csvRecords = new LinkedList<>(csvService.readCsv(classpathResource.getFile()));
//			// Get headers to find fields that need to be updated
			var idHeader = csvHeaders.get(0);
			var traderHeader = csvHeaders.get(1);
//			// Get all products by id/cusip/isin /v2/searchCriteria
			var recordsIdTraderMap = new LinkedHashMap<String, String>();
//			var keys = new HashSet<String>();
			csvRecords.forEach(record -> {
				recordsIdTraderMap.put(record.get(idHeader), record.get(traderHeader));
//				keys.add(record.get(0));
			});
			var keys = recordsIdTraderMap.keySet();
			var products = productsService.retrieveProducts(keys);
//			// Make updates for field in csv
			var count = new AtomicInteger(0);


			products.forEach(product -> {
				var matchingProductTrader = recordsIdTraderMap.get(product.getProductGeneral().getCusip());
				if (matchingProductTrader == null) {
					log.error("No match for product with productId: {}", product.getProductId());
					return;
				}
				if (product.getClientSpecific() != null) {
					product.getClientSpecific().setTrader(matchingProductTrader);
				} else {
					product.setClientSpecific(new ClientSpecific().trader(matchingProductTrader));
				}
//				product.getProductGeneral().setCompletionStatus(ProductGeneral.CompletionStatusEnum.COMPLETE);

//				productsService.save(product, count.incrementAndGet());
			});

			log.info("Successfully saved {} products out of {} attempted.", count.get(), products.size());
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
