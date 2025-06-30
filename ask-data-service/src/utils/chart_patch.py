"""
图表文件名修补模块

通过 monkey patch 的方式修改 PandasAI 的图表文件名生成逻辑
"""

import os
import re
from .logger import get_logger


def patch_chart_filename():
    """
    修补 PandasAI 的图表文件名生成逻辑
    """
    logger = get_logger(__name__)
    
    try:
        from pandasai.core.code_generation.code_cleaning import CodeCleaner
        
        # 保存原始方法
        original_method = CodeCleaner._replace_output_filenames_with_temp_chart
        
        def custom_replace_output_filenames_with_temp_chart(self, code: str) -> str:
            """
            自定义的图表文件名替换方法，同时修复变量引用错误
            """
            # 检查是否设置了自定义的 query_id
            query_id = os.environ.get('CURRENT_QUERY_ID')
            
            if query_id:
                # 使用自定义的 query_id
                from pandasai.constants import DEFAULT_CHART_DIRECTORY
                chart_path = os.path.join(DEFAULT_CHART_DIRECTORY, f"{query_id}.png")
                chart_path = chart_path.replace("\\", "\\\\")
                logger.info(f"🎨 使用自定义图表文件名: {chart_path}")
                
                # 替换代码中的图表文件名
                updated_code = re.sub(
                    r"""(['"])([^'"]*\.png)\1""",
                    lambda m: f"{m.group(1)}{chart_path}{m.group(1)}",
                    code,
                )
                
                # 修复f-string中的变量引用错误
                # 查找所有可能的文件名变量（如 plot_filename, chart_filename 等）
                filename_vars = re.findall(r'(\w*(?:plot|chart|file)(?:name|_name)?)\s*=\s*["\'][^"\']*\.png["\']', updated_code)
                
                if filename_vars:
                    # 获取第一个文件名变量
                    filename_var = filename_vars[0]
                    logger.info(f"🔧 检测到文件名变量: {filename_var}")
                    
                    # 修复f-string中错误的变量引用
                    # 1. 匹配形如 {some_filename.png} 的模式，应该替换为 {filename_var}
                    pattern1 = r'\{([^{}]*\.png)\}'
                    def fix_filename_ref(match):
                        original_ref = match.group(1)
                        # 如果引用的不是变量名而是直接的文件名，替换为正确的变量名
                        if '.' in original_ref and original_ref != filename_var:
                            logger.info(f"🔧 修复文件名引用: {{{original_ref}}} -> {{{filename_var}}}")
                            return f"{{{filename_var}}}"
                        return match.group(0)
                    
                    updated_code = re.sub(pattern1, fix_filename_ref, updated_code)
                    
                    # 2. 修复形如 {filename_without_extension.png} 的模式
                    # 特别处理类似 {qingyang_land_area.png} 的情况
                    pattern2 = r'\{([a-zA-Z_][a-zA-Z0-9_]*\.png)\}'
                    def fix_undefined_var(match):
                        var_ref = match.group(1)
                        var_name = var_ref.replace('.png', '')
                        # 检查这个变量名是否在代码中定义过
                        # 使用正则表达式检查是否有 var_name = 的赋值语句
                        var_definition_pattern = rf'\b{re.escape(var_name)}\s*='
                        if not re.search(var_definition_pattern, updated_code) and var_name != filename_var:
                            logger.info(f"🔧 修复未定义变量引用: {{{var_ref}}} -> {{{filename_var}}}")
                            return f"{{{filename_var}}}"
                        return match.group(0)
                    
                    updated_code = re.sub(pattern2, fix_undefined_var, updated_code)
                
                return updated_code
            else:
                # 使用原始方法
                logger.debug("🎨 使用默认图表文件名生成方法")
                return original_method(self, code)
        
        # 替换方法
        CodeCleaner._replace_output_filenames_with_temp_chart = custom_replace_output_filenames_with_temp_chart
        
        logger.info("✅ 图表文件名修补成功")
        return True
        
    except Exception as e:
        logger.error(f"❌ 图表文件名修补失败: {str(e)}")
        return False


def apply_chart_patch():
    """
    应用图表修补，仅在需要时调用
    """
    logger = get_logger(__name__)
    
    if not hasattr(apply_chart_patch, 'applied'):
        if patch_chart_filename():
            apply_chart_patch.applied = True
            logger.info("🔧 图表文件名修补已应用")
        else:
            logger.error("❌ 图表文件名修补失败")
    else:
        logger.debug("🔧 图表文件名修补已经应用过了") 