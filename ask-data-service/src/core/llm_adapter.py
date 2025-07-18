"""
LLM适配器模块

提供对不同LLM模型的适配支持，针对Deepseek模型的特殊处理
"""

from pandasai_openai import OpenAI
from pandasai.exceptions import NoCodeFoundError
import openai
import inspect
import re
from ..utils.logger import get_logger, LogContext

def patch_openai():
    """对OpenAI类进行monkey patch以支持第三方模型"""
    try:
        # 获取OpenAI类的原始__init__方法
        original_init = OpenAI.__init__
        
        def patched_init(self, api_token=None, **kwargs):
            # 暂存原始模型名称
            original_model = kwargs.get("model", "gpt-3.5-turbo")
            
            # 如果是deepseek-chat，临时改为gpt-3.5-turbo绕过检查
            if "deepseek" in original_model.lower():
                kwargs["model"] = "gpt-3.5-turbo"
                logger = get_logger(__name__)
                logger.debug(f"临时将模型名称从{original_model}改为gpt-3.5-turbo以绕过检查")
            
            # 调用原始初始化
            original_init(self, api_token, **kwargs)
            
            # 初始化后恢复真实模型名称
            if "deepseek" in original_model.lower():
                self.model = original_model
                logger = get_logger(__name__)
                logger.debug(f"恢复模型名称为: {self.model}")
                # 设置为聊天模型
                self._is_chat_model = True
        
        # 应用monkey patch
        OpenAI.__init__ = patched_init
        logger = get_logger(__name__)
        logger.info("成功应用monkey patch到OpenAI类")
        
    except Exception as e:
        logger = get_logger(__name__)
        logger.error(f"应用monkey patch失败: {str(e)}")

# 应用monkey patch
patch_openai()

class CustomOpenAI(OpenAI):
    """自定义OpenAI适配器，支持Deepseek等第三方模型"""
    
    def __init__(self, api_token=None, model="gpt-3.5-turbo", api_base=None, **kwargs):
        
        # 调用父类构造函数，支持第三方模型
        super().__init__(api_token=api_token, model=model, api_base=api_base, **kwargs)
        
        # 确保设置正确的客户端参数
        model_name = self.model.split(":")[1] if "ft:" in self.model else self.model
        
        # 检查是否为第三方模型
        if "deepseek" in model_name.lower():
            self._is_chat_model = True
            self.client = openai.OpenAI(**self._client_params).chat.completions
        
        logger = get_logger(__name__)
        logger.info("成功初始化自定义OpenAI LLM")
    
    def _extract_code(self, response: str, separator: str = "```") -> str:
        """针对Deepseek模型优化的代码提取"""
        logger = get_logger(__name__)
        logger.debug(f"🔍 开始提取代码，响应长度: {len(response)}")
        logger.debug(f"📝 响应内容前200字符: {response[:200]}...")
        
        # 预处理响应
        response = self._preprocess_deepseek_response(response)
        
        # 尝试多种代码块标识符
        code_separators = ["```python", "```py", "```", "```Python", "```PYTHON"]
        
        for sep in code_separators:
            if sep in response:
                logger.debug(f"🎯 使用分隔符: {sep}")
                try:
                    parts = response.split(sep)
                    if len(parts) >= 2:
                        # 取第二部分（第一个代码块）
                        code = parts[1]
                        if len(parts) > 2:
                            # 找到结束的```
                            end_idx = code.find("```")
                            if end_idx != -1:
                                code = code[:end_idx]
                        
                        code = self._polish_code(code)
                        logger.debug(f"✅ 提取到代码长度: {len(code)}")
                        logger.debug(f"📋 代码内容: {code[:100]}...")
                        
                        if self._is_python_code(code):
                            return code
                        else:
                            logger.warning("❌ 代码语法验证失败")
                            continue
                except Exception as e:
                    logger.error(f"❌ 代码提取失败: {e}")
                    continue
        
        # 如果没有找到标准的代码块，尝试其他方式
        code = self._extract_chinese_code(response)
        if code:
            logger.debug(f"🔧 使用中文代码提取: {code[:100]}...")
            if self._is_python_code(code):
                return code
        
        # 最后尝试：查找import语句到result =的代码
        code = self._extract_by_patterns(response)
        if code:
            logger.debug(f"🔍 使用模式匹配提取: {code[:100]}...")
            if self._is_python_code(code):
                return code
        
        logger.error("❌ 所有代码提取方法都失败")
        raise NoCodeFoundError("No code found in the response")
    
    def _preprocess_deepseek_response(self, response: str) -> str:
        """预处理Deepseek模型的响应"""
        # 移除常见的中文回复前缀
        prefixes_to_remove = [
            "好的，我来帮你", "当然，我来", "我来帮你", "以下是", "这是", "代码如下",
            "下面是", "我来写", "让我来", "我将", "根据你的要求"
        ]
        
        for prefix in prefixes_to_remove:
            if response.startswith(prefix):
                response = response[len(prefix):].strip()
        
        return response
    
    def _extract_chinese_code(self, response: str) -> str:
        """从中文响应中提取代码"""
        # 查找中文描述后的代码模式
        patterns = [
            r'(?:代码如下|以下是代码|下面是代码|我来写代码)[:：]?\s*\n(.*?)(?=\n\n|\Z)',
            r'(?:Python代码|python代码)[:：]?\s*\n(.*?)(?=\n\n|\Z)',
            r'(?:import\s+|#.*\n).*?result\s*=.*?(?=\n\n|\Z)',
        ]
        
        for pattern in patterns:
            match = re.search(pattern, response, re.DOTALL | re.IGNORECASE)
            if match:
                return match.group(1).strip()
        
        return None
    
    def _extract_by_patterns(self, response: str) -> str:
        """通过模式匹配提取代码"""
        # 查找从import开始到result =结束的代码
        pattern = r'(import.*?result\s*=.*?(?:}|"]").*?(?=\n[^#\s]|\Z))'
        match = re.search(pattern, response, re.DOTALL | re.IGNORECASE)
        if match:
            return match.group(1).strip()
        
        # 查找execute_sql_query相关代码
        pattern = r'(.*?execute_sql_query.*?result\s*=.*?(?:}|"]").*?(?=\n[^#\s]|\Z))'
        match = re.search(pattern, response, re.DOTALL | re.IGNORECASE)
        if match:
            return match.group(1).strip()
        
        return None
    
    def generate_code(self, instruction, context) -> str:
        """为Deepseek模型优化的代码生成"""
        # 检查instruction是否为字符串，如果是则转换为合适的格式
        if isinstance(instruction, str):
            instruction_text = instruction
        else:
            # 如果是BasePrompt对象，调用to_string()方法
            instruction_text = instruction.to_string() if hasattr(instruction, 'to_string') else str(instruction)
        
        # 增强提示词，明确要求代码格式
        enhanced_instruction = f"""
{instruction_text}

重要要求：
1. 必须返回完整的Python代码，不要有任何解释文字
2. 代码必须用```python开头，用```结尾
3. 代码必须包含完整的import语句
4. 代码必须以 result = {{"type": "...", "value": "..."}} 结尾
5. 不要添加任何中文解释，只返回代码

示例格式：
```python
import pandas as pd
# 你的代码
result = {{"type": "string", "value": "你的结果"}}
```
"""
        
        logger = get_logger(__name__)
        logger.debug(f"🚀 开始生成代码...")
        # 使用父类的completion方法而不是call方法
        memory = context.memory if context else None
        response = (
            self.chat_completion(enhanced_instruction, memory)
            if self._is_chat_model
            else self.completion(enhanced_instruction, memory)
        )
        logger.debug(f"📝 LLM响应: {response[:300]}...")
        
        return self._extract_code(response)
    
    def completion(self, prompt: str, **kwargs) -> str:
        """重写completion方法，添加中文支持"""
        # 添加中文系统提示
        if kwargs.get("messages") is None and kwargs.get("system_prompt") is None:
            kwargs["system_prompt"] = "你必须始终使用中文回答问题，无论输入是什么语言。"
        
        return super().completion(prompt, **kwargs) 