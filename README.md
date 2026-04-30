# GrokNovelApp

基于 Grok 4.20 API 的 AI 小说生成 Android 应用

## 功能特性

- 🤖 **AI 故事生成**: 使用 Grok 4.20 API 智能生成精彩小说内容
- 📚 **多种类型支持**: 奇幻冒险、科幻未来、浪漫爱情、悬疑推理、仙侠修真、武侠江湖
- 🎭 **互动剧情选择**: 根据剧情发展提供多个选择项，由你决定故事走向
- 💾 **本地存储**: 自动保存故事历史，随时回顾和继续
- 🎨 **Material3 设计**: 现代美观的 Material Design 3 界面

## 技术栈

- **Kotlin** - 开发语言
- **Jetpack Compose** - 现代声明式 UI
- **Room** - 本地数据库
- **OkHttp** - 网络请求
- **Coroutines + Flow** - 异步编程
- **MVVM** - 架构模式

## 项目结构

```
app/
├── src/main/java/com/example/groknovel/
│   ├── ui/                    # Compose UI 层
│   │   ├── StoryScreen.kt   # 主界面
│   │   └── theme/            # 主题配置
│   ├── viewmodel/            # ViewModel 层
│   │   └── StoryViewModel.kt
│   ├── data/                 # 数据层
│   │   ├── StoryEntity.kt   # 数据库实体
│   │   ├── StoryDao.kt      # 数据访问对象
│   │   ├── StoryDatabase.kt # 数据库
│   │   └── StoryRepository.kt
│   ├── network/              # 网络层
│   │   └── GrokApi.kt       # API 调用
│   └── MainActivity.kt       # 入口 Activity
```

## API 配置

1. 复制 `local.properties.example` 为 `local.properties`
2. 在 `local.properties` 中填入你的 API Key:

```properties
API_KEY=your_api_key_here
```

## 构建

### 前置要求

- JDK 17+
- Android SDK (API 34)
- Gradle 8.2

### 命令

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 运行测试
./gradlew test
```

## GitHub Actions

项目配置了自动 CI/CD，每次推送到 main 分支会自动：
1. 检出代码
2. 构建 Debug APK
3. 上传构建产物

## 许可证

MIT License
