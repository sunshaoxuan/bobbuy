# GAP-01: 前端 UI 层技术负债清单

**记录日期**: 2026-01-20
**状态**: 待处理 (Pending)
**负责人**: Antigravity

## 1. 概述

本文件记录了在 BobBuy 项目 UI 设计与原型开发阶段发现的、需在后期优化的技术负债。重点关注多语言 (I18n) 架构的不一致性、硬编码问题以及设计原型与 React 业务代码的同步差距。

## 2. 负债明细 (Debt Backlog)

### 2.1 多语言架构分裂 (I18n Architecture Fragmentation)
- **描述**: 
  - 后端使用 `messages.properties` (Standard Spring)。
  - 设计原型使用 `i18n.json` + `ui-core.js`。
  - 前端 React 代码使用硬编码在 `i18n.tsx` 中的字典对象。
- **影响**: 翻译资源无法共享，增加维护成本，且不符合 `STD-07` 全局 I18n 优先原则。
- **建议修复**: 统一提取为后端管理的动态配置，或共享独立的 JSON 资源文件。

### 2.2 前端代码中的硬编码字符 (Hardcoded Text in Code)
- **描述**: `frontend/src/i18n.tsx` 中包含了大量的翻译字串原文。
- **影响**: 代码库过于臃肿，不便于非技术人员（如翻译、产品）直接编辑。
- **建议修复**: 将 `translations` 字典外置为资源文件，代码中仅保留 Key。

### 2.3 设计原型与实际代码的视觉差距 (Design-Code Sync Gap)
- **描述**: 
  - [设计原型](file:///c:/workspace/bobbuy/docs/design/pc/UID0000_all_login.html) 已经实现了基于 Tailwind 的富内容落地页、3度旋转 Logo 和平滑动画。
  - [前端 React App](file:///c:/workspace/bobbuy/frontend/src/App.tsx) 仍停留在基于 Ant Design 的基础布局（Default White Style），未应用最新的“砖红+暖白”品牌视觉系统。
- **影响**: 业务系统视觉体验滞后于设计标准，品牌一致性较差。
- **建议修复**: UI 设计阶段结束后，执行全量 UI 刷新 (UI Refurbishment)，将原型组件转化为 React/Tailwind 组件。

### 2.4 API 错误消息与 UI 标签解耦不足
- **描述**: 前端 `api.ts` 中存在临时处理 `Generic Error` 的逻辑，且对后端返回的消息处理不够优雅。
- **建议修复**: 建立标准化的 `ErrorCode-to-I18nKey` 映射机制。

## 3. 处理计划

1.  **UI 设计阶段**: 继续完善 HTML 原型及视觉规范。
2.  **联调启动前**: 优先解决 2.3 (视觉刷新)。
3.  **前后端联调期**: 彻底解决 2.1 和 2.2 (I18n 统一)。

---
*记录人: Antigravity*
