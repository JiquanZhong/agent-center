"""
图表文件名修补模块

通过 monkey patch 的方式修改 PandasAI 的图表文件名生成逻辑
"""

import os
import re


def patch_chart_filename():
    """
    修补 PandasAI 的图表文件名生成逻辑
    """
    try:
        from pandasai.core.code_generation.code_cleaning import CodeCleaner
        
        # 保存原始方法
        original_method = CodeCleaner._replace_output_filenames_with_temp_chart
        
        def custom_replace_output_filenames_with_temp_chart(self, code: str) -> str:
            """
            自定义的图表文件名替换方法
            """
            # 检查是否设置了自定义的 query_id
            query_id = os.environ.get('CURRENT_QUERY_ID')
            
            if query_id:
                # 使用自定义的 query_id
                from pandasai.constants import DEFAULT_CHART_DIRECTORY
                chart_path = os.path.join(DEFAULT_CHART_DIRECTORY, f"{query_id}.png")
                chart_path = chart_path.replace("\\", "\\\\")
                return re.sub(
                    r"""(['"])([^'"]*\.png)\1""",
                    lambda m: f"{m.group(1)}{chart_path}{m.group(1)}",
                    code,
                )
            else:
                # 使用原始方法
                return original_method(self, code)
        
        # 替换方法
        CodeCleaner._replace_output_filenames_with_temp_chart = custom_replace_output_filenames_with_temp_chart
        
        return True
        
    except Exception as e:
        print(f"⚠️ 图表文件名修补失败: {str(e)}")
        return False


def apply_chart_patch():
    """
    应用图表修补，仅在需要时调用
    """
    if not hasattr(apply_chart_patch, 'applied'):
        if patch_chart_filename():
            apply_chart_patch.applied = True
            print("✅ 图表文件名修补已应用")
        else:
            print("❌ 图表文件名修补失败") 