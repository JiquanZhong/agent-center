"""
API依赖注入

提供数据库连接、配置等依赖
"""

from fastapi import Depends, HTTPException, status
from functools import lru_cache
import time

from ..config.settings import Settings
from ..utils.schema_database import SchemaDatabase
from ..core.llm_adapter import CustomOpenAI

# 全局变量
_start_time = time.time()

@lru_cache()
def get_settings() -> Settings:
    """获取系统设置单例"""
    try:
        return Settings()
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"配置加载失败: {str(e)}"
        )

def get_database(settings: Settings = Depends(get_settings)) -> SchemaDatabase:
    """获取数据库连接"""
    try:
        return SchemaDatabase(settings)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"数据库连接失败: {str(e)}"
        )

def get_llm(settings: Settings = Depends(get_settings)) -> CustomOpenAI:
    """获取LLM实例"""
    try:
        llm_config = settings.get_llm_config()
        return CustomOpenAI(**llm_config)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"LLM初始化失败: {str(e)}"
        )

def get_uptime() -> float:
    """获取服务运行时间"""
    return time.time() - _start_time