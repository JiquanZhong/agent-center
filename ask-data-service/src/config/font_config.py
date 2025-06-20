"""
字体配置模块

配置matplotlib中文字体支持
"""

import matplotlib.pyplot as plt
import matplotlib
from matplotlib.font_manager import FontProperties
import platform

def setup_chinese_font():
    """配置matplotlib中文字体支持"""
    try:
        # 获取操作系统类型
        system = platform.system()
        
        if system == "Windows":
            # Windows系统常用中文字体
            fonts = ['SimHei', 'Microsoft YaHei', 'SimSun', 'KaiTi']
        elif system == "Darwin":  # macOS
            fonts = ['Heiti TC', 'Arial Unicode MS', 'Songti SC']
        else:  # Linux
            fonts = ['WenQuanYi Micro Hei', 'Droid Sans Fallback', 'DejaVu Sans']
        
        # 尝试设置字体
        for font in fonts:
            try:
                plt.rcParams['font.sans-serif'] = [font] + plt.rcParams['font.sans-serif']
                plt.rcParams['axes.unicode_minus'] = False  # 解决负号显示问题
                
                # 测试字体是否可用
                fig, ax = plt.subplots(figsize=(1, 1))
                ax.text(0.5, 0.5, '测试', fontsize=12)
                plt.close(fig)
                
                print(f"✅ 成功配置中文字体: {font}")
                return True
            except:
                continue
        
        # 如果都失败了，尝试自动查找系统字体
        print("⚠️ 使用默认字体配置，图表中文可能显示为方框")
        plt.rcParams['axes.unicode_minus'] = False
        return False
        
    except Exception as e:
        print(f"⚠️ 字体配置失败: {str(e)}")
        return False

def get_available_fonts():
    """获取系统可用的中文字体列表"""
    import matplotlib.font_manager as fm
    
    # 获取所有字体
    fonts = [f.name for f in fm.fontManager.ttflist]
    
    # 过滤中文字体（包含常见中文字体名称的）
    chinese_fonts = []
    chinese_keywords = ['SimHei', 'SimSun', 'Microsoft YaHei', 'KaiTi', 
                       'Heiti', 'Songti', 'WenQuanYi', 'Droid Sans Fallback']
    
    for font in fonts:
        for keyword in chinese_keywords:
            if keyword.lower() in font.lower():
                chinese_fonts.append(font)
                break
    
    return list(set(chinese_fonts))  # 去重

def test_font_display():
    """测试字体显示效果"""
    try:
        # 创建测试图表
        fig, ax = plt.subplots(figsize=(8, 6))
        
        # 测试各种中文字符
        test_text = "测试中文字体显示效果\n数据分析图表标题\n坐标轴标签"
        ax.text(0.5, 0.5, test_text, fontsize=14, ha='center', va='center')
        ax.set_title('中文字体测试', fontsize=16)
        ax.set_xlabel('X轴标签', fontsize=12)
        ax.set_ylabel('Y轴标签', fontsize=12)
        
        # 保存测试图片
        plt.savefig('font_test.png', dpi=150, bbox_inches='tight')
        plt.close()
        
        print("📊 字体测试图表已保存为 font_test.png")
        return True
        
    except Exception as e:
        print(f"❌ 字体测试失败: {str(e)}")
        return False 