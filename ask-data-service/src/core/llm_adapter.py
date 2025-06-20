"""
LLM适配器模块

提供对不同LLM模型的适配支持
"""

from pandasai_openai import OpenAI
import openai
import inspect

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
                print(f"临时将模型名称从{original_model}改为gpt-3.5-turbo以绕过检查")
            
            # 调用原始初始化
            original_init(self, api_token, **kwargs)
            
            # 初始化后恢复真实模型名称
            if "deepseek" in original_model.lower():
                self.model = original_model
                print(f"恢复模型名称为: {self.model}")
                # 设置为聊天模型
                self._is_chat_model = True
        
        # 应用monkey patch
        OpenAI.__init__ = patched_init
        print("成功应用monkey patch到OpenAI类")
        
    except Exception as e:
        print(f"应用monkey patch失败: {str(e)}")

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
        
        print("成功初始化自定义OpenAI LLM")
    
    def completion(self, prompt: str, **kwargs) -> str:
        """重写completion方法，添加中文支持"""
        # 添加中文系统提示
        if kwargs.get("messages") is None and kwargs.get("system_prompt") is None:
            kwargs["system_prompt"] = "你必须始终使用中文回答问题，无论输入是什么语言。"
        
        return super().completion(prompt, **kwargs) 