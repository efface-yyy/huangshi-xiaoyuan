# 黄师校园

面向黄冈师范学院的非官方 Android 校园客户端，使用 Java 原生界面独立实现，包名为 `com.smart.hgnu`，可与官方客户端并存。

> **“服务”功能尚未适配完毕。** 当前提供服务目录、搜索和 App 内网页入口，已接入官方轻应用登录跳板；部分服务会显示无访问权限，其他原生模块、独立应用跳转及扫码、定位等网页桥接能力尚未接入。目录中存在某项服务，不代表该账号可以使用它。

## 功能

- 校园账号登录；在手机端完成密钥协商及加密请求，不内置任何账号或密码。
- 使用 Android Keystore + AES-GCM 保存必要会话；重启 App 后自动恢复，退出登录时清理本机会话及网页存储，不保存登录密码。
- 启动时同步校园资料、服务目录、学期和课表；支持下拉刷新及“我的 → 重新同步数据”，网络异常使用气泡提示。
- 根据服务器校历与服务器时间计算当前教学周，默认显示本周，支持学期、周数选择和返回本周。
- 课表支持周表格和列表；切换列表时自动选中当天，同一课程在不同周保持同色，不同课程分配不同颜色。
- 表格按可用屏幕空间展示一周课程，地点支持多行和字号自适应；点击课程查看完整详情。
- 显示具体上课时间：5 月 1 日至 9 月 30 日使用夏秋作息，其他日期使用冬春作息。
- 校园概览、个人资料及手机号遮罩。

## 下载与安装

在本仓库的 [Releases](https://github.com/efface-yyy/huangshi-xiaoyuan/releases) 下载 `黄师校园-0.1.0.apk`。首个版本为预览版，功能与服务兼容性仍在完善。

要求 Android 6.0（API 23）及以上，并能够访问学校服务器。Release APK 已签名，安装后需自行输入校园账号；安装包不包含本机登录状态或测试数据。

## 项目结构

仓库根目录即 Android 工程，可直接使用 Android Studio 打开。

```text
.
├── app/
│   ├── src/main/       # 原生 Java 页面、网络协议、会话保存和资源
│   └── build.gradle
├── gradle/wrapper/     # Gradle 构建工具
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

应用使用原生 Android API，没有引入原 App 的业务类、加固壳或第三方 SDK。

## 构建

1. 使用 Android Studio 直接打开仓库根目录。
2. 配置 **JDK 11**，安装 **Android SDK 33**，等待 Gradle 同步。
3. 运行 `app`，或在仓库根目录执行：

```powershell
.\gradlew.bat assembleDebug
```

macOS / Linux 使用 `sh ./gradlew assembleDebug`。调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

构建工具：Android Gradle Plugin 7.4.2、Gradle 7.6、Java 11；minSdk 23、compileSdk / targetSdk 33。本机 SDK 地址由 Android Studio 写入 `local.properties`，无需提交。

### 签名发布

自行准备签名密钥，并通过环境变量提供以下配置；密钥与密码不提交到仓库。

| 环境变量 | 内容 |
| --- | --- |
| `HGNU_KEYSTORE_PATH` | 本机密钥库的绝对路径 |
| `HGNU_STORE_PASSWORD` | 密钥库密码 |
| `HGNU_KEY_ALIAS` | 签名别名 |
| `HGNU_KEY_PASSWORD` | 私钥密码 |

执行 `.\gradlew.bat assembleRelease`，配置签名后输出 `app/build/outputs/apk/release/app-release.apk`；未配置签名时生成未签名产物。

## 当前限制

- **“服务”功能尚未适配完毕**：校园概况入口已显示正常内容；借阅排行、学生请假在入口检查中返回“您没有访问权限”，尚不能确认可用。没有遍历全部服务，也没有测试请假、预约、支付等业务提交。
- 本项目不是学校官方客户端，也不代表学校提供支持；功能依赖学校现有接口、账号权限及网络环境。
- 校历和课程依赖服务端数据，每次启动重新同步，尚无离线课表缓存或通知推送。
- 退出登录目前清理本机状态，未调用服务端注销接口。
- 学校现有服务使用 HTTP；客户端按原协议加密业务负载，且仅对学校域名放行明文 HTTP。这不等同于 HTTPS 的传输保护。

## 文件与隐私

请勿将个人资料、账号密码或会话凭据提交到 Issue、日志或代码仓库。
