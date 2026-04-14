# PDF简历解析功能说明

## 功能概述

本功能支持对面试者上传的PDF格式简历进行内容解析，无论PDF内容是**文本形式**还是**图片形式**，系统都能统一提取出可使用的文本内容。

## 工作原理

系统采用**智能双模式解析**策略：

1. **文本提取模式**（优先）
   - 使用 Apache PDFBox 库直接从PDF提取文本
   - 适用于包含可复制文字的PDF文档

2. **OCR识别模式**（备用）
   - 当文本提取结果少于100个字符时，自动切换到OCR模式
   - 使用 Tesseract OCR 引擎识别图片中的文字
   - 适用于扫描件、图片型PDF

### 自动判断流程

```
PDF文件
  ↓
尝试文本提取
  ↓
文本长度 > 100字符？
  ↓ 是 → 返回文本结果
  ↓ 否
使用OCR识别
  ↓
返回OCR结果
```

## 环境准备

### 1. 安装 Tesseract OCR 引擎（Windows）

**方式一：使用预编译二进制文件**

1. 下载 Tesseract Windows 版本：
   - 官方发布页：https://github.com/UB-Mannheim/tesseract/wiki
   - 或下载独立DLL版本

2. 运行安装程序，记住安装路径（如 `C:\Program Files\Tesseract-OCR`）

**方式二：使用 Scoop 包管理器**

```powershell
scoop install tesseract
```

### 2. 下载中文语言包

Tesseract 默认只包含英文语言包，需要额外下载中文语言包：

1. 访问 Tesseract 语言包仓库：https://github.com/tesseract-ocr/tessdata

2. 下载以下文件：
   - `chi_sim.traineddata` - 简体中文
   - `eng.traineddata` - 英文（通常已包含）

3. 将下载的文件放入 `tessdata` 目录：
   ```
   InterviewSimulation/
   └── tessdata/
       ├── chi_sim.traineddata
       └── eng.traineddata
   ```

### 3. 配置路径

在 `application.yml` 中配置 tessdata 路径：

```yaml
app:
  pdf:
    parser:
      # tessdata 目录的绝对路径或相对路径
      tessdata-path: ./tessdata
      # OCR语言设置：chi_sim=简体中文，eng=英文
      ocr-lang: chi_sim+eng
```

## API 接口

### 解析简历内容

**接口地址**：`GET /api/user/{userId}/resume/parse`

**功能**：解析指定用户已上传的PDF简历，提取文本内容

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileName": "张三_简历.pdf",
    "content": "张三\n联系电话：13800138000\n邮箱：zhangsan@example.com\n\n教育背景\n...",
    "contentLength": 1520
  }
}
```

### 错误响应

```json
{
  "code": 500,
  "message": "未找到简历"
}
```

## 调用示例

### 使用 curl

```bash
# 先上传简历
curl -X POST http://localhost:8080/api/user/1/resume \
  -F "file=@/path/to/resume.pdf"

# 解析简历内容
curl http://localhost:8080/api/user/1/resume/parse
```

### 使用前端 JavaScript

```javascript
// 解析简历
async function parseResume(userId) {
  try {
    const response = await fetch(`/api/user/${userId}/resume/parse`);
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('简历内容：', result.data.content);
    } else {
      console.error('解析失败：', result.message);
    }
  } catch (error) {
    console.error('请求失败：', error);
  }
}
```

## 服务类说明

### PdfResumeParser

核心解析服务类：`com.ruijie.interview.service.PdfResumeParser`

**主要方法**：

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `parsePdfContent(String pdfFilePath)` | PDF文件路径 | String | 解析PDF内容 |
| `parsePdfContent(byte[] pdfContent, String fileName)` | PDF字节数组、文件名 | String | 解析PDF内容（字节数组方式） |

### ResumeService 扩展

在 `ResumeService` 中新增了以下方法：

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `parseResumeContent(Long resumeId)` | 简历ID | String | 解析指定简历内容 |
| `getUserResumeContent(Long userId)` | 用户ID | Optional\<String\> | 获取用户简历内容 |

## 常见问题

### Q1: OCR识别结果为空或乱码

**原因**：未正确配置中文语言包

**解决方法**：
1. 确认 `chi_sim.traineddata` 文件已下载
2. 确认文件放在正确的 `tessdata` 目录
3. 检查 `application.yml` 中的 `tessdata-path` 配置是否正确

### Q2: Tess4J 依赖下载失败

**原因**：Tess4J 依赖较大，包含 native 库

**解决方法**：
```bash
# 清理Maven缓存后重新下载
mvn dependency:purge-local-repository
mvn clean install
```

### Q3: PDF解析速度慢

**原因**：OCR识别需要渲染图片和识别文字，耗时较长

**优化建议**：
- 对于文本型PDF，系统会直接使用文本提取，速度很快
- 只有图片型PDF才需要OCR，建议前端添加加载提示

### Q4: 内存占用高

**原因**：高分辨率图片渲染会占用较多内存

**优化建议**：
- 可在 `PdfResumeParser` 中调整 DPI 值（默认300）
- 降低DPI可提高速度但可能影响识别准确率

## 依赖说明

| 依赖 | 版本 | 用途 |
|------|------|------|
| Apache PDFBox | 3.0.1 | PDF文档加载和文本提取 |
| PDFBox Tools | 3.0.1 | PDF渲染为图片 |
| Tess4J | 5.11.0 | Tesseract OCR 的 Java 封装 |

## 后续扩展建议

1. **添加简历结构化解析**：利用大模型对提取的文本进行结构化解析（姓名、电话、教育背景等）
2. **支持更多格式**：扩展支持 Word (.doc/.docx) 格式
3. **缓存机制**：对已解析的简历内容进行缓存，避免重复解析
4. **异步处理**：对于大文件使用异步处理，避免阻塞请求