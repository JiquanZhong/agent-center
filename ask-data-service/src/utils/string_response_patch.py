"""
String响应补丁模块

通过monkey patching强制PandasAI chat方法返回String类型的响应
"""

import pandas as pd
import pandasai as pai
import functools
import logging

# 获取logger
logger = logging.getLogger(__name__)

def force_string_response(original_chat_method):
    """
    装饰器：强制chat方法返回String类型的响应
    
    Args:
        original_chat_method: 原始的chat方法
        
    Returns:
        装饰后的方法
    """
    @functools.wraps(original_chat_method)
    def wrapper(self, query, *args, **kwargs):
        # 修改查询以强制返回文本描述
        enhanced_query = _enhance_query_for_string(query)
        
        # 调用原始方法
        try:
            response = original_chat_method(self, enhanced_query, *args, **kwargs)
            
            # 确保返回String类型
            return _ensure_string_response(response, query)
            
        except Exception as e:
            logger.error(f"查询失败: {str(e)}")
            # 返回错误字符串
            return f"查询失败: {str(e)}"
    
    return wrapper

def _enhance_query_for_string(original_query):
    """
    增强查询以强制返回String格式
    
    Args:
        original_query: 原始查询
        
    Returns:
        增强后的查询
    """
    enhanced_query = f"""
请分析以下问题并用中文文本形式回答：{original_query}

重要要求：
1. 只返回文本字符串格式的结果
2. 结果要求为三段式结构：筛选条件、查询结果、总结分析，每一部分标题前都应添加“### ”符号
3. 给出清晰、完整的文字描述
4. 筛选条件的示例如下：
    ### 筛选条件：
    - 行政区划：县级名称='黄岛区'
    - 年份：SJNF=2019
    - 地类类型：一级地类名称='耕地'
    - 面积计算：对TBMJ(图斑面积)列求和


"""
    
    return enhanced_query

def _ensure_string_response(response, original_query):
    """
    确保响应是String类型
    
    Args:
        response: PandasAI的响应
        original_query: 原始查询
        
    Returns:
        str: 字符串格式的响应
    """
    logger.debug(f"响应类型: {type(response)}")
    
    # 如果已经是字符串，直接返回
    if isinstance(response, str):
        logger.debug("响应已经是字符串类型")
        return response
    
    # 检查是否是PandasAI的响应对象
    if hasattr(response, 'value'):
        value = response.value
        logger.debug(f"从响应对象提取值，类型: {type(value)}")
        
        if isinstance(value, str):
            return value
        elif isinstance(value, pd.DataFrame):
            # DataFrame转文字描述
            if value.empty:
                return "查询结果为空"
            else:
                return f"查询结果包含 {len(value)} 行数据。\n\n主要内容：\n{value.to_string()}"
        elif isinstance(value, (int, float)):
            return f"计算结果：{value}"
        elif isinstance(value, (list, tuple)):
            if len(value) == 0:
                return "查询结果为空列表"
            else:
                return f"查询结果包含 {len(value)} 个项目：{', '.join(map(str, value[:10]))}" + ("..." if len(value) > 10 else "")
        else:
            return str(value)
    
    # 直接处理原始响应
    if isinstance(response, pd.DataFrame):
        if response.empty:
            return "查询结果为空"
        else:
            return f"查询结果包含 {len(response)} 行数据。\n\n主要内容：\n{response.to_string()}"
    elif isinstance(response, (int, float)):
        return f"计算结果：{response}"
    elif isinstance(response, (list, tuple)):
        if len(response) == 0:
            return "查询结果为空列表"
        else:
            return f"查询结果包含 {len(response)} 个项目：{', '.join(map(str, response[:10]))}" + ("..." if len(response) > 10 else "")
    else:
        # 最后的fallback
        return f"查询：{original_query}\n响应：{str(response)}\n类型：{type(response).__name__}"

def apply_string_response_patch():
    """
    应用String响应补丁
    
    这个函数会修改PandasAI的DataFrame类的chat方法，
    使其始终返回String类型的响应
    """
    try:
        # 获取PandasAI的DataFrame类
        PandasAI_DataFrame = pai.DataFrame
        
        # 保存原始的chat方法
        if not hasattr(PandasAI_DataFrame, '_original_chat'):
            PandasAI_DataFrame._original_chat = PandasAI_DataFrame.chat
            
            # 应用补丁
            PandasAI_DataFrame.chat = force_string_response(PandasAI_DataFrame.chat)
            
            logger.info("✅ String响应补丁已成功应用")
            print("🔧 已应用String响应补丁，所有查询将返回String格式")
        else:
            logger.info("String响应补丁已经应用过了")
            
    except Exception as e:
        logger.error(f"应用String响应补丁失败: {str(e)}")
        print(f"❌ 应用String响应补丁失败: {str(e)}")

def remove_dataframe_response_patch():
    """
    移除String响应补丁，恢复原始行为
    """
    try:
        PandasAI_DataFrame = pai.DataFrame
        
        if hasattr(PandasAI_DataFrame, '_original_chat'):
            # 恢复原始方法
            PandasAI_DataFrame.chat = PandasAI_DataFrame._original_chat
            delattr(PandasAI_DataFrame, '_original_chat')
            
            logger.info("✅ String响应补丁已移除")
            print("🔄 已移除String响应补丁，恢复原始行为")
        else:
            logger.info("没有找到已应用的String响应补丁")
            
    except Exception as e:
        logger.error(f"移除String响应补丁失败: {str(e)}")
        print(f"❌ 移除String响应补丁失败: {str(e)}")

# 可选：自动应用补丁
def auto_apply_patch():
    """
    自动应用补丁（仅在模块被导入时调用一次）
    """
    apply_string_response_patch()

if __name__ == "__main__":
    # 如果直接运行此文件，应用补丁
    apply_string_response_patch() 