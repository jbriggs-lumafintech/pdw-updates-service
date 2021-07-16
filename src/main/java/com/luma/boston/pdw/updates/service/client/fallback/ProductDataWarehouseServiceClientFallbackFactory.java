package com.luma.boston.pdw.updates.service.client.fallback;

import com.luma.boston.pdw.updates.service.client.ProductDataWarehouseServiceClient;
import com.luma.data.model.product.ProductDate;
import com.luma.pdw.model.CanonicalProduct;
import com.luma.pdw.model.SearchCriteria;
import com.luma.pdw.model.SearchOptions;
import feign.hystrix.FallbackFactory;
import lombok.CustomLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Component
@CustomLog
public class ProductDataWarehouseServiceClientFallbackFactory implements FallbackFactory<ProductDataWarehouseServiceClient> {
    @Override
    public ProductDataWarehouseServiceClient create(Throwable throwable) {
        return new ProductDataWarehouseServiceClient() {
            @Override
            public Page<CanonicalProduct> getProducts(Pageable pageable) {
                log.error("Fallback for ProductDataWarehouseServiceClient::getProducts", throwable);
                return Page.empty();
            }

            @Override
            public CanonicalProduct updateProduct(CanonicalProduct productUpdate) {
                log.error("Fallback for ProductDataWarehouseServiceClient::updateProduct", throwable);
                if (productUpdate != null) {
                    log.error("Error saving product with cusip {}, isin {}, productId {}", productUpdate.getProductGeneral().getCusip(),
                            productUpdate.getProductGeneral().getIsin(),
                            productUpdate.getProductId());
                }
                return null;
            }

            @Override
            public Page<CanonicalProduct> getProductsBySearchCriteria(SearchOptions searchOptions, int page, int size, Collection<String> sort) {
                log.error("Fallback for ProductDataWarehouseServiceClient::getProductsBySearchCriteria", throwable);
                return Page.empty();
            }

            @Override
            public List<ProductDate> getProductDatesByDateRange(LocalDate startDate, LocalDate endDate, SearchCriteria searchCriteria) {
                log.error("Fallback for ProductDataWarehouseServiceClient::getProductDatesByDateRange", throwable);
                return List.of();
            }

            @Override
            public CanonicalProduct getProductByProductId(String id) {
                log.error("Fallback for ProductDataWarehouseServiceClient::getProductDatesByDateRange", throwable);
                return null;
            }
        };
    }
}
