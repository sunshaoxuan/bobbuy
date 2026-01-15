# PLAN-03: 第一周开发任务优先级与实施计划

**制定日期**: 2026-01-15  
**状态**: 执行中  
**评估依据**: PLAN-00 看板 + AUDIT-01/04 未完成事项

---

## 1. 目标与背景

### 1.1 当前状态
- **PLAN-01 Sprint 1**: ✅ 已完成（70%）
- **PLAN-01 Sprint 2**: 🔄 进行中（40%）- 缺少前端流程按钮
- **PLAN-02**: ⏳ 待开始 - 测试覆盖率不足

### 1.2 未完成任务统计
- PLAN-01 Sprint 2: 5 个任务
- PLAN-01 Sprint 3: 已转至 PLAN-02
- PLAN-02: 全部未开始（4 个 Phase）
- 技术债务: 4/8 未解决

### 1.3 核心问题
需要平衡**业务价值**（用户可见功能）和**技术债务**（测试、并发控制）。

---

## 2. 任务优先级矩阵

| 任务 | 业务价值 | 紧急程度 | 阻塞性 | 工作量 | 综合优先级 |
|------|---------|---------|--------|--------|-----------|
| **前端流程按钮（状态流转）** | 高 | 中 | 低 | 1天 | **P0** |
| **测试基础设施配置** | 低 | 高 | 高 | 1天 | **P0** |
| **并发控制修复** | 高 | 高 | 低 | 0.5天 | **P0** |
| 后端单元测试补充 | 中 | 高 | 中 | 3天 | **P1** |
| 前端组件测试 | 中 | 高 | 中 | 2天 | **P1** |
| 前端日志与提醒 | 中 | 低 | 低 | 1天 | **P1** |
| 可观测性日志规范化 | 低 | 低 | 低 | 1天 | **P2** |
| Controller 集成测试 | 低 | 中 | 低 | 2天 | **P2** |

---

## 3. 第一周开发计划（2026-01-16 ~ 2026-01-19）

### Day 1（周四）- 并行启动三大任务

#### 前端工程师（8小时）
**任务 A: 订单状态流转按钮**（4小时）- P0

**文件**: `frontend/src/pages/Orders.tsx`

**实现要点**:
```tsx
// 在表格中添加操作列
{
  title: t('orders.table.actions'),
  render: (_: unknown, record: Order) => (
    <Select
      value={record.status}
      onChange={(newStatus) => handleStatusChange(record.id, newStatus)}
      options={statusOptions.map(s => ({ value: s, label: s }))}
      disabled={record.status === 'SETTLED'}
    />
  )
}

// 添加处理函数
const handleStatusChange = async (orderId: number, newStatus: string) => {
  try {
    await fetch(`/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    });
    message.success(t('orders.status.updated'));
    api.orders().then(setOrders);
  } catch {
    message.error(t('errors.request_failed'));
  }
};
```

**验收标准**:
- [ ] 订单列表每行显示状态下拉菜单
- [ ] 切换状态后调用 API
- [ ] 成功后显示提示并刷新列表
- [ ] 已结算订单禁用修改

**为什么优先**: 用户最需要的功能，完成后订单流程可以真正流转。

---

**任务 B: 配置 Vitest 测试框架**（4小时）- P0

**步骤**:
1. 安装依赖
```bash
npm install -D vitest @testing-library/react @testing-library/user-event @vitest/ui @vitest/coverage-v8 jsdom
```

2. 创建 `frontend/vitest.config.ts`
```typescript
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      exclude: ['node_modules/', 'src/test/'],
      branches: 60,
      functions: 60,
      lines: 60,
      statements: 60
    }
  }
});
```

3. 创建 `frontend/src/test/setup.ts`
```typescript
import { expect, afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';

expect.extend(matchers);

afterEach(() => {
  cleanup();
});
```

4. 更新 `package.json`
```json
"scripts": {
  "test": "vitest run",
  "test:watch": "vitest",
  "test:ui": "vitest --ui",
  "test:coverage": "vitest run --coverage"
}
```

**验收标准**:
- [ ] `npm test` 可运行
- [ ] `npm run test:coverage` 生成覆盖率报告

---

#### 后端工程师（8小时）
**任务 C: 并发控制修复**（3小时）- P0

**文件**: `backend/src/main/java/com/bobbuy/service/BobbuyStore.java`

**实现要点**:
```java
public synchronized Trip reserveTripCapacity(Long id, int quantity) {
  Trip trip = getTrip(id).orElseThrow(
    () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found")
  );
  
  if (quantity <= 0) {
    throw new ApiException(ErrorCode.INVALID_REQUEST, "error.trip.invalid_quantity");
  }
  
  int remaining = trip.getRemainingCapacity();
  if (remaining < quantity) {
    throw new ApiException(ErrorCode.CAPACITY_NOT_ENOUGH, "error.trip.capacity_not_enough");
  }
  
  trip.setReservedCapacity(trip.getReservedCapacity() + quantity);
  return trip;
}
```

**并发测试**: `backend/src/test/java/com/bobbuy/service/BobbuyStoreTest.java`
```java
@Test
void handlesConcurrentReservations() throws InterruptedException {
  ExecutorService executor = Executors.newFixedThreadPool(10);
  CountDownLatch latch = new CountDownLatch(10);
  AtomicInteger successCount = new AtomicInteger(0);
  
  for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
      try {
        store.reserveTripCapacity(2000L, 1);
        successCount.incrementAndGet();
      } catch (ApiException e) {
        // Expected for some threads
      } finally {
        latch.countDown();
      }
    });
  }
  
  latch.await();
  assertThat(successCount.get()).isLessThanOrEqualTo(5); // 容量只有5
}
```

**验收标准**:
- [ ] 方法添加 `synchronized` 关键字
- [ ] 并发测试通过

**为什么优先**: 容量超卖是生产事故级风险。

---

**任务 D: 配置 JaCoCo 覆盖率工具**（3小时）- P0

**文件**: `backend/pom.xml`

**修改内容**: 在 `<build><plugins>` 中添加
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.11</version>
  <executions>
    <execution>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
    <execution>
      <id>check</id>
      <phase>verify</phase>
      <goals>
        <goal>check</goal>
      </goals>
      <configuration>
        <rules>
          <rule>
            <element>PACKAGE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.50</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

**验收标准**:
- [ ] 执行 `mvn clean test` 生成 `target/site/jacoco/index.html`
- [ ] 覆盖率 <50% 时构建警告

---

**任务 E: 验证行程状态接口**（2小时）

**目标**: 确保 TripController 有类似订单的状态更新接口

**检查**:
```java
// 应该存在类似的接口
@PatchMapping("/{id}/status")
public ResponseEntity<ApiResponse<Trip>> updateStatus(
  @PathVariable Long id, 
  @Valid @RequestBody TripStatusRequest request
) { ... }
```

如不存在则补充。

---

### Day 2-3（周五-周六）- 测试补充

#### 前端工程师
- [ ] 行程状态变更按钮（2小时）
- [ ] 异常提醒与空态引导（3小时）
- [ ] Orders 组件测试（4小时）

#### 后端工程师
- [ ] BobbuyStore 订单 CRUD 测试（6个用例，4小时）
- [ ] BobbuyStore 状态流转测试（3个用例，2小时）
- [ ] AuditLogService 测试（3个用例，2小时）

---

### Day 4（周日）- 可选加班

根据进度决定是否继续测试补充。

---

## 4. 第一周预期成果

### Day 1 结束后
- ✅ 订单可以手动流转状态（业务可用！）
- ✅ 容量预订不会超卖（风险解除）
- ✅ 覆盖率可见（15% 基线）
- ✅ 测试可运行

### Day 3 结束后
- ✅ 订单和行程状态可流转
- ✅ 前端有基础提醒
- ✅ 覆盖率 35-40%

---

## 5. 第二周计划（2026-01-20 ~ 2026-01-23）

参考 PLAN-02 Phase 3-4：
- 前端组件测试完成
- 覆盖率达标（后端 ≥50%，前端 ≥60%）
- 生成验收报告

---

## 6. 风险与应对

| 风险 | 应对 |
|------|------|
| 测试编写慢于预期 | Day 1 优先业务功能，测试分散到整周 |
| 状态流转 UI 复杂 | 先做最简单的下拉菜单，后续优化 |
| 并发测试不稳定 | 至少保证功能正确，测试可后补 |

---

## 7. 交付物清单

### Day 1
- [ ] 前端 PR: "feat: add order status transition button"
- [ ] 后端 PR: "fix: add concurrency control to trip reservation"
- [ ] 配置 PR: "feat: add test coverage infrastructure"

### Day 3
- [ ] 测试 PR: "test: add unit tests for BobbuyStore and AuditLogService"
- [ ] 覆盖率报告截图

---

**负责人**: 全栈团队  
**评审人**: Tech Lead  
**截止日期**: 2026-01-19
