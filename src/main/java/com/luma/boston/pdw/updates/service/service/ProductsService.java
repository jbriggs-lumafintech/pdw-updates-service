package com.luma.boston.pdw.updates.service.service;

import com.luma.boston.pdw.updates.service.client.ProductDataWarehouseServiceClient;
import com.luma.pdw.model.CanonicalProduct;
import com.luma.pdw.model.JunctionOperation;
import com.luma.pdw.model.SearchCriteria;
import com.luma.pdw.model.SearchOperation;
import com.luma.pdw.model.SearchOptions;
import lombok.CustomLog;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@CustomLog
public class ProductsService {

    private final ProductDataWarehouseServiceClient productDataWarehouseServiceClient;
    private final Integer pageSizeOverride;

    public ProductsService(ProductDataWarehouseServiceClient productDataWarehouseServiceClient, @Value("${page-size-override:}") Integer pageSizeOverride) {
        this.productDataWarehouseServiceClient = productDataWarehouseServiceClient;
        this.pageSizeOverride = pageSizeOverride;
    }

    public List<CanonicalProduct> retrieveProducts(Set<String> ids) {
        int page = 0;
        var pageSize = ObjectUtils.firstNonNull(pageSizeOverride, ids.size());
        Page<CanonicalProduct> canonicalProductsPage = productDataWarehouseServiceClient
                .getProductsBySearchCriteria(withSearchOperations(ids), page, pageSize, Set.of("id"));

        List<CanonicalProduct> canonicalProducts = new ArrayList<>(canonicalProductsPage.getContent());
        var hasNext = canonicalProductsPage.hasNext();
        while (hasNext) {
            var remainingElements = canonicalProductsPage.getTotalElements() - canonicalProducts.size();
            if (remainingElements > Integer.MAX_VALUE) {
                throw new RuntimeException();
            }
            var nextPageSize = remainingElements > pageSize ? pageSize : remainingElements;
            var nextCanonicalProductsPage = productDataWarehouseServiceClient
                    .getProductsBySearchCriteria(withSearchOperations(ids), ++page, (int) nextPageSize, Set.of("id"));
            canonicalProducts.addAll(nextCanonicalProductsPage.getContent());
            hasNext = nextCanonicalProductsPage.hasNext();
        }

        return canonicalProducts;
    }

    @SuppressWarnings("unchecked")
    public void save(CanonicalProduct canonicalProduct) {
        var response = productDataWarehouseServiceClient.updateProduct(canonicalProduct);

        log.info("Received {} from PDW on save.", response.getStatusCode());
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
