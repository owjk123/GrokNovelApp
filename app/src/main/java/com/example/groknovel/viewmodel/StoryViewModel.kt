package com.example.groknovel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.groknovel.data.StoryEntity
import com.example.groknovel.data.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 故事状态数据类
 * 包含 UI 需要的所有状态
 */
data class StoryState(
    val content: String = "",           // 当前故事内容
    val choices: List<String> = emptyList(),  // 可选择的剧情选项
    val isLoading: Boolean = false,      // 是否正在加载
    val error: String? = null,            // 错误信息
    val selectedGenre: StoryRepository.StoryGenre = StoryRepository.StoryGenre.FANTASY,  // 当前选中的类型
    val protagonist: String = "",        // 主角名字
    val storyHistory: String = "",       // 故事历史（用于继续创作）
    val currentStoryId: Int? = null,     // 当前故事的数据库 ID
    val isStarted: Boolean = false       // 是否已开始创作
)

/**
 * 故事 ViewModel
 * 管理 UI 状态和处理业务逻辑
 */
class StoryViewModel(
    private val repository: StoryRepository
) : ViewModel() {

    // 私有可变状态
    private val _state = MutableStateFlow(StoryState())
    
    // 公有只读状态
    val state: StateFlow<StoryState> = _state.asStateFlow()

    // 所有历史故事
    val allStories: kotlinx.coroutines.flow.Flow<List<StoryEntity>> = repository.getAllStories()

    init {
        // 初始化时设置默认主角名
        _state.update { it.copy(protagonist = "林风") }
    }

    /**
     * 选择小说类型
     */
    fun selectGenre(genre: StoryRepository.StoryGenre) {
        _state.update { it.copy(selectedGenre = genre) }
    }

    /**
     * 设置主角名字
     */
    fun setProtagonist(name: String) {
        _state.update { it.copy(protagonist = name) }
    }

    /**
     * 生成新故事
     */
    fun generateStory() {
        val currentState = _state.value

        // 验证输入
        if (currentState.protagonist.isBlank()) {
            _state.update { it.copy(error = "请输入主角名字") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // 构建提示词
                val prompt = repository.buildPrompt(
                    genre = currentState.selectedGenre,
                    protagonist = currentState.protagonist,
                    currentStory = "",
                    userChoice = ""
                )

                // 调用 API 生成故事
                val result = repository.generateStory(prompt)

                result.onSuccess { response ->
                    // 保存故事到数据库
                    val storyEntity = StoryEntity(
                        title = "${currentState.protagonist}的${currentState.selectedGenre.displayName}之旅",
                        content = response.content,
                        genre = currentState.selectedGenre.displayName,
                        protagonist = currentState.protagonist
                    )
                    val storyId = repository.saveStory(storyEntity)

                    // 更新状态
                    _state.update {
                        it.copy(
                            content = response.content,
                            choices = response.choices,
                            isLoading = false,
                            storyHistory = response.content,
                            currentStoryId = storyId.toInt(),
                            isStarted = true
                        )
                    }
                }.onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "生成故事失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "发生错误: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 选择剧情走向
     * @param choice 用户选择的选项
     */
    fun selectChoice(choice: String) {
        val currentState = _state.value

        // 将选择添加到故事历史
        val newHistory = buildString {
            append(currentState.storyHistory)
            append("\n\n[用户选择: $choice]\n\n")
        }

        _state.update { it.copy(storyHistory = newHistory) }

        // 继续生成故事
        continueStory(choice)
    }

    /**
     * 继续故事发展
     * @param userChoice 用户的选择
     */
    private fun continueStory(userChoice: String) {
        val currentState = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // 构建继续创作的提示词
                val prompt = repository.buildPrompt(
                    genre = currentState.selectedGenre,
                    protagonist = currentState.protagonist,
                    currentStory = currentState.storyHistory,
                    userChoice = userChoice
                )

                // 调用 API 继续故事
                val result = repository.generateStory(prompt, currentState.storyHistory)

                result.onSuccess { response ->
                    // 更新数据库中的故事
                    currentState.currentStoryId?.let { storyId ->
                        val updatedStory = StoryEntity(
                            id = storyId,
                            title = "${currentState.protagonist}的${currentState.selectedGenre.displayName}之旅",
                            content = currentState.storyHistory + "\n\n" + response.content,
                            genre = currentState.selectedGenre.displayName,
                            protagonist = currentState.protagonist
                        )
                        repository.saveStory(updatedStory)
                    }

                    // 更新状态
                    val newContent = currentState.content + "\n\n" + response.content
                    val newHistory = currentState.storyHistory + response.content

                    _state.update {
                        it.copy(
                            content = newContent,
                            choices = response.choices,
                            isLoading = false,
                            storyHistory = newHistory
                        )
                    }
                }.onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "继续故事失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "发生错误: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 重置故事状态
     */
    fun resetStory() {
        _state.update {
            StoryState(
                protagonist = it.protagonist,
                selectedGenre = it.selectedGenre
            )
        }
    }

    /**
     * 加载已有故事
     */
    fun loadStory(story: StoryEntity) {
        _state.update {
            it.copy(
                content = story.content,
                choices = emptyList(), // 已保存的故事不包含动态选项
                storyHistory = story.content,
                currentStoryId = story.id,
                isStarted = true,
                protagonist = story.protagonist
            )
        }
    }

    /**
     * 删除故事
     */
    fun deleteStory(story: StoryEntity) {
        viewModelScope.launch {
            repository.deleteStory(story)
            // 如果删除的是当前故事，重置状态
            if (_state.value.currentStoryId == story.id) {
                resetStory()
            }
        }
    }

    /**
     * 清空所有历史
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllStories()
            resetStory()
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * 重试生成
     */
    fun retry() {
        clearError()
        if (_state.value.storyHistory.isNotEmpty()) {
            // 继续故事
            val lastChoice = _state.value.choices.firstOrNull() ?: ""
            if (lastChoice.isNotEmpty()) {
                selectChoice(lastChoice)
            }
        } else {
            generateStory()
        }
    }
}

/**
 * ViewModel 工厂
 */
class StoryViewModelFactory(
    private val repository: StoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryViewModel::class.java)) {
            return StoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
