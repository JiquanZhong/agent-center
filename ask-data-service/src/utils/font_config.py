"""
matplotlib中文字体配置模块

解决matplotlib显示中文字符时的字体缺失问题
"""

import matplotlib
# 设置非交互式后端，防止弹出图表窗口
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import matplotlib.font_manager as fm
import platform
import warnings
import os
import glob
from .logger import get_logger

# 关闭交互模式
plt.ioff()

logger = get_logger(__name__)

def configure_chinese_fonts():
    """配置matplotlib中文字体支持"""
    try:
        # 检测操作系统
        system = platform.system()
        
        # 尝试找到合适的中文字体
        chinese_fonts = []
        
        if system == "Windows":
            # Windows系统常用中文字体
            possible_fonts = [
                'Microsoft YaHei',  # 微软雅黑
                'SimHei',           # 黑体
                'SimSun',           # 宋体
                'KaiTi',            # 楷体
                'FangSong'          # 仿宋
            ]
        elif system == "Darwin":  # macOS
            possible_fonts = [
                'PingFang SC',      # 苹方
                'STHeiti',          # 华文黑体
                'STSong',           # 华文宋体
                'Heiti SC'          # 黑体
            ]
        else:  # Linux
            possible_fonts = [
                'DejaVu Sans',
                'WenQuanYi Micro Hei',  # 文泉驿微米黑
                'WenQuanYi Zen Hei',    # 文泉驿正黑
                'Noto Sans CJK SC',     # 思源黑体
                'Source Han Sans SC'    # 思源黑体
            ]
        
        # 获取系统所有字体
        system_fonts = [f.name for f in fm.fontManager.ttflist]
        
        # 找到第一个可用的中文字体
        for font in possible_fonts:
            if font in system_fonts:
                chinese_fonts.append(font)
                break
        
        if not chinese_fonts:
            # 如果没找到预设字体，尝试找包含中文的字体
            for font in system_fonts:
                if any(keyword in font.lower() for keyword in ['cjk', 'chinese', 'han', 'hei', 'song']):
                    chinese_fonts.append(font)
                    break
        
        if chinese_fonts:
            # 配置matplotlib使用中文字体
            plt.rcParams['font.sans-serif'] = chinese_fonts + ['DejaVu Sans']
            plt.rcParams['axes.unicode_minus'] = False  # 解决负号显示问题
            
            logger.info(f"✅ 已配置中文字体: {chinese_fonts[0]}")
        else:
            # 如果没有找到中文字体，至少配置避免错误
            plt.rcParams['font.sans-serif'] = ['DejaVu Sans']
            plt.rcParams['axes.unicode_minus'] = False
            logger.warning("⚠️ 未找到中文字体，可能会有中文显示问题")
        
        # 清除matplotlib字体缓存
        try:
            # 新版本matplotlib使用的方法
            if hasattr(fm, '_rebuild'):
                fm._rebuild()
            elif hasattr(fm.fontManager, '_rebuild_fontlist'):
                fm.fontManager._rebuild_fontlist()
            else:
                # 删除matplotlib缓存文件，让它重新加载
                import matplotlib as mpl
                cache_dir = mpl.get_cachedir()
                import glob
                cache_files = glob.glob(os.path.join(cache_dir, "*.cache"))
                for cache_file in cache_files:
                    try:
                        os.remove(cache_file)
                    except Exception:
                        pass
        except Exception as cache_error:
            logger.warning(f"清除字体缓存失败: {cache_error}")
        
        return True
        
    except Exception as e:
        logger.error(f"❌ 配置中文字体失败: {str(e)}")
        return False

def suppress_font_warnings():
    """抑制字体相关的警告"""
    # 过滤matplotlib字体警告
    warnings.filterwarnings('ignore', category=UserWarning, module='matplotlib')
    warnings.filterwarnings('ignore', message='.*Glyph.*missing from current font.*')
    
    # 过滤matplotlib分类数据警告
    warnings.filterwarnings('ignore', message='.*Using categorical units to plot a list of strings.*')
    
    # 过滤其他常见的matplotlib警告
    warnings.filterwarnings('ignore', message='.*tight_layout.*')
    warnings.filterwarnings('ignore', message='.*No artists with labels found.*')
    warnings.filterwarnings('ignore', category=FutureWarning, module='matplotlib')
    
    # 设置matplotlib日志级别，减少INFO日志
    import logging
    matplotlib_logger = logging.getLogger('matplotlib')
    matplotlib_logger.setLevel(logging.WARNING)

def get_available_fonts():
    """获取系统可用字体列表"""
    try:
        fonts = sorted([f.name for f in fm.fontManager.ttflist])
        chinese_fonts = [f for f in fonts if any(keyword in f.lower() for keyword in 
                        ['chinese', 'cjk', 'han', 'hei', 'song', 'yahei', 'simhei', 'simsun'])]
        
        return {
            'all_fonts': fonts,
            'chinese_fonts': chinese_fonts,
            'current_font': plt.rcParams['font.sans-serif']
        }
    except Exception as e:
        logger.error(f"获取字体列表失败: {str(e)}")
        return {}

# 自动配置字体
def auto_configure():
    """自动配置matplotlib字体和警告"""
    suppress_font_warnings()
    configure_chinese_fonts()
    
    logger.info("🎨 matplotlib中文字体配置完成")

# 模块导入时自动配置
auto_configure() 