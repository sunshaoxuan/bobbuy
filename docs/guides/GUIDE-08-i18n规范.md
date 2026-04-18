# GUIDE-08: i18n 国际化规范

## 目标
- 系统统一支持 `zh-CN`、`ja-JP`、`en-US`
- 默认语言为 `zh-CN`
- 所有用户可见文案必须走词条资源，禁止在页面与接口返回中硬编码

## 前端规范
- 语言资源文件位于：
  - `frontend/src/locales/zh-CN.ts`
  - `frontend/src/locales/ja-JP.ts`
  - `frontend/src/locales/en-US.ts`
- 统一通过 `frontend/src/i18n.tsx` 中的 `t(key)` 取词
- `supportedLocales` 必须包含 `zh-CN`、`ja-JP`、`en-US`
- 新增页面或组件时：
  1. 先定义词条 key
  2. 三种语言同时补齐
  3. 组件中只保留 key，不保留硬编码文案
- 多语字段（商品名称、描述、媒体标题）统一使用 `L10nValues` 结构并提供三语值

## 后端规范
- 后端国际化资源文件位于：
  - `backend/src/main/resources/messages_zh_CN.properties`
  - `backend/src/main/resources/messages_ja_JP.properties`
  - `backend/src/main/resources/messages.properties`（英文默认）
- 所有接口错误信息统一使用 message key（`ApiException + MessageSource`）
- 禁止直接返回硬编码错误文案
- 通过 `Accept-Language` 协商语言，默认 `Locale.SIMPLIFIED_CHINESE`

## 开发检查清单
- [ ] 所有新增/修改文案是否已抽取为词条
- [ ] 三语资源是否都已补齐
- [ ] 默认语言是否仍为中文
- [ ] 前后端异常信息是否均可随语言切换
- [ ] 回归测试（前端 test/build、后端 test/package）是否通过
