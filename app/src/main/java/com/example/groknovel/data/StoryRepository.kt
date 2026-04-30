package com.example.groknovel.data

import com.example.groknovel.network.GrokApi
import com.example.groknovel.network.GrokResponse
import kotlinx.coroutines.flow.Flow

/**
 * 故事仓库类
 * 协调 ViewModel 与数据源（API 和数据库）
 */
class StoryRepository(
    private val storyDao: StoryDao,
    private val grokApi: GrokApi
) {

    // 故事类型枚举
    enum class StoryGenre(val displayName: String, val description: String) {
        FANTASY("奇幻冒险", "魔法、异世界、勇者与恶龙的故事"),
        SCIFI("科幻未来", "太空探索、时间旅行、人工智能的史诗"),
        ROMANCE("浪漫爱情", "甜蜜温馨或虐恋情深的爱恋故事"),
        MYSTERY("悬疑推理", "烧脑的谜题和出人意料的真相"),
        XIANXIA("仙侠修真", "修仙问道、御剑飞行的东方奇幻"),
        WUXIA("武侠江湖", "刀光剑影、侠骨柔情的江湖传奇")
    }

    /**
     * 获取所有故事的 Flow
     */
    fun getAllStories(): Flow<List<StoryEntity>> = storyDao.getAllStories()

    /**
     * 保存故事到数据库
     */
    suspend fun saveStory(story: StoryEntity): Long = storyDao.insert(story)

    /**
     * 删除故事
     */
    suspend fun deleteStory(story: StoryEntity) = storyDao.delete(story)

    /**
     * 清空所有故事
     */
    suspend fun clearAllStories() = storyDao.deleteAll()

    /**
     * 构建提示词
     * 根据用户选择的类型、主角和当前剧情生成提示词
     *
     * @param genre 小说类型
     * @param protagonist 主角名字
     * @param currentStory 当前剧情（空则创建新故事）
     * @param userChoice 用户选择（用于继续故事）
     * @return 构建好的提示词
     */
    fun buildPrompt(
        genre: StoryGenre,
        protagonist: String,
        currentStory: String = "",
        userChoice: String = ""
    ): String {
        val genrePrompt = when (genre) {
            StoryGenre.FANTASY -> """
                这是一个发生在艾瑟尔大陆的奇幻故事。
                世界观：魔法与剑并存，人类、精灵、矮人、兽人共同生活。
                主角是一位刚刚觉醒魔法天赋的年轻人。
            """.trimIndent()

            StoryGenre.SCIFI -> """
                这是一个发生在公元2357年的科幻故事。
                世界观：人类已经殖民火星和多个星系，人工智能与人类共生。
                主角是一名星际飞船的年轻驾驶员。
            """.trimIndent()

            StoryGenre.ROMANCE -> """
                这是一个发生在现代都市的爱情故事。
                世界观：繁华的都市中，不同背景的人们相遇相知。
                主角是一个追求真爱的年轻人。
            """.trimIndent()

            StoryGenre.MYSTERY -> """
                这是一个充满悬疑的推理故事。
                世界观：表面平静的小镇暗流涌动，每个人都有不为人知的秘密。
                主角是一位初出茅庐的侦探。
            """.trimIndent()

            StoryGenre.XIANXIA -> """
                这是一个发生在修真世界的仙侠故事。
                世界观：三界六道，修士追求长生不老，以丹药、法宝、阵法为尊。
                主角是一个偶然获得上古传承的少年。
            """.trimIndent()

            StoryGenre.WUXIA -> """
                这是一个快意恩仇的武侠故事。
                世界观：江湖门派林立，正邪对立，刀剑无眼。
                主角是一个初入江湖的年轻侠客。
            """.trimIndent()
        }

        return if (currentStory.isEmpty()) {
            // 创建新故事
            """
                ${genrePrompt}
                
                主角名字：$protagonist
                
                请开始创作一个引人入胜的开篇故事，包含：
                1. 主角的出场和背景介绍
                2. 故事冲突的引入
                3. 至少3个精彩的剧情选择项
                
                故事长度：500-800字
            """.trimIndent()
        } else {
            // 继续现有故事
            """
                ${genrePrompt}
                
                当前故事进展：
                $currentStory
                
                主角的选择：$userChoice
                
                请继续故事的发展，保持：
                1. 情节的连贯性和合理性
                2. 主角性格的一致性（$protagonist）
                3. 至少3个新的剧情选择项
                
                故事长度：400-600字
            """.trimIndent()
        }
    }

    /**
     * 生成故事
     * @param prompt 提示词
     * @param storyHistory 故事历史
     * @return API 调用结果
     */
    suspend fun generateStory(prompt: String, storyHistory: String = ""): Result<GrokResponse> {
        return grokApi.generateStory(prompt, storyHistory)
    }
}
