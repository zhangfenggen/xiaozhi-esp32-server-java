package com.xiaozhi.utils;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 表情符号处理工具类
 * 用于从文本中提取表情符号、过滤表情符号并映射为情感词
 *
 * @author yuchen
 * @date 2025/4/14
 */
@Slf4j
public class EmojiUtils {

    // 定义表情符号的Unicode范围
    private static final int[][] EMOJI_RANGES = {
            { 0x1F600, 0x1F64F }, // 表情符号
            { 0x1F300, 0x1F5FF }, // 符号和图案
            { 0x1F680, 0x1F6FF }, // 交通工具和地图符号
            { 0x1F900, 0x1F9FF }, // 补充符号
            { 0x1FA70, 0x1FAFF }, // 更多补充符号
            { 0x2600, 0x26FF }, // 杂项符号
            { 0x2700, 0x27BF }, // 装饰符号
            { 0x1F1E6, 0x1F1FF }, // 国旗表情
            { 0x1F700, 0x1F77F }, // 额外的表情符号
            { 0x20000, 0x2A6DF }, // 补充符号（更多表情）
    };

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[@#№$%&*]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    // 表情符号到情绪单词的映射
    private static final Map<String, String> emojiToEmotionMap = new HashMap<>();

    static {
        // 初始化表情符号到情绪的映射关系
        initEmojiToEmotionMap();
    }

    /**
     * 初始化表情符号到情绪的映射
     */
    private static void initEmojiToEmotionMap() {
        Map<String, String[]> emotionToEmojis = new HashMap<>();
        // 中立
        emotionToEmojis.put("neutral", new String[] { "😐", "😶" });
        // 开心
        emotionToEmojis.put("happy", new String[] { "🌈", "😊", "🎈", "🐱" });
        // 笑
        emotionToEmojis.put("laughing", new String[] { "😀", "😃", "😁", "😏", "😄", "🤪" });
        // 搞笑
        emotionToEmojis.put("funny", new String[] { "😂", "🤣", "😆" });
        // 悲伤
        emotionToEmojis.put("sad", new String[] { "😢", "😔", "😞", "😑" });
        // 生气
        emotionToEmojis.put("angry", new String[] { "😠", "😡", "😒", "😤", "🤬" });
        // 哭泣
        emotionToEmojis.put("crying", new String[] { "😭" });
        // 爱
        emotionToEmojis.put("loving", new String[] { "❤️", "💕", "😍", "🥰", "💖" });
        // 尴尬
        emotionToEmojis.put("embarrassed", new String[] { "😳", "😓", "😅" });
        // 惊讶
        emotionToEmojis.put("surprised", new String[] { "😮", "😲", "😯" });
        // 震惊
        emotionToEmojis.put("shocked", new String[] { "😱", "😨", "😬" });
        // 思考
        emotionToEmojis.put("thinking", new String[] { "🤔", "💭", "💬", "🧐" });
        // 眨眼
        emotionToEmojis.put("winking", new String[] { "😉", "🤗", "👋", "🌟", "🐶" });
        // 酷
        emotionToEmojis.put("cool", new String[] { "😎" });
        // 放松
        emotionToEmojis.put("relaxed", new String[] { "😌" });
        // 美味
        emotionToEmojis.put("delicious", new String[] { "😋", "🤤", "🍽️" });
        // 亲吻
        emotionToEmojis.put("kissy", new String[] { "😘", "💋", "😚", "😗", "😙" });
        // 自信
        emotionToEmojis.put("confident", new String[] { "💪" });
        // 困倦
        emotionToEmojis.put("sleepy", new String[] { "😴" });
        // 愚蠢
        emotionToEmojis.put("silly", new String[] { "😛", "😜", "😝" });
        // 困惑
        emotionToEmojis.put("confused", new String[] { "😕", "🙄" });

        // 填充表情符号到情绪单词的映射
        for (Map.Entry<String, String[]> entry : emotionToEmojis.entrySet()) {
            String emotion = entry.getKey();
            for (String emoji : entry.getValue()) {
                // 将表情符号的字符逐个映射到情绪单词
                emojiToEmotionMap.put(emoji, emotion);
            }
        }
    }

    /**
     * 清理文本，移除HTML标签、特殊字符和控制字符
     *
     * @param text 输入文本
     * @return 清理后的文本
     */
    public static String cleanText(String text) {
        // 移除控制字符
        text = text.replaceAll("[\\t\\n\\r\b\\f]", "");

        // 移除HTML标签
        text = HTML_TAG_PATTERN.matcher(text).replaceAll("");

        // 移除特殊符号
        text = SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");

        // 替换连续的空白字符为单个空格
        text = WHITESPACE_PATTERN.matcher(text).replaceAll(" ");

        // 去除首尾空格
        return text.trim();
    }

    /**
     * 检查字符是否是表情符号
     *
     * @param codePoint 输入字符的Unicode码点
     * @return 如果是表情符号返回true，否则返回false
     */
    public static boolean isEmoji(int codePoint) {
        for (int[] range : EMOJI_RANGES) {
            if (codePoint >= range[0] && codePoint <= range[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取句子中的表情符号
     *
     * @param text 输入的句子
     * @return 包含所有表情符号的列表
     */
    public static List<String> extractEmojis(String text) {
        List<String> emojis = new ArrayList<>();
        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            if (Character.isValidCodePoint(codePoint)) {
                String emoji = new String(Character.toChars(codePoint));
                if (isEmoji(codePoint)) {
                    emojis.add(emoji);
                }
            }
            i += Character.charCount(codePoint);
        }
        return emojis;
    }

    /**
     * 通过表情符号获取情绪单词
     *
     * @param emoji 表情符号
     * @return 情绪单词，如果没有匹配则返回"happy"
     */
    public static String getEmotionByEmoji(String emoji) {
        return emojiToEmotionMap.getOrDefault(emoji, "happy");
    }

    /**
     * 处理句子，移除表情符号并映射为心情单词
     *
     * @param text 输入的句子
     * @return 返回包含处理后句子和表情列表的对象
     */
    public static EmoSentence processSentence(String text) {
        text = cleanText(text);
        StringBuilder cleanedText = new StringBuilder();
        List<String> moods = new ArrayList<>();

        int length = text.length();
        for (int i = 0; i < length;) {
            int codePoint = text.codePointAt(i);
            if (isEmoji(codePoint)) {
                // 转换为表情字符串并匹配情感词
                String emoji = new String(Character.toChars(codePoint));
                String mood = getEmotionByEmoji(emoji);
                if (StrUtil.isNotEmpty(mood)) {
                    moods.add(mood);
                }
                // 跳过当前表情符号
                i += Character.charCount(codePoint);
            } else {
                // 保留非表情字符
                cleanedText.appendCodePoint(codePoint);
                i++;
            }
        }
        return new EmoSentence(text, cleanedText.toString().trim(), moods);
    }

    /**
     * 表情处理后的句子对象
     */
    @Data
    public static class EmoSentence {

        private String sentence;

        private String ttsSentence;

        private List<String> moods;

        private boolean audioCleanup = true;

        /**
         * 构造函数
         *
         * @param sentence     原句子
         * @param ttsSentence  处理后的句子
         * @param audioCleanup 是否清理音频
         */
        public EmoSentence(String sentence, String ttsSentence, boolean audioCleanup) {
            this.sentence = sentence;
            this.ttsSentence = ttsSentence;
            this.audioCleanup = audioCleanup;
        }

        /**
         * 构造函数
         *
         * @param sentence    原句子
         * @param ttsSentence 处理后的句子
         * @param moods       情感词列表
         */
        public EmoSentence(String sentence, String ttsSentence, List<String> moods) {
            this.sentence = sentence;
            this.ttsSentence = ttsSentence;
            this.moods = moods;
        }
    }

    /**
     * 示例方法
     */
    public static void main(String[] args) {
        // 示例句子
        String sentence = "你好！😃 今天天气真好。🌈 This is \na sun( ny<H1> day! \n**  \t## $$$ 哈（）哈哈 ";

        // 处理句子
        EmoSentence result = processSentence(sentence);
        System.err.println(result.getTtsSentence());

        String cleanedText = cleanText(sentence);
        System.out.println("清理后的文本: " + cleanedText);
    }
}