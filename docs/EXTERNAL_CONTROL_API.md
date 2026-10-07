# DiPlay 外部调用链与第三方车载桌面（Launcher）集成指南

本指南详细介绍如何通过 **广播（Broadcast）** 与 **Activity 路由链（Trampoline Activity）** 对 DiPlay 进行全生命周期控制，包括：后台静默连接、前台唤起、断开连接、状态监听与冷启动唤醒。

---

## 目录

1. [架构设计与选型原则](#1-架构设计与选型原则)
2. [广播控制通道（Broadcast API）](#2-广播控制通道broadcast-api)
3. [状态监听广播（Status Receiver）](#3-状态监听广播status-receiver)
4. [Activity 路由通道（冷启动唤醒）](#4-activity-路由通道冷启动唤醒)
5. [最佳实践：第三方 Launcher 智能连接方案](#5-最佳实践第三方-launcher-智能连接方案)
6. [完整代码示例（Kotlin）](#6-完整代码示例kotlin)
7. [完整代码示例（Java）](#7-完整代码示例java)
8. [ADB 调试命令大全](#8-adb-调试命令大全)

---

## 1. 架构设计与选型原则

为了确保第三方桌面在任何场景下都能稳定控制 DiPlay，同时避免不必要的界面抢焦与闪烁，DiPlay 提供了双调用通道：

| 通道 | 触发方式 | 适用场景 | 优势与特点 |
|---|---|---|---|
| **广播通道 (Broadcast)** | `sendBroadcast()` | **热状态控制**（DiPlay 进程在后台存活时） | 零界面开销、完全不抢占前台 Launcher 焦点、纯后台执行 |
| **Activity 路由通道** | `startActivity()` | **冷启动唤醒**（DiPlay 进程被杀死、刚开机） | Android 系统保证无条件拉起目标进程并脱离 `STOPPED` 状态 |

> **⚠️ 为什么冷启动不能仅依赖广播？**  
> 当 DiPlay 在多任务列表中被划掉或被系统杀死后，Android 会给其打上 `STOPPED` 冻结标志。为了防止恶意后台自启，Android 系统底层机制规定：**普通跨应用广播无法唤醒处于 `STOPPED` 状态的进程**。此时必须通过显式调用 Activity 来触发系统的用户交互级冷启动。

---

## 2. 广播控制通道（Broadcast API）

目标包名限制：`com.shihab.diplay`

### 2.1 发起连接 (`ACTION_CONNECT`)
- **Action**: `com.shihab.diplay.action.CONNECT`
- **Extras**:
  - `silent` (*Boolean*, 可选, 默认 `false`): 是否启用后台静默连接。若为 `true`，连接握手与音视频协商全在后台进行，**不会弹到前台抢夺当前桌面焦点**。
  - `fast_rfcomm` (*Boolean*, 可选, 默认 `true`): 是否启用跳过 SDP 的 Root 极速直连。

### 2.2 断开连接 (`ACTION_DISCONNECT`)
- **Action**: `com.shihab.diplay.action.DISCONNECT`
- **说明**: 主动终止当前活跃的 CarPlay 会话并重置连接状态。

### 2.3 打开 CarPlay 界面 (`ACTION_OPEN_CARPLAY`)
- **Action**: `com.shihab.diplay.action.OPEN_CARPLAY`
- **说明**: 将已在后台运行或静默连通的 CarPlay 全屏画面切换到前台显示。

### 2.4 查询当前状态 (`ACTION_GET_STATUS`)
- **Action**: `com.shihab.diplay.action.GET_STATUS`
- **说明**: 向 DiPlay 请求当前状态回执，DiPlay 接收后会立即通过 `STATUS_CHANGED` 广播回传当前状态。

---

## 3. 状态监听广播（Status Receiver）

外部桌面可动态注册接收器以监听 DiPlay 的实时连接状态。

- **Action**: `com.shihab.diplay.action.STATUS_CHANGED`
- **Extras 数据字典**:

| Key | 类型 | 说明 | 取值范围 / 示例 |
|---|---|---|---|
| `status` | `String` | 当前连接状态大类 | `"IDLE"`, `"CONNECTING"`, `"CONNECTED"`, `"DISCONNECTED"` |
| `stage` | `String` | 细粒度协议进度阶段描述 | `"正在跳过 SDP 直连 iPhone 蓝牙 (RFCOMM)..."`, `"车机 Wi-Fi 热点已就绪，等待 iPhone 连接..."`, `"iPhone 正在接入车载 Wi-Fi..."`, `"Wi-Fi 通道已连通，正在建立 AirPlay 会话..."`, `"AirPlay 会话就绪，正在渲染画面..."`, `"CarPlay 已连接"` |
| `is_wireless` | `Boolean` | 是否为无线连接 | `true` (无线) / `false` (USB 有线) |
| `silent` | `Boolean` | 当前会话是否在静默后台运行 | `true` / `false` |
| `phone_address` | `String?` | 绑定的 iPhone 蓝牙 MAC 地址 | `"XX:XX:XX:XX:XX:XX"` |
| `timestamp` | `Long` | 事件触发时间戳 (毫秒) | `System.currentTimeMillis()` |

---

## 4. Activity 路由通道（冷启动唤醒）

所有路由 Activity 均配置了以下特性：
- **完全透明**：`@android:style/Theme.Translucent.NoTitleBar`
- **零动画**：禁用窗口进出场缩放动画（`overridePendingTransition(0, 0)`）
- **无历史栈残留**：`android:noHistory="true"`、`excludeFromRecents="true"`
- **毫秒级转发生命周期**：在 `onCreate` 中分发指令后立即调用 `finish()` 销毁。

### 4.1 冷启动静默/前台连接
- **Component**: `ComponentName("com.shihab.diplay", "com.shilapi.xcertplay.api.ConnectActivity")`
- **Extras**: `silent` (*Boolean*), `fast_rfcomm` (*Boolean*)

### 4.2 显式拉起 CarPlay 画面
- **Component**: `ComponentName("com.shihab.diplay", "com.shilapi.xcertplay.api.OpenCarPlayActivity")`

### 4.3 显式断开连接
- **Component**: `ComponentName("com.shihab.diplay", "com.shilapi.xcertplay.api.DisconnectActivity")`

---

## 5. 最佳实践：第三方 Launcher 智能连接方案

在车载桌面中，推荐使用 **“智能双轨调用”** 策略：

```mermaid
flowchart TD
    Start([用户触发连接 / 开机自连]) --> SilentCheck{是否需要静默?}
    SilentCheck -->|是| CheckProcess[检查 DiPlay 进程或直接路由]
    CheckProcess --> LaunchConnectActivity[调用 ConnectActivity (silent=true)]
    LaunchConnectActivity --> DiPlayAlive[DiPlay 冷启动并自动退至后台]
    DiPlayAlive --> BackgroundConnect[后台完成蓝牙与 Wi-Fi 握手]
    BackgroundConnect --> BroadcastStatus[收到 STATUS_CHANGED = CONNECTED]
    BroadcastStatus --> UpdateCard[桌面卡片更新为: 已连接 iPhone]

    UpdateCard --> UserClick[用户点击桌面 CarPlay 卡片]
    UserClick --> OpenBroadcast[发送 OPEN_CARPLAY 广播]
    OpenBroadcast --> ShowCarPlay[CarPlay 瞬间全屏秒切，零等待]
```

由于 `ConnectActivity` 具备完全透明且瞬时销毁的特性：
- 当 DiPlay 进程已被杀：`ConnectActivity` 能够唤醒进程并在后台静默连好，桌面无感知。
- 当 DiPlay 进程存活时：`ConnectActivity` 毫秒级转发指令后立即销毁，同样无感知。
- 当进入日常使用时：打开全屏画面使用 `OPEN_CARPLAY` 广播，断开使用 `DISCONNECT` 广播。

---

## 6. 完整代码示例（Kotlin）

你可以将以下单例类直接复制到你的 Launcher 项目中：

```kotlin
package com.yourlauncher.diplay

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

object DiPlayClient {
    private const val DIPLAY_PACKAGE = "com.shihab.diplay"

    // 控制广播 Action
    private const val ACTION_CONNECT = "com.shihab.diplay.action.CONNECT"
    private const val ACTION_DISCONNECT = "com.shihab.diplay.action.DISCONNECT"
    private const val ACTION_OPEN_CARPLAY = "com.shihab.diplay.action.OPEN_CARPLAY"
    private const val ACTION_GET_STATUS = "com.shihab.diplay.action.GET_STATUS"

    // 状态接收 Action
    const val ACTION_STATUS_CHANGED = "com.shihab.diplay.action.STATUS_CHANGED"

    // 状态常量
    const val STATUS_IDLE = "IDLE"
    const val STATUS_CONNECTING = "CONNECTING"
    const val STATUS_CONNECTED = "CONNECTED"
    const val STATUS_DISCONNECTED = "DISCONNECTED"

    /**
     * 1. 注册状态变化监听
     */
    fun registerStatusListener(
        context: Context,
        onStatusChanged: (status: String, stage: String, isWireless: Boolean, silent: Boolean) -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val status = intent.getStringExtra("status") ?: STATUS_IDLE
                val stage = intent.getStringExtra("stage") ?: ""
                val isWireless = intent.getBooleanExtra("is_wireless", true)
                val silent = intent.getBooleanExtra("silent", false)
                onStatusChanged(status, stage, isWireless, silent)
            }
        }
        context.registerReceiver(receiver, IntentFilter(ACTION_STATUS_CHANGED))
        // 注册后立即查询一次当前状态
        queryStatus(context)
        return receiver
    }

    /**
     * 2. 注销监听器
     */
    fun unregisterStatusListener(context: Context, receiver: BroadcastReceiver) {
        runCatching { context.unregisterReceiver(receiver) }
    }

    /**
     * 3. 智能连接（推荐）：冷热通杀
     * @param silent true: 后台静默连; false: 直接拉起全屏
     * @param fastRfcomm true: 启用 Root 极速直连
     */
    fun connect(context: Context, silent: Boolean = true, fastRfcomm: Boolean = true) {
        val intent = Intent().apply {
            component = ComponentName(DIPLAY_PACKAGE, "com.shilapi.xcertplay.api.ConnectActivity")
            putExtra("silent", silent)
            putExtra("fast_rfcomm", fastRfcomm)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 4. 打开 CarPlay 全屏画面（热切）
     */
    fun openCarPlay(context: Context) {
        val intent = Intent(ACTION_OPEN_CARPLAY).apply {
            setPackage(DIPLAY_PACKAGE)
        }
        context.sendBroadcast(intent)
    }

    /**
     * 5. 断开连接
     */
    fun disconnect(context: Context) {
        val intent = Intent(ACTION_DISCONNECT).apply {
            setPackage(DIPLAY_PACKAGE)
        }
        context.sendBroadcast(intent)
    }

    /**
     * 6. 主动查询当前状态
     */
    fun queryStatus(context: Context) {
        val intent = Intent(ACTION_GET_STATUS).apply {
            setPackage(DIPLAY_PACKAGE)
        }
        context.sendBroadcast(intent)
    }
}
```

---

## 7. 完整代码示例（Java）

对于基于 Java 编写的车载桌面：

```java
package com.yourlauncher.diplay;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class DiPlayClientJava {
    private static final String DIPLAY_PACKAGE = "com.shihab.diplay";

    public static final String ACTION_STATUS_CHANGED = "com.shihab.diplay.action.STATUS_CHANGED";
    private static final String ACTION_CONNECT = "com.shihab.diplay.action.CONNECT";
    private static final String ACTION_DISCONNECT = "com.shihab.diplay.action.DISCONNECT";
    private static final String ACTION_OPEN_CARPLAY = "com.shihab.diplay.action.OPEN_CARPLAY";
    private static final String ACTION_GET_STATUS = "com.shihab.diplay.action.GET_STATUS";

    public interface StatusCallback {
        void onStatusChanged(String status, String stage, boolean isWireless, boolean isSilent);
    }

    public static BroadcastReceiver registerStatusReceiver(Context context, final StatusCallback callback) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String status = intent.getStringExtra("status");
                String stage = intent.getStringExtra("stage");
                boolean isWireless = intent.getBooleanExtra("is_wireless", true);
                boolean isSilent = intent.getBooleanExtra("silent", false);
                if (callback != null) {
                    callback.onStatusChanged(status != null ? status : "IDLE",
                                             stage != null ? stage : "",
                                             isWireless, isSilent);
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(ACTION_STATUS_CHANGED));
        queryStatus(context);
        return receiver;
    }

    public static void connect(Context context, boolean silent, boolean fastRfcomm) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(DIPLAY_PACKAGE, "com.shilapi.xcertplay.api.ConnectActivity"));
        intent.putExtra("silent", silent);
        intent.putExtra("fast_rfcomm", fastRfcomm);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void openCarPlay(Context context) {
        Intent intent = new Intent(ACTION_OPEN_CARPLAY);
        intent.setPackage(DIPLAY_PACKAGE);
        context.sendBroadcast(intent);
    }

    public static void disconnect(Context context) {
        Intent intent = new Intent(ACTION_DISCONNECT);
        intent.setPackage(DIPLAY_PACKAGE);
        context.sendBroadcast(intent);
    }

    public static void queryStatus(Context context) {
        Intent intent = new Intent(ACTION_GET_STATUS);
        intent.setPackage(DIPLAY_PACKAGE);
        context.sendBroadcast(intent);
    }
}
```

---

## 8. ADB 调试命令大全

车载系统工程师或开发者可通过 ADB 命令行直接发送测试指令：

### 8.1 广播测试
```bash
# 后台静默极速连接 (热状态)
adb shell am broadcast -a com.shihab.diplay.action.CONNECT -p com.shihab.diplay --ez silent true --ez fast_rfcomm true

# 前台全屏极速连接 (热状态)
adb shell am broadcast -a com.shihab.diplay.action.CONNECT -p com.shihab.diplay --ez silent false --ez fast_rfcomm true

# 切出 CarPlay 全屏
adb shell am broadcast -a com.shihab.diplay.action.OPEN_CARPLAY -p com.shihab.diplay

# 断开连接
adb shell am broadcast -a com.shihab.diplay.action.DISCONNECT -p com.shihab.diplay

# 查询当前连接状态
adb shell am broadcast -a com.shihab.diplay.action.GET_STATUS -p com.shihab.diplay
```

### 8.2 Activity 路由冷启动测试（即使进程被杀也能启动）
```bash
# 唤醒冷启动并后台静默连接
adb shell am start -n com.shihab.diplay/com.shilapi.xcertplay.api.ConnectActivity --ez silent true --ez fast_rfcomm true

# 唤醒冷启动并直接显示界面
adb shell am start -n com.shihab.diplay/com.shilapi.xcertplay.api.OpenCarPlayActivity
```
