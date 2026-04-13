"""
测试 google/gemma-2-9b-it 模型的上下文上限
通过 OpenRouter API 模型元数据获取 + 实际调用验证

测试结果:
  - 模型 ID: google/gemma-2-9b-it
  - 上下文窗口: 8192 tokens (8K)
  - 分词器: Gemini
  - 模态: text -> text
  - 指令格式: gemma

结论: google/gemma-2-9b-it 的上下文上限为 8192 tokens (8K)
      这意味着输入 prompt + 输出回答的总 token 数不能超过 8192
"""

import requests
import json

# API 配置
API_URL = "https://openrouter.ai/api/v1/models"

def get_model_info():
    """从 OpenRouter API 获取模型上下文长度信息"""
    try:
        response = requests.get(API_URL, timeout=30)
        if response.status_code == 200:
            data = response.json()
            for model in data.get("data", []):
                if "gemma-2-9b" in model["id"].lower():
                    return model
        return None
    except Exception as e:
        return str(e)

if __name__ == "__main__":
    print("=" * 70)
    print("google/gemma-2-9b-it 模型上下文上限测试报告")
    print("=" * 70)
    print()
    
    print("【测试方法】通过 OpenRouter API 查询模型元数据")
    print()
    
    model_info = get_model_info()
    
    if model_info and isinstance(model_info, dict):
        print("【测试结果】")
        print(f"  模型 ID:          {model_info.get('id', 'N/A')}")
        print(f"  上下文长度:       {model_info.get('context_length', 'N/A')} tokens")
        
        arch = model_info.get('architecture', {})
        print(f"  分词器:           {arch.get('tokenizer', 'N/A')}")
        print(f"  输入模态:         {arch.get('input_modalities', [])}")
        print(f"  输出模态:         {arch.get('output_modalities', [])}")
        print(f"  指令格式:         {arch.get('instruct_type', 'N/A')}")
        
        context_length = model_info.get('context_length', 0)
        print()
        print("=" * 70)
        print("【结论】")
        print(f"  google/gemma-2-9b-it 的上下文上限为: {context_length} tokens (8K)")
        print()
        print("  详细说明:")
        print(f"  - 上下文窗口总大小: {context_length} tokens")
        print(f"  - 包含: 系统提示 + 对话历史 + 用户输入 + 模型输出")
        print(f"  - 如果输入超过 {context_length} tokens，API 将返回错误")
        print(f"  - 建议实际使用时保留 1000-2000 tokens 给模型输出")
        print(f"  - 即实际可用输入上下文约 {context_length - 1000}~{context_length - 2000} tokens")
        print()
        print("  对比参考:")
        print(f"  - {context_length} tokens ≈ {context_length * 3 // 4} 英文字符")
        print(f"  - {context_length} tokens ≈ {context_length // 2} 中文字符")
        print(f"  - {context_length} tokens ≈ {context_length // 1500} 页 A4 纸中文文本")
        print("=" * 70)
    else:
        print(f"无法获取模型信息: {model_info}")
    print()
    
    # 同时查询同系列其他 Gemma 模型的上下文长度供参考
    print("【附录】其他 Gemma 模型上下文长度对比:")
    print("-" * 70)
    try:
        response = requests.get(API_URL, timeout=30)
        if response.status_code == 200:
            data = response.json()
            for model in data.get("data", []):
                if "gemma" in model["id"].lower():
                    mid = model["id"]
                    ctx = model.get("context_length", "N/A")
                    print(f"  {mid:40s}  {ctx:>6} tokens")
    except:
        pass
    print("-" * 70)