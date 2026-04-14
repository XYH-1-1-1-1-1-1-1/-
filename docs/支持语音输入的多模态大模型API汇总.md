# 支持语音输入的多模态大模型API汇总

> 更新时间: 2026年4月

---

## 一、可直接接收语音+提示词生成结果的多模态大模型API

### 1. OpenAI API (GPT-4o / GPT-4o-mini)
- **官网**: https://platform.openai.com/docs/guides/audio
- **API端点**: `chat.completions` with audio input
- **功能**: 
  - 直接接收音频文件/音频URL
  - 支持语音+文本提示词的多模态输入
  - 可分析语音中的情感、语气、内容等
- **支持语言**: 多语言（含中文）
- **特点**:
  - 端到端音频理解，无需先转文字
  - 可分析说话语气、情感倾向
  - 支持实时语音对话
- **价格**: 按token计费
- **示例**:
  ```python
  from openai import OpenAI
  client = OpenAI()
  
  response = client.chat.completions.create(
      model="gpt-4o-audio-preview",
      messages=[
          {
              "role": "user",
              "content": [
                  {"type": "text", "text": "分析这段语音的情感"},
                  {"type": "input_audio", "input_audio": {"data": audio_base64, "format": "wav"}}
              ]
          }
      ]
  )
  ```

### 2. Google Gemini API
- **官网**: https://ai.google.dev/gemini-api
- **功能**: 
  - 支持音频+文本多模态输入
  - 可直接接收音频文件进行分析
- **支持语言**: 多语言（含中文）
- **特点**:
  - 真正的原生多模态模型
  - 支持音频、图像、文本、视频
  - 情感分析能力强
- **模型**: gemini-1.5-pro, gemini-1.5-flash
- **示例**:
  ```python
  import google.generativeai as genai
  genai.configure(api_key="YOUR_API_KEY")
  
  model = genai.GenerativeModel('gemini-1.5-pro')
  response = model.generate_content([
      "分析这段语音的情感倾向",
      {"mime_type": "audio/wav", "data": audio_bytes}
  ])
  ```

### 3. 阿里通义千问 (Qwen-Audio)
- **官网**: https://dashscope.aliyun.com/
- **API**: 通义千问 Audio 理解
- **功能**:
  - 支持中文语音理解
  - 可接收音频输入+文本提示词
  - 支持语音内容识别和情感分析
- **支持语言**: 中文为主
- **特点**:
  - 对中文语音优化好
  - 价格相对较低
  - 国内访问速度快
- **模型**: qwen-audio-turbo, qwen-audio-plus

### 4. 百度千帆大模型平台 (ERNIE-Audio)
- **官网**: https://cloud.baidu.com/product/wenxinworkshop
- **功能**:
  - 语音理解多模态模型
  - 支持音频+提示词生成结果
- **支持语言**: 中文
- **特点**:
  - 中文语音理解能力强
  - 提供情感分析能力
  - 有免费额度

### 5. MiniMax (海螺AI)
- **官网**: https://api.minimax.chat/
- **功能**:
  - 多模态对话模型
  - 支持音频输入
- **支持语言**: 中文
- **特点**:
  - 中文场景优化
  - 适合客服场景

### 6. 智谱AI (GLM-4-Voice)
- **官网**: https://open.bigmodel.cn/
- **功能**:
  - 语音理解模型
  - 支持音频直接输入+提示词
- **支持语言**: 中文
- **特点**:
  - 端到端语音理解
  - 支持情感分析

---

## 二、语音转文字 + LLM 组合方案

如果不使用原生多模态API，也可以使用以下组合方案：

### 方案1: Whisper + GPT-4
```
语音 → Whisper API (转文字) → GPT-4 (情感分析+提示词处理)
```
- **Whisper**: OpenAI的语音识别模型，支持中文
- **优势**: 成本低，灵活度高

### 方案2: 百度语音识别 + 文心一言
```
语音 → 百度ASR → 文心一言分析
```

### 方案3: 阿里语音识别 + 通义千问
```
语音 → 阿里NLS → Qwen分析
```

---

## 三、推荐对比

| API | 是否支持语音直连 | 中文支持 | 情感分析 | 价格 | 延迟 |
|-----|----------------|---------|---------|------|------|
| OpenAI GPT-4o | ✅ | ✅ | ✅ | 中 | 低 |
| Google Gemini 1.5 | ✅ | ✅ | ✅ | 中 | 低 |
| 阿里Qwen-Audio | ✅ | ✅✅ | ✅ | 低 | 低 |
| 百度ERNIE-Audio | ✅ | ✅✅ | ✅ | 低 | 低 |
| 智谱GLM-4-Voice | ✅ | ✅✅ | ✅ | 低 | 中 |

---

## 四、快速开始示例

### 使用OpenAI GPT-4o进行语音情感分析

```python
import base64
from openai import OpenAI

client = OpenAI(api_key="your-api-key")

def analyze_speech_emotion(audio_file_path, prompt="请分析这段语音的情感，包括说话人的情绪状态、语气特征等"):
    # 读取音频文件
    with open(audio_file_path, "rb") as f:
        audio_data = f.read()
    
    audio_base64 = base64.b64encode(audio_data).decode('utf-8')
    
    response = client.chat.completions.create(
        model="gpt-4o-audio-preview",
        messages=[
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": prompt},
                    {
                        "type": "input_audio",
                        "input_audio": {
                            "data": audio_base64,
                            "format": "wav"
                        }
                    }
                ]
            }
        ],
        max_tokens=500
    )
    
    return response.choices[0].message.content

# 使用示例
result = analyze_speech_emotion("test.wav")
print(result)
```

### 使用Google Gemini进行语音情感分析

```python
import google.generativeai as genai

genai.configure(api_key="your-api-key")

def analyze_with_gemini(audio_path, prompt="分析这段语音的情感特征"):
    model = genai.GenerativeModel('gemini-1.5-pro')
    
    # 读取音频
    with open(audio_path, 'rb') as f:
        audio_data = f.read()
    
    response = model.generate_content([
        prompt,
        {"mime_type": "audio/wav", "data": audio_data}
    ])
    
    return response.text

# 使用示例
result = analyze_with_gemini("test.wav")
print(result)
```

### 使用阿里通义千问Qwen-Audio

```python
import dashscope
from dashscope import MultiModalConversation

dashscope.api_key = "your-api-key"

def analyze_with_qwen_audio(audio_path, prompt="请分析这段语音的情感"):
    messages = [
        {
            "role": "system",
            "content": [{"text": "你是一个语音情感分析助手"}]
        },
        {
            "role": "user",
            "content": [
                {"text": prompt},
                {"audio": f"file://{audio_path}"}
            ]
        }
    ]
    
    response = MultiModalConversation.call(
        model="qwen-audio-turbo",
        messages=messages
    )
    
    return response.output.choices[0].message.content

# 使用示例
result = analyze_with_qwen_audio("test.wav")
print(result)
```

---

## 五、总结与建议

**🎯 推荐方案**:
1. **最佳效果**: OpenAI GPT-4o Audio / Google Gemini 1.5
2. **最佳性价比**: 阿里Qwen-Audio / 百度ERNIE-Audio
3. **中文最优**: 阿里Qwen-Audio / 智谱GLM-4-Voice

**⚡ 快速集成**:
- 以上API都支持RESTful接口
- 都有Python SDK
- 可直接接收音频文件+提示词

**🔧 集成建议**:
- 如果需要情感分析+自然语言理解，优先选多模态大模型
- 如果只需要情感分类，可以用专门的语音情感API

---

*本文档持续更新*