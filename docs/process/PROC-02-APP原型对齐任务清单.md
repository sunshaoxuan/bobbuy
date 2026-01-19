# PROC-02: APP原型对齐任务清单

**生效日期**: 2026-01-19
**状态**: 执行中

## 1. 编号与对齐审计 (ID & Alignment Audit)
- [x] 建立 PC 与 App 的 1:1 映射检查表
- [x] 重命名/重组 App 目录，确保 ID 严格对应 PC (UID1xxx <-> UID0xxx)
- [x] 删除剩余的旧版冗余文件 (UID1201, 1202, 1203)
- [x] 修正全量 App 原型内部的 `<h1>` 标题，确保包含“ID + 中文名称”

## 2. 链接完整性修复 (Link Integrity)
- [x] 修正 `UID1200` 中的 `UID1201` 废弃链接 -> `UID1210`
- [x] 遍历所有 HTML，通过 Grep 确保无 `UID1201/2/3` 残留引用
- [x] 确保商户端各环节 (New -> Processing -> Settlement) 链接闭环

## 3. 命名与语言合规 (Naming & Language Compliance)
- [x] 将所有 App 原型页面标题中的英文翻译为中文 (如 "Create Shopping Trip" -> "创建代购行程")
- [x] 确保所有文件遵循 `UID[ID]_[领域]_[名称].html` 的一致性


## 4. 品牌色二次核查 (Brand Color Final Audit)
- [x] 确认所有高保真 App 原型主色为 `#C14B3B`
- [x] 检查背景色是否统一为温润白 `#FAF7F2`
- [x] 确认 Logo 在所有页面加载路径 correct

## 5. 验收报告更新
- [x] 完成 `AUDIT-01-APP原型对齐验收报告`
