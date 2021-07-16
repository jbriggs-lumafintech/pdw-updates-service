package com.luma.boston.pdw.updates.service.client;

import com.luma.boston.pdw.updates.service.client.fallback.ProductDataWarehouseServiceClientFallbackFactory;
import com.luma.data.model.product.ProductDate;
import com.luma.pdw.model.CanonicalProduct;
import com.luma.pdw.model.SearchCriteria;
import com.luma.pdw.model.SearchOptions;
import com.luma.security.feign.security.InterServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@FeignClient(name = "pdw-service", url = "${pdw-service.url}", configuration = InterServiceFeignConfig.class, fallbackFactory = ProductDataWarehouseServiceClientFallbackFactory.class)
public interface ProductDataWarehouseServiceClient {

    @GetMapping(value = "/products")
    Page<CanonicalProduct> getProducts(Pageable pageable);

    @PutMapping(value = "/products")
    CanonicalProduct updateProduct(@RequestBody CanonicalProduct productUpdate);

    @PostMapping("/products/v2/searchCriteria")
    Page<CanonicalProduct> getProductsBySearchCriteria(@RequestBody SearchOptions searchOptions,
                                                       @RequestParam(name = "page") int page,
                                                       @RequestParam(name = "size") int size,
                                                       @RequestParam(name = "sort") Collection<String> sort);

    @PostMapping("/products/dates")
    List<ProductDate> getProductDatesByDateRange(@RequestParam(value = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                 @RequestParam(value = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                 @RequestBody SearchCriteria searchCriteria);

    @GetMapping("/products/{id}")
    CanonicalProduct getProductByProductId(@PathVariable("id") String id);
}
