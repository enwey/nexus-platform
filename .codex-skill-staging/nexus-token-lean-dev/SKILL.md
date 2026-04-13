---
name: nexus-token-lean-dev
description: 當任務發生在 nexus-platform repo，且使用者希望更省 token 地開發、除錯或定位問題時使用。這個 skill 會優先限制搜尋範圍、避開大型產物與無關模組，並根據 dev-portal、ops-portal、mock-sdk、backend、android-client、ios-client 的結構做最小化探索與驗證。
---

# Nexus Token Lean Dev

這是 `nexus-platform` 專用的低 token 工作模式。把 repo 視為多子系統單體倉，先判斷落點，再只讀那一塊。

## Repo 快速分區

- `dev-portal/`：前端入口之一。
- `ops-portal/`：後台或營運前端。
- `mock-sdk/`：前端 SDK 或模擬層。
- `backend/`：Maven/Spring 後端。
- `android-client/`：Android app。
- `ios-client/`：iOS app。
- `contracts/`、`docs/`：契約與文件，只有在需求直接依賴時才讀。

## 先做分類

收到任務後先判斷它屬於哪一類，再限制搜尋路徑：

1. Web UI 問題：先查 `dev-portal/src`、`ops-portal/src`、`mock-sdk/src`。
2. 後端 API 或資料問題：先查 `backend/src` 與需要時的 `contracts/`。
3. Android 問題：先查 `android-client/app/src/main`。
4. iOS 問題：先查 `ios-client/` 內對應 app 目錄。
5. 只有文件或規格問題：先查 `docs/` 與對應模組，不要先掃整個 repo。

## 搜尋規則

- 永遠先用 `rg` 限制目錄，再讀檔。
- 不預設搜尋整個 repo；除非第一輪找不到，再逐步擴大。
- 避開這些路徑，除非它們正是問題來源：
  - `**/node_modules/**`
  - `backend/target/**`
  - `ops-portal/dist/**`
  - `android-client/.gradle/**`
  - `.git/**`
- 若需求與執行環境相關，先看 `.env`、`.env.example`、`docker-compose/docker-compose.yml`，但只讀命中區塊。

## 讀檔順序

1. 先看入口檔或引用點。
2. 再看被呼叫的實作。
3. 最後才看相鄰檔案或跨模組依賴。

不要反過來從底層大面積展開。

## 常用最小驗證

- `dev-portal`、`ops-portal`、`mock-sdk`：優先跑單一測試、單一 build、或目標檔案相關 lint。
- `backend`：優先跑單一測試類別或最小編譯檢查。
- `android-client`：預設不主動編譯、打包、安裝或跑完整測試；只有使用者明確要求時才執行。
- `ios-client`：預設不主動編譯、打包、安裝或跑完整測試；只有使用者明確要求時才執行。

## 跨模組任務規則

如果任務跨前後端或跨 SDK/客戶端：

1. 先確認真正的契約面。
2. 只讀兩端與契約直接相連的檔案。
3. 不要因為跨模組就把每一層都完整打開。

## 回報格式

保持短而有用：

- 任務落點
- 目前假設
- 修改範圍
- 驗證結果
- 剩餘風險

每一項只保留最需要決策的資訊。

## 內部工作語句

- 「落點已確定：先只看 `backend/src`。」
- 「這次只需要 portal 端與 SDK 契約，不擴到 Android。」
- 「先證明失敗點，再決定要不要擴到下一層。」
