# 使用 GitHub Actions 自动编译 APK 教程

## 步骤

1. 登录 GitHub，点击右上角 **+** → **New repository**
2. 仓库名随便写，例如 `SdcardCreateHook`，选择 **Public**，点击 Create
3. 在仓库页面点击 **uploading an existing file**
4. 把本项目**所有文件**拖进去上传（包括 `.github` 文件夹）
5. 点击 Commit changes
6. 点击仓库上方的 **Actions** 标签
7. 左侧选择 **Build LSPosed Module APK**
8. 点击右侧 **Run workflow** → **Run workflow**
9. 等待几分钟，绿色勾出现后，点击该次运行
10. 在页面底部 **Artifacts** 区域下载 `SdcardCreateHook-debug` 或 `release`
11. 解压得到 APK，安装到手机即可

## 注意事项
- 第一次运行可能需要 3~8 分钟
- 下载的是 Debug 版（已足够使用）
- 安装后去 LSPosed 启用模块并勾选作用域，然后重启
