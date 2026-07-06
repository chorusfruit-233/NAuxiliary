# NAuxiliary

niconico Android 的 LSPosed/Xposed 模块，提供翻译与增强功能。

## 功能

- **文本翻译**：运行时翻译应用内文本（日→中），支持 WebView 内容翻译
- **广告移除**：去除应用内广告、横幅广告、视频前贴片广告
- **设置入口**：在 niconico 设置页面注入模块配置入口
- **DexKit 定位**：通过 DexKit 字符串指纹自动适配混淆后的类与方法

## 构建

```bash
./gradlew :app:assembleDebug
```

## 安装

1. 安装 [LSPosed](https://github.com/LSPosed/LSPosed) 或其他 Xposed 框架
2. 构建或下载 APK 安装到设备
3. 在 LSPosed 管理器中启用模块，作用域选择 `niconico`

## 测试

```bash
./gradlew :app:testDebugUnitTest
```

## 翻译资源

自定义翻译文件位于 `app/src/main/assets/translations/zh-CN/`：

| 文件 | 类型 |
|------|------|
| `strings.properties` | 字符串资源翻译 |
| `exact.properties` | 精确文本替换 |
| `phrases.properties` | 短语替换 |

## 许可

MIT
