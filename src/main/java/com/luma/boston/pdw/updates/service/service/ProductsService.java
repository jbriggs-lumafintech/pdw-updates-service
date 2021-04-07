package com.luma.boston.pdw.updates.service.service;

import com.luma.boston.pdw.updates.service.client.ProductDataWarehouseServiceClient;
import com.luma.pdw.model.CanonicalProduct;
import com.luma.pdw.model.JunctionOperation;
import com.luma.pdw.model.SearchCriteria;
import com.luma.pdw.model.SearchOperation;
import com.luma.pdw.model.SearchOptions;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import lombok.CustomLog;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@CustomLog
public class ProductsService {

    private final ProductDataWarehouseServiceClient productDataWarehouseServiceClient;
    private final Integer pageSizeOverride;

    public ProductsService(ProductDataWarehouseServiceClient productDataWarehouseServiceClient, @Value("${page-size-override:}") Integer pageSizeOverride) {
        this.productDataWarehouseServiceClient = productDataWarehouseServiceClient;
        this.pageSizeOverride = pageSizeOverride;
    }

    public List<CanonicalProduct> retrieveProducts() {

        return retrieveProducts(Set.of());
    }

    public List<CanonicalProduct> retrieveProducts(Set<String> ids) {
        int page = 0;
        var getAllProducts = ids == null || ids.isEmpty();
        var pageSize = ids != null && !ids.isEmpty() ? ObjectUtils.firstNonNull(pageSizeOverride, ids.size()) : 500;
        Page<CanonicalProduct> canonicalProductsPage = getAllProducts
                ? productDataWarehouseServiceClient.getProducts(PageRequest.of(page, pageSize, Sort.by("id")))
                : productDataWarehouseServiceClient.getProductsBySearchCriteria(withSearchOperations(ids), page, pageSize, Set.of("id"));

        List<CanonicalProduct> canonicalProducts = new ArrayList<>(canonicalProductsPage.getContent());
        var hasNext = canonicalProductsPage.hasNext();
        while (hasNext) {
            var remainingElements = canonicalProductsPage.getTotalElements() - canonicalProducts.size();
            if (remainingElements > Integer.MAX_VALUE) {
                throw new RuntimeException();
            }
            var nextCanonicalProductsPage = getAllProducts
                    ? productDataWarehouseServiceClient.getProducts(PageRequest.of(++page, pageSize, Sort.by("id")))
                    : productDataWarehouseServiceClient.getProductsBySearchCriteria(withSearchOperations(ids), ++page, pageSize, Set.of("id"));

            canonicalProducts.addAll(nextCanonicalProductsPage.getContent());
            hasNext = nextCanonicalProductsPage.hasNext();
        }

        var missingIds = ids.stream()
                .filter(id -> !canonicalProducts.stream()
                        .map(cp -> cp.getProductGeneral().getIsin())
                        .collect(Collectors.toSet()).contains(id))
                .collect(Collectors.toSet());
        if (!getAllProducts) {
            log.info("Retrieved {} products from PDW for {} ids.", canonicalProducts.size(), ids.size());
            log.info("Products missing for the following ids: {}.", missingIds);
        } else {
            log.info("Retrieved {} products from PDW.", canonicalProducts.size());
        }

        return canonicalProducts;
    }

    public Optional<CanonicalProduct> save(CanonicalProduct canonicalProduct) {
        ResponseEntity<CanonicalProduct> response = null;
        try {
            response = productDataWarehouseServiceClient.updateProduct(canonicalProduct);
            var updatedProduct = response.getBody();
            log.info("Updated product with cusip {}, isin {}, productId {} successfully.", updatedProduct.getProductGeneral().getCusip(),
                    updatedProduct.getProductGeneral().getIsin(),
                    updatedProduct.getProductId());
            return Optional.of(updatedProduct);
        } catch (HystrixRuntimeException e) {
            log.error("Error saving product with cusip {}, isin {}, productId {}", canonicalProduct.getProductGeneral().getCusip(),
                    canonicalProduct.getProductGeneral().getIsin(),
                    canonicalProduct.getProductId());;
            log.error("Exception: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private SearchOptions withSearchOperations(Set<String> ids) {
        var cusipCriteria = new SearchCriteria();
        cusipCriteria.setKey("productGeneral.cusip");
        cusipCriteria.setOperation(SearchOperation.IN);
        cusipCriteria.setValue(ids);
        var isinCriteria = new SearchCriteria();
        isinCriteria.setKey("productGeneral.isin");
        isinCriteria.setOperation(SearchOperation.IN);
        isinCriteria.setValue(ids);
        var searchOptions = new SearchOptions();
        searchOptions.setSearchCriteriaList(List.of(cusipCriteria, isinCriteria));
        searchOptions.setJunctionOperation(Optional.of(JunctionOperation.OR));

        return searchOptions;
    }
}
