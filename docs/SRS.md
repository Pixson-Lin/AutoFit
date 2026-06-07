# Android Background Execution Lab
## Software Requirements Specification (SRS)
### Version 0.2 Draft

| 項目 | 內容 |
|------|------|
| Project Name | Android Background Execution Lab |
| Document Type | Software Requirements Specification (SRS) |
| Version | 0.1 Draft |
| Author | Pixson |
| Date | 2026-06-06 |
| Status | Draft |

---

# 1. Introduction

## 1.1 Purpose

本專案旨在建立一套可重複執行、可量測、可比較的 Android 實驗平台，用於研究不同 Android 版本、不同 OEM 客製化環境下的背景執行行為。

本系統以 Health Connect 步數寫入作為實際工作負載（Workload），透過 Foreground Service 持續執行步數產生任務，同時記錄系統狀態與執行結果，以分析 Android 系統對背景執行工作的影響。

本專案主要目標並非健康資料管理，而是研究：

- Android Background Execution Model
- Foreground Service Lifecycle
- Process Survival Behavior
- Doze Mode Impact
- Battery Optimization Impact
- OEM Customization Impact
- Android Version Compatibility

---

## 1.2 Scope

本系統將提供：

### 實驗配置能力

使用者可設定：

- 基準步頻（Steps Per Minute）
- 隨機步頻變動範圍
- 執行時間

---

### 背景執行能力

系統將：

- 啟動 Foreground Service
- 維持長時間執行
- 顯示 Notification
- 週期性產生步數資料
- 顯示在其他應用程式上層(Display over other apps)

---

### Health Connect 整合

系統將：

- 寫入 StepsRecord
- 記錄寫入結果
- 統計成功與失敗次數

---

### Observability（可觀測性）

系統將：

- 記錄 Heartbeat
- 記錄系統狀態
- 記錄執行結果
- 保留歷史實驗資料

---

### Compatibility Testing

支援於不同 Android 版本上執行，以比較：

- Android 12
- Android 15
- Android 16

以及不同 Samsung 裝置上的差異行為。

---

## 1.3 Definitions

### Experiment

一次完整的測試執行。

例如：

```text
120 SPM
±15 Random
60 Minutes
```

---

### Heartbeat

系統定期記錄的執行狀態樣本。

用於判斷：

- Service 是否持續運作
- Process 是否被終止
- 執行期間是否出現中斷

---

### Health Write Event

一次 Health Connect 寫入嘗試，對應一個已結束的 1 分鐘步數區間。
留存 stepCount、success、errorMessage、寫入時刻 timestamp，以及該區間的 recordStart / recordEnd。

---

### Retrospective Write（回溯寫入）

StepsRecord 僅在其時間區間「已結束」後才寫入（endTime ≤ 寫入當下），不寫入尚未發生的未來區間。

---

### Batch Write（批次寫入）

延遲並集中 Health Connect 寫入時機的實驗參數（batchMinutes）。
當 tick 編號 k（k > 0）滿足 k mod batchMinutes = 0 時，於單一 insertRecords 呼叫一次寫入該批多筆 1 分鐘紀錄。
若實驗結束時仍有未觸發批次的已結束分鐘（例如 batch=5、duration=3），由 FR-008 completion flush 一次寫出。
批次僅改變「寫入時機與 IPC 次數」，不改變紀錄粒度（仍 1 分鐘 1 筆），不做步數加總。

---

### Tick（實驗時脈）

以 experiment.startTime 為 T0 的離散時脈：

- **tick 0**（T0）：實驗開始，記錄 Heartbeat（generatedSteps = 0），不產生步數、不寫入 HC。
- **tick k**（T0 + k·min，k ≥ 1）：第 k 個模擬分鐘剛結束，產生該格步數並記錄 Heartbeat。
- 第 k 格名義區間為 `[startTime+(k−1)min, startTime+k·min)`。
- durationMinutes 表示步數模擬分鐘數（tick 1…durationMinutes）；實驗內 Heartbeat 共 durationMinutes + 1 筆（含 tick 0）。

---

### Experiment Result

一次實驗的最終統計結果。

---

# 2. System Overview

## 2.1 System Goal

建立 Android 背景執行研究平台。

驗證以下問題：

### Q1

Foreground Service 是否能夠穩定存活？

### Q2

不同 Android 版本是否有行為差異？

### Q3

Samsung Battery Optimization 是否影響執行？

### Q4

Doze Mode 是否造成執行中斷？

### Q5

Health Connect 寫入是否穩定？

---

## 2.2 High-Level Architecture

```text
┌──────────────────────────────┐
│         MainActivity         │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│          ViewModel           │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│          Repository          │
└───────┬─────────────┬────────┘
        │             │
        ▼             ▼
┌────────────┐   ┌────────────┐
│    Room    │   │HealthConnect│
└────────────┘   └────────────┘
        ▲
        │
┌───────┴─────────────────────┐
│     Foreground Service      │
└─────────────────────────────┘
```

---

# 3. Functional Requirements

## FR-001 Create Experiment

### Description

使用者可建立新的實驗。

### Inputs

- Target Cadence
- Random Range
- Duration
- Write Batch Size（batchMinutes，可選 1 / 3 / 5）

### Outputs

建立實驗配置。

---

## FR-002 Start Experiment

### Description

使用者可啟動實驗。

### Trigger

Start Button

### Expected Result

- 啟動 Foreground Service
- 建立 Experiment Instance
- 開始 Heartbeat

---

## FR-003 Generate Step Data

### Description

系統週期性產生步數。

### Formula

```text
Generated Steps
=
Target Cadence
+
Random Offset
```

### Example

```text
Target = 120

Random Range = ±15

Result:
105
112
121
134
118
```

### Timing

- tick 0（T0）：不產生步數。
- tick k（T0 + k·min，k ≥ 1）：於該分鐘**結束後**產生（lazy）第 k 格步數，數值僅由 targetCadence、randomRange 與 seed 決定。
- 第 k 格名義區間為 `[startTime+(k−1)min, startTime+k·min)`。

---

## FR-004 Write Health Connect Data

### Description

系統以「回溯寫入」方式將步數寫入 Health Connect。

### Rules

- 粒度：1 分鐘 1 筆 StepsRecord，不做加總。
- 回溯：每筆紀錄的時間區間必須已結束（endTime ≤ 寫入當下），不得寫入未來區間。
- 區間錨點：以 experiment.startTime 為基準，按分鐘遞增、不重疊。
- 批次：依 batchMinutes（1/3/5）延遲寫入；當 tick k（k > 0）滿足 k mod batchMinutes = 0 時，以單一 insertRecords 呼叫一次寫入該批多筆 1 分鐘紀錄（降低 IPC 次數）；未整批觸發的尾端分鐘於 FR-008 completion flush。
- 留存：每筆紀錄寫入一筆 HealthWriteEvent（含 recordStart / recordEnd）；同一批共用該次寫入結果（success / errorMessage）。
- Fail-open：寫入失敗不中斷實驗，仍記錄 success=false。

### Data Type

StepsRecord（每分鐘一筆）

### Expected Result

已結束之各分鐘步數成功寫入，且 HC 中無未來區間紀錄。

---

## FR-005 Heartbeat Logging

### Description

系統每分鐘記錄一次 Heartbeat。

### Data Collected

- Timestamp
- Experiment ID
- Generated Steps
- Battery Level
- Screen State
- Charging State

### Timing

- tick 0（T0）：記錄 Heartbeat（generatedSteps = 0），標記實驗開始。
- tick k（k ≥ 1）：於 T0 + k·min 記錄該格剛產生的 generatedSteps。
- Heartbeat 不隨 Health Connect 批次/回溯寫入延遲（兩者語意不同，允許相差一個以上 tick）。

---

## FR-006 Notification Update

### Description

系統持續更新 Notification。

### Information Displayed

- Running Status
- Total Steps
- Remaining Time
- Current Experiment ID

---

## FR-007 Stop Experiment

### Description

使用者可手動停止實驗。

### Expected Result

- 停止 Service
- 不 flush 尚未寫出的分鐘（丟棄未完成批次，避免干擾實驗結果）
- 聚合並儲存結果（僅計入已成功寫入的分鐘）
- 更新 UI

---

## FR-008 Experiment Completion

### Description

達到設定時間後自動結束。

### Expected Result

- 停止產生步數
- flush 所有已結束、尚未寫出的分鐘後，完成資料寫入
- 聚合並產生實驗報告

---

## FR-009 View Experiment History

### Description

使用者可查看歷史紀錄。

### Information

- Start Time
- End Time
- Duration
- Total Steps
- Success Rate

---

## FR-010 Environment Assessment

### Description

系統應能檢查：

### Information

- Battery Optimization
- Power Save Mode
- Charging State
- Notification Permission
- Health Connect Permission

---

## FR-012 Settings Navigation

### Description

系統應提供快速入口：

### Information

- Battery Optimization Settings
- Application Details Settings
- Notification Settings
- Health Connect Settings

---

# 4. Non-Functional Requirements

## NFR-001 Reliability

系統應能在以下情況持續執行：

- Screen Off
- App Backgrounded
- Device Locked

---

## NFR-002 Repeatability

相同參數應能重複執行測試。

例如：

```text
120 SPM
60 Minutes
```

可多次執行並比較結果。

---

## NFR-003 Observability

所有重要事件皆需被記錄。

包含：

- Service Start
- Service Stop
- Heartbeat
- Health Connect Write
- Errors

---

## NFR-004 Compatibility

支援：

- Android 12+
- Health Connect Supported Devices

---

## NFR-005 Maintainability

架構採用：

- MVVM
- Repository Pattern
- Single Source of Truth

---

# 5. Use Cases

## UC-001 Run Experiment

### Actor

User

### Flow

```text
Open App
    ↓
Configure Parameters
    ↓
Press Start
    ↓
Foreground Service Starts
    ↓
Experiment Running
```

---

## UC-002 Screen Off Test

### Actor

User

### Flow

```text
Start Experiment
    ↓
Turn Screen Off
    ↓
Wait
    ↓
Observe Result
```

---

## UC-003 Recent Task Removal

### Actor

User

### Flow

```text
Start Experiment
    ↓
Swipe App Away
    ↓
Observe Service Survival
```

---

## UC-004 Battery Saver Test

### Actor

User

### Flow

```text
Start Experiment
    ↓
Enable Battery Saver
    ↓
Observe Result
```

---

# 6. Data Model

## Experiment

| Field | Type |
|---------|---------|
| id | UUID |
| startTime | Instant |
| durationMinutes | Int |
| targetCadence | Int |
| randomRange | Int |
| batchMinutes | Int |
| status | Enum |

---

## Heartbeat

| Field | Type |
|---------|---------|
| id | UUID |
| experimentId | UUID |
| timestamp | Instant |
| generatedSteps | Int |
| batteryLevel | Int |
| screenOn | Boolean |
| charging | Boolean |

---

## HealthWriteEvent

| Field | Type |
|---------|---------|
| id | UUID |
| experimentId | UUID |
| timestamp | Instant |
| recordStart | Instant |
| recordEnd | Instant |
| stepCount | Int |
| success | Boolean |
| errorMessage | String |

---

## ExperimentResult

| Field | Type |
|---------|---------|
| experimentId | UUID |
| totalSteps | Int |
| heartbeatCount | Int |
| writeSuccessCount | Int |
| writeFailureCount | Int |
| actualDuration | Int |

---

## EnvironmentSnapshot

| Field | Type |
|---------|---------|
| experimentId | UUID |
| deviceModel | String |
| manufacturer | String |
| androidVersion | String |
| batteryOptimization | Boolean |
| powerSaveMode | Boolean |
| charging | Boolean |
| batteryLevel | Int |
| notificationPermission | Int |
| healthConnectPermission | Int |

---

# 7. Experiment Design

## Test Case A

### Foreground Service Survival

Condition:

```text
120 SPM
60 Minutes
Screen Off
```

Observe:

```text
Service Alive?
Heartbeat Continuous?
```

---

## Test Case B

### Battery Saver Impact

Condition:

```text
120 SPM
60 Minutes
Battery Saver Enabled
```

Observe:

```text
Missed Heartbeats
Service Restart
```

---

## Test Case C

### Recent Task Removal

Condition:

```text
Swipe App Away
```

Observe:

```text
Service Still Running?
```

---

## Test Case D

### Device Reboot

Condition:

```text
Reboot During Experiment
```

Observe:

```text
Experiment Lost?
Recovered?
```

---

# 8. Future Enhancements

## Phase 2

### Cloud Synchronization

可能整合：

- Firebase
- Google Drive

---

### Multi-Device Dashboard

比較：

- Android Version
- Device Model
- OEM Behavior

---

### Export Function

支援：

- CSV
- JSON

---

### Grafana Integration

將實驗資料匯出至：

- InfluxDB
- Grafana

進行長期分析。

---

# 9. Success Criteria

專案完成時應具備：

- 可設定實驗參數
- 可執行 Foreground Service
- 可寫入 Health Connect
- 可記錄 Heartbeat
- 可查看歷史紀錄
- 可比較不同 Android 裝置結果

本專案成功後，可作為 Android Background Execution Behavior 的研究平台與教學案例。