package com.bobbuy.api;

import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.service.FinancialPdfService;
import com.bobbuy.service.ProcurementHudService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/procurement")
public class ProcurementController {
  private final ProcurementHudService procurementHudService;
  private final FinancialPdfService financialPdfService;

  public ProcurementController(ProcurementHudService procurementHudService,
                               FinancialPdfService financialPdfService) {
    this.procurementHudService = procurementHudService;
    this.financialPdfService = financialPdfService;
  }

  @GetMapping("/{tripId}/hud")
  public ResponseEntity<ApiResponse<ProcurementHudResponse>> hud(@PathVariable Long tripId) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.getHudStats(tripId)));
  }

  @GetMapping("/{tripId}/deficit")
  public ResponseEntity<ApiResponse<List<ProcurementDeficitItemResponse>>> deficit(@PathVariable Long tripId) {
    List<ProcurementDeficitItemResponse> items = procurementHudService.getDeficitItems(tripId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @GetMapping("/{tripId}/expenses")
  public ResponseEntity<ApiResponse<List<TripExpenseResponse>>> expenses(@PathVariable Long tripId) {
    List<TripExpenseResponse> items = procurementHudService.getTripExpenses(tripId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @GetMapping("/{tripId}/expenses/{expenseId}/receipt-preview")
  public ResponseEntity<ApiResponse<ReceiptPreviewResponse>> receiptPreview(@PathVariable Long tripId,
                                                                            @PathVariable Long expenseId) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.getExpenseReceiptPreview(tripId, expenseId)));
  }

  @PostMapping("/{tripId}/expenses")
  public ResponseEntity<ApiResponse<TripExpenseResponse>> createExpense(@PathVariable Long tripId,
                                                                        @RequestBody TripExpenseRequest request) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.createTripExpense(tripId, request)));
  }

  @GetMapping("/{tripId}/profit-sharing")
  public ResponseEntity<ApiResponse<ProfitSharingConfigResponse>> profitSharing(@PathVariable Long tripId) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.getProfitSharingConfig(tripId)));
  }

  @PatchMapping("/{tripId}/profit-sharing")
  public ResponseEntity<ApiResponse<ProfitSharingConfigResponse>> updateProfitSharing(@PathVariable Long tripId,
                                                                                       @RequestBody ProfitSharingConfigRequest request) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.updateProfitSharingConfig(tripId, request)));
  }

  @GetMapping("/{tripId}/logistics")
  public ResponseEntity<ApiResponse<List<LogisticsTrackingResponse>>> logistics(@PathVariable Long tripId) {
    List<LogisticsTrackingResponse> items = procurementHudService.getTripLogisticsTrackings(tripId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @PostMapping("/{tripId}/logistics")
  public ResponseEntity<ApiResponse<LogisticsTrackingResponse>> createLogistics(@PathVariable Long tripId,
                                                                                 @RequestBody LogisticsTrackingRequest request) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.createTripLogisticsTracking(tripId, request)));
  }

  @PostMapping("/{tripId}/logistics/{trackingId}/refresh")
  public ResponseEntity<ApiResponse<LogisticsTrackingResponse>> refreshLogistics(@PathVariable Long tripId,
                                                                                  @PathVariable Long trackingId) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.refreshTripLogisticsTracking(tripId, trackingId)));
  }

  @PostMapping("/{tripId}/manual-reconcile")
  public ResponseEntity<ApiResponse<ManualReconcileResponse>> manualReconcile(@PathVariable Long tripId,
                                                                               @RequestBody ManualReconcileRequest request) {
    int transferred = procurementHudService.manualReconcile(
        tripId,
        request.getSkuId(),
        request.getFromBusinessId(),
        request.getToBusinessId(),
        request.getQuantity());
    return ResponseEntity.ok(ApiResponse.success(
        new ManualReconcileResponse(
            request.getSkuId(),
            request.getFromBusinessId(),
            request.getToBusinessId(),
            transferred)));
  }

  @GetMapping("/{tripId}/export")
  public ResponseEntity<byte[]> exportTripSettlement(@PathVariable Long tripId,
                                                     @RequestParam(defaultValue = "csv") String format) {
    ProcurementHudResponse hud = procurementHudService.getHudStats(tripId);
    List<TripExpenseResponse> expenses = procurementHudService.getTripExpenses(tripId);
    String csvContent = buildSettlementCsv(hud, expenses);
    if ("pdf".equalsIgnoreCase(format)) {
      byte[] pdfBytes = financialPdfService.buildTripSettlementPdf(hud, expenses);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-" + tripId + "-settlement.pdf")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdfBytes);
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-" + tripId + "-settlement.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csvContent.getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping("/{tripId}/ledger")
  public ResponseEntity<ApiResponse<List<CustomerBalanceLedgerResponse>>> customerLedger(@PathVariable Long tripId,
                                                                                          Authentication authentication) {
    List<CustomerBalanceLedgerResponse> entries = procurementHudService.getCustomerBalanceLedger(
        tripId,
        authentication);
    return ResponseEntity.ok(ApiResponse.success(entries, new ApiMeta(entries.size())));
  }

  @PostMapping("/{tripId}/ledger/{businessId}/confirm")
  public ResponseEntity<ApiResponse<CustomerBalanceLedgerResponse>> confirmCustomerLedger(@PathVariable Long tripId,
                                                                                          @PathVariable String businessId,
                                                                                          @RequestBody LedgerConfirmationRequest request,
                                                                                          Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(
        procurementHudService.confirmCustomerLedger(tripId, businessId, request, authentication)));
  }

  @PostMapping("/{tripId}/payments")
  public ResponseEntity<ApiResponse<CustomerPaymentRecordResponse>> recordOfflinePayment(@PathVariable Long tripId,
                                                                                         @RequestBody CustomerPaymentRecordRequest request,
                                                                                         Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.recordOfflinePayment(tripId, request, authentication)));
  }

  @GetMapping("/{tripId}/ledger/{businessId}/payments")
  public ResponseEntity<ApiResponse<List<CustomerPaymentRecordResponse>>> tripCustomerPayments(@PathVariable Long tripId,
                                                                                                @PathVariable String businessId) {
    List<CustomerPaymentRecordResponse> items = procurementHudService.getTripCustomerPayments(tripId, businessId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @GetMapping("/{tripId}/picking")
  public ResponseEntity<ApiResponse<List<PickingChecklistResponse>>> pickingChecklist(@PathVariable Long tripId) {
    List<PickingChecklistResponse> items = procurementHudService.getPickingChecklist(tripId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @PatchMapping("/{tripId}/picking/{businessId}")
  public ResponseEntity<ApiResponse<PickingChecklistResponse>> updatePickingChecklist(@PathVariable Long tripId,
                                                                                      @PathVariable String businessId,
                                                                                      @RequestBody PickingChecklistUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.updatePickingChecklistItem(tripId, businessId, request)));
  }

  @GetMapping("/{tripId}/delivery-preparations")
  public ResponseEntity<ApiResponse<List<DeliveryPreparationResponse>>> deliveryPreparations(@PathVariable Long tripId) {
    List<DeliveryPreparationResponse> items = procurementHudService.getDeliveryPreparations(tripId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @GetMapping("/{tripId}/delivery-preparations/export")
  public ResponseEntity<byte[]> exportDeliveryPreparations(@PathVariable Long tripId) {
    List<DeliveryPreparationResponse> items = procurementHudService.getDeliveryPreparations(tripId);
    StringBuilder builder = new StringBuilder();
    builder.append("businessId,customerId,customerName,deliveryStatus,addressSummary,contactName,contactPhone,latitude,longitude,totalPickItems,pickedItems\n");
    for (DeliveryPreparationResponse item : items) {
      builder.append(csvSafe(item.getBusinessId())).append(",")
          .append(item.getCustomerId()).append(",")
          .append(csvSafe(item.getCustomerName())).append(",")
          .append(csvSafe(item.getDeliveryStatus())).append(",")
          .append(csvSafe(item.getAddressSummary())).append(",")
          .append(csvSafe(item.getContactName())).append(",")
          .append(csvSafe(item.getContactPhone())).append(",")
          .append(item.getLatitude() == null ? "" : item.getLatitude()).append(",")
          .append(item.getLongitude() == null ? "" : item.getLongitude()).append(",")
          .append(item.getTotalPickItems()).append(",")
          .append(item.getPickedItems()).append("\n");
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-" + tripId + "-delivery-preparations.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(builder.toString().getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping("/customers/{customerId}/balance")
  public ResponseEntity<ApiResponse<CustomerBalanceSummaryResponse>> customerBalance(@PathVariable Long customerId) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.getCustomerBalanceSummary(customerId)));
  }

  @GetMapping("/customers/{customerId}/ledger-history")
  public ResponseEntity<ApiResponse<List<CustomerPaymentRecordResponse>>> customerLedgerHistory(@PathVariable Long customerId) {
    List<CustomerPaymentRecordResponse> items = procurementHudService.getCustomerLedgerHistory(customerId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @GetMapping("/{tripId}/audit-logs")
  public ResponseEntity<ApiResponse<List<FinancialAuditLogResponse>>> financialAuditLogs(@PathVariable Long tripId) {
    List<FinancialAuditLogResponse> logs = procurementHudService.getFinancialAuditLogs(tripId);
    return ResponseEntity.ok(ApiResponse.success(logs, new ApiMeta(logs.size())));
  }

  @GetMapping("/{tripId}/customers/{businessId}/statement")
  public ResponseEntity<byte[]> exportCustomerStatement(@PathVariable Long tripId,
                                                        @PathVariable String businessId) {
    OrderHeader order = procurementHudService.getTripOrderByBusinessId(tripId, businessId);
    byte[] pdfBytes = financialPdfService.buildCustomerStatementPdf(order);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-" + tripId + "-customer-" + businessId + "-statement.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdfBytes);
  }

  @PostMapping("/{tripId}/finalize-settlement")
  public ResponseEntity<ApiResponse<Void>> finalizeSettlement(@PathVariable Long tripId) {
    procurementHudService.finalizeTripSettlement(tripId);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @GetMapping("/{tripId}/receipts")
  public ResponseEntity<ApiResponse<List<ProcurementReceiptResponse>>> procurementReceipts(@PathVariable Long tripId) {
    List<ProcurementReceiptResponse> items = procurementHudService.getProcurementReceipts(tripId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @PostMapping("/{tripId}/receipts")
  public ResponseEntity<ApiResponse<List<ProcurementReceiptResponse>>> uploadProcurementReceipts(@PathVariable Long tripId,
                                                                                                  @RequestBody ProcurementReceiptUploadRequest request,
                                                                                                  Authentication authentication) {
    List<ProcurementReceiptResponse> items = procurementHudService.uploadProcurementReceipts(tripId, request, authentication);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  @PatchMapping("/{tripId}/receipts/{receiptId}")
  public ResponseEntity<ApiResponse<ProcurementReceiptResponse>> saveProcurementReceiptReconciliation(@PathVariable Long tripId,
                                                                                                       @PathVariable Long receiptId,
                                                                                                       @RequestBody ProcurementReceiptSaveRequest request,
                                                                                                       Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(
            procurementHudService.saveProcurementReceiptReconciliation(tripId, receiptId, request, authentication)));
  }

  @PostMapping("/{tripId}/receipts/{receiptId}/re-recognize")
  public ResponseEntity<ApiResponse<ProcurementReceiptResponse>> rerecognizeProcurementReceipt(@PathVariable Long tripId,
                                                                                               @PathVariable Long receiptId,
                                                                                               Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(
        procurementHudService.rerecognizeProcurementReceipt(tripId, receiptId, authentication)));
  }

  @GetMapping("/wallets/{partnerId}")
  public ResponseEntity<ApiResponse<WalletSummaryResponse>> wallet(@PathVariable String partnerId) {
    return ResponseEntity.ok(ApiResponse.success(procurementHudService.getWalletSummary(partnerId)));
  }

  @GetMapping("/wallets/{partnerId}/transactions")
  public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> walletTransactions(@PathVariable String partnerId) {
    List<WalletTransactionResponse> items = procurementHudService.getWalletTransactions(partnerId);
    return ResponseEntity.ok(ApiResponse.success(items, new ApiMeta(items.size())));
  }

  private String buildSettlementCsv(ProcurementHudResponse hud, List<TripExpenseResponse> expenses) {
    StringBuilder builder = new StringBuilder();
    builder.append("tripId,currentFxRate,referenceFxRate,currentPurchasedAmount,totalTripExpenses,totalEstimatedProfit\n");
    builder.append(hud.getTripId()).append(",")
        .append(hud.getCurrentFxRate()).append(",")
        .append(hud.getReferenceFxRate()).append(",")
        .append(hud.getCurrentPurchasedAmount()).append(",")
        .append(hud.getTotalTripExpenses()).append(",")
        .append(hud.getTotalEstimatedProfit()).append("\n\n");
    builder.append("expenseId,category,cost,createdAt\n");
    for (TripExpenseResponse expense : expenses) {
      builder.append(expense.getId()).append(",")
          .append(csvSafe(expense.getCategory())).append(",")
          .append(expense.getCost()).append(",")
          .append(expense.getCreatedAt()).append("\n");
    }
    return builder.toString();
  }

  private String csvSafe(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
