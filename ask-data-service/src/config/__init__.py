"""
配置管理模块

包含系统配置、字体配置等
"""

from .settings import Settings
from .font_config import setup_chinese_font

__all__ = ['Settings', 'setup_chinese_font'] 