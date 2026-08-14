# SD卡根目录创建监视 (LSPosed 模块)

## 功能
- 使用 LSPosed Hook 精确记录**哪个 App** 在 `/storage/emulated/0` 根目录创建了文件或文件夹
- 支持 Hook `File.mkdir / mkdirs / createNewFile` 以及部分 `ContentResolver.insert`
- 自带简单界面：查看日志、清空日志、用其他应用打开日志
- 日志保存在：`/storage/emulated/0/Download/sdcard_create_hook.log`

## 日志格式示例
```
[创建] 2026-08-14 10:30:15
应用：微信
包名：com.tencent.mm
类型：目录
路径：/storage/emulated/0/微信测试
方法：File.mkdir
```

## 使用方法

1. **用 Android Studio 打开本项目**，编译生成 APK（或使用命令行 gradle）
2. 安装 APK 到手机
3. 打开 **LSPosed 管理器** → 模块 → 勾选「SD卡创建监视」
4. **作用域建议**：
   - 勾选「系统框架」(android)
   - 勾选 `com.android.providers.media.module`（媒体存储）
   - 勾选你想监视的具体 App（文件管理器、微信等）
   - 或者直接勾选「推荐应用」
5. **强制重启**手机（或软重启）
6. 打开本模块 App，点击「刷新日志」查看记录

## 编译说明
```bash
# 需要 Android SDK
./gradlew assembleRelease
# APK 位于 app/build/outputs/apk/release/
```

## 注意事项
- 必须安装并启用 LSPosed（推荐 Zygisk 版本）
- 作用域没勾选对应 App 时，该 App 的创建行为不会被记录
- 部分系统级创建仍可能显示为「媒体存储」，这是正常的
- 本模块只监视**根目录直接创建**（一级文件/文件夹），子目录内的创建默认不记录

## 作者
Grok 生成 · 2026
