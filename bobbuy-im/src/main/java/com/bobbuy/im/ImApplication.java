package com.bobbuy.im;

import com.bobbuy.api.AiAgentController;
import com.bobbuy.api.AuditLogController;
import com.bobbuy.api.BobbuyApplication;
import com.bobbuy.api.FinancialAuditController;
import com.bobbuy.api.HealthController;
import com.bobbuy.api.MetricsController;
import com.bobbuy.api.MobileCategoryController;
import com.bobbuy.api.MobileProductController;
import com.bobbuy.api.MobileSupplierController;
import com.bobbuy.api.OrderController;
import com.bobbuy.api.ProcurementController;
import com.bobbuy.api.TripController;
import com.bobbuy.api.UserController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.bobbuy.repository")
@EntityScan(basePackages = "com.bobbuy.model")
@ComponentScan(basePackages = "com.bobbuy",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AiAgentController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuditLogController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FinancialAuditController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = HealthController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MetricsController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MobileCategoryController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MobileProductController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MobileSupplierController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OrderController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ProcurementController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TripController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UserController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BobbuyApplication.class)
    })
public class ImApplication {
  public static void main(String[] args) {
    SpringApplication.run(ImApplication.class, args);
  }
}
