package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.Supplier;
import com.bobbuy.service.BobbuyStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile/suppliers")
public class MobileSupplierController {
    private final BobbuyStore store;

    public MobileSupplierController(BobbuyStore store) {
        this.store = store;
    }

    @GetMapping
    public ApiResponse<List<Supplier>> list() {
        List<Supplier> suppliers = store.listSuppliers();
        return ApiResponse.success(suppliers, new ApiMeta(suppliers.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> get(@PathVariable String id) {
        Supplier supplier = store.getSupplier(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.supplier.not_found"));
        return ResponseEntity.ok(ApiResponse.success(supplier));
    }

    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<ApiResponse<Supplier>> create(@RequestBody Supplier supplier) {
        Supplier saved = store.saveSupplier(supplier);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> update(@PathVariable String id, @RequestBody Supplier supplier) {
        Supplier updated = store.updateSupplier(id, supplier)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.supplier.not_found"));
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
