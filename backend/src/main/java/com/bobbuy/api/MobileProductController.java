package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.Product;
import com.bobbuy.model.ProductPatch;
import com.bobbuy.service.BobbuyStore;
import com.bobbuy.service.LocalizedJsonbReaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/mobile/products")
public class MobileProductController {
    private final BobbuyStore store;
    private final LocalizedJsonbReaderService localizedJsonbReaderService;

    public MobileProductController(BobbuyStore store, LocalizedJsonbReaderService localizedJsonbReaderService) {
        this.store = store;
        this.localizedJsonbReaderService = localizedJsonbReaderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MobileProductResponse>> get(@PathVariable String id, Locale locale) {
        Product product = store.getProduct(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.product.not_found"));
        return ResponseEntity.ok(ApiResponse.success(toResponse(product, locale)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MobileProductResponse>> patch(@PathVariable String id,
                                                                     @RequestBody ProductPatch patch,
                                                                     Locale locale) {
        Product updated = store.patchProduct(id, patch)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.product.not_found"));
        return ResponseEntity.ok(ApiResponse.success(toResponse(updated, locale)));
    }

    private MobileProductResponse toResponse(Product product, Locale locale) {
        String displayName = localizedJsonbReaderService.read(product.getName(), locale);
        String displayDescription = localizedJsonbReaderService.read(product.getDescription(), locale);
        return new MobileProductResponse(product, displayName, displayDescription);
    }
}
