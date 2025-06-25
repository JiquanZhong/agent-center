"""
统一日志系统

提供项目全局的日志配置和管理
"""

import logging
import logging.handlers
import os
import sys
import traceback
from datetime import datetime
from pathlib import Path
from typing import Optional

class RichTracebackFormatter(logging.Formatter):
    """富文本错误堆栈格式化器"""
    
    def format(self, record):
        if record.exc_info:
            # 获取异常信息
            exc_type, exc_value, exc_traceback = record.exc_info
            
            # 构建详细的错误信息
            error_lines = []
            error_lines.append("=" * 80)
            error_lines.append(f"🚨 ERROR OCCURRED: {exc_type.__name__}")
            error_lines.append("=" * 80)
            
            # 添加基本信息
            error_lines.append(f"📅 时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
            error_lines.append(f"📝 消息: {record.getMessage()}")
            error_lines.append(f"🏷️  日志器: {record.name}")
            error_lines.append(f"⚠️  异常类型: {exc_type.__name__}")
            error_lines.append(f"💬 异常消息: {str(exc_value)}")
            error_lines.append("")
            
            # 构建堆栈跟踪
            error_lines.append("📍 TRACEBACK:")
            error_lines.append("─" * 80)
            
            tb_lines = traceback.format_tb(exc_traceback)
            for i, tb_line in enumerate(tb_lines):
                lines = tb_line.strip().split('\n')
                for line in lines:
                    if line.strip():
                        if line.strip().startswith('File'):
                            # 文件信息行
                            error_lines.append(f"  📁 {line.strip()}")
                        else:
                            # 代码行
                            error_lines.append(f"  ❱ {line.strip()}")
            
            error_lines.append("─" * 80)
            error_lines.append("")
            
            # 如果有额外的上下文信息
            if hasattr(record, 'extra_context'):
                error_lines.append("🔍 CONTEXT:")
                error_lines.append("─" * 80)
                for key, value in record.extra_context.items():
                    error_lines.append(f"  {key}: {value}")
                error_lines.append("")
            
            return '\n'.join(error_lines)
        else:
            # 普通错误日志
            return super().format(record)

class ColoredFormatter(logging.Formatter):
    """彩色日志格式化器"""
    
    # ANSI颜色代码
    COLORS = {
        'DEBUG': '\033[36m',    # 青色
        'INFO': '\033[32m',     # 绿色
        'WARNING': '\033[33m',  # 黄色
        'ERROR': '\033[31m',    # 红色
        'CRITICAL': '\033[35m', # 紫色
        'RESET': '\033[0m'      # 重置
    }
    
    def format(self, record):
        # 如果是异常记录且在控制台显示，使用富文本格式
        if record.exc_info and hasattr(self, '_is_console') and self._is_console:
            formatter = RichTracebackFormatter()
            return formatter.format(record)
        
        # 添加颜色
        if record.levelname in self.COLORS:
            record.levelname = f"{self.COLORS[record.levelname]}{record.levelname}{self.COLORS['RESET']}"
        
        # 添加模块名称简化
        if hasattr(record, 'name'):
            parts = record.name.split('.')
            if len(parts) > 2:
                record.short_name = f"{parts[0]}...{parts[-1]}"
            else:
                record.short_name = record.name
        else:
            record.short_name = "unknown"
            
        return super().format(record)

class LoggerManager:
    """日志管理器"""
    
    _instance = None
    _loggers = {}
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if self._initialized:
            return
        
        self._initialized = True
        self.log_dir = Path("logs")
        self.log_dir.mkdir(exist_ok=True)
        
        # 默认配置
        self.default_level = logging.INFO
        self.file_level = logging.DEBUG
        self.console_level = logging.INFO
        
        # 设置根日志器
        self._setup_root_logger()
    
    def _setup_root_logger(self):
        """设置根日志器"""
        root_logger = logging.getLogger()
        root_logger.setLevel(self.default_level)
        
        # 清除现有处理器
        for handler in root_logger.handlers[:]:
            root_logger.removeHandler(handler)
    
    def get_logger(self, name: str, level: Optional[int] = None) -> logging.Logger:
        """获取指定名称的日志器"""
        if name in self._loggers:
            return self._loggers[name]
        
        logger = logging.getLogger(name)
        logger.setLevel(level or self.default_level)
        
        # 防止日志重复
        logger.propagate = False
        
        # 添加控制台处理器
        console_handler = self._create_console_handler()
        logger.addHandler(console_handler)
        
        # 添加文件处理器
        file_handler = self._create_file_handler()
        logger.addHandler(file_handler)
        
        # 添加错误文件处理器
        error_handler = self._create_error_handler()
        logger.addHandler(error_handler)
        
        self._loggers[name] = logger
        return logger
    
    def _create_console_handler(self) -> logging.StreamHandler:
        """创建控制台处理器"""
        handler = logging.StreamHandler(sys.stdout)
        handler.setLevel(self.console_level)
        
        # 彩色格式
        formatter = ColoredFormatter(
            fmt='%(asctime)s | %(levelname)-8s | %(short_name)-20s | %(message)s',
            datefmt='%H:%M:%S'
        )
        # 标记这是控制台格式化器，用于错误显示
        formatter._is_console = True
        handler.setFormatter(formatter)
        return handler
    
    def _create_file_handler(self) -> logging.handlers.RotatingFileHandler:
        """创建文件处理器"""
        log_file = self.log_dir / f"ask_data_{datetime.now().strftime('%Y%m%d')}.log"
        
        handler = logging.handlers.RotatingFileHandler(
            log_file,
            maxBytes=50 * 1024 * 1024,  # 50MB
            backupCount=5,
            encoding='utf-8'
        )
        handler.setLevel(self.file_level)
        
        # 文件格式（不包含颜色）
        formatter = logging.Formatter(
            fmt='%(asctime)s | %(levelname)-8s | %(name)-30s | %(funcName)-15s | %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        handler.setFormatter(formatter)
        return handler
    
    def _create_error_handler(self) -> logging.handlers.RotatingFileHandler:
        """创建错误文件处理器"""
        error_file = self.log_dir / f"ask_data_error_{datetime.now().strftime('%Y%m%d')}.log"
        
        handler = logging.handlers.RotatingFileHandler(
            error_file,
            maxBytes=10 * 1024 * 1024,  # 10MB
            backupCount=3,
            encoding='utf-8'
        )
        handler.setLevel(logging.ERROR)
        
        # 使用富文本错误格式化器
        formatter = RichTracebackFormatter()
        handler.setFormatter(formatter)
        return handler
    
    def set_level(self, level: int):
        """设置全局日志级别"""
        self.default_level = level
        for logger in self._loggers.values():
            logger.setLevel(level)
    
    def set_console_level(self, level: int):
        """设置控制台日志级别"""
        self.console_level = level
        for logger in self._loggers.values():
            for handler in logger.handlers:
                if isinstance(handler, logging.StreamHandler) and not isinstance(handler, logging.handlers.RotatingFileHandler):
                    handler.setLevel(level)
    
    def set_file_level(self, level: int):
        """设置文件日志级别"""
        self.file_level = level
        for logger in self._loggers.values():
            for handler in logger.handlers:
                if isinstance(handler, logging.handlers.RotatingFileHandler):
                    if 'error' not in str(handler.baseFilename):
                        handler.setLevel(level)

# 全局日志管理器实例
logger_manager = LoggerManager()

def get_logger(name: str = None, level: Optional[int] = None) -> logging.Logger:
    """
    获取日志器的便捷函数
    
    Args:
        name: 日志器名称，默认使用调用模块名
        level: 日志级别
    
    Returns:
        配置好的日志器实例
    """
    if name is None:
        # 自动获取调用模块名
        import inspect
        frame = inspect.currentframe().f_back
        name = frame.f_globals.get('__name__', 'unknown')
    
    return logger_manager.get_logger(name, level)

def setup_api_logging(debug: bool = False):
    """
    设置API服务的日志配置
    
    Args:
        debug: 是否启用调试模式
    """
    if debug:
        logger_manager.set_level(logging.DEBUG)
        logger_manager.set_console_level(logging.DEBUG)
    else:
        logger_manager.set_level(logging.INFO)
        logger_manager.set_console_level(logging.INFO)
    
    # 设置第三方库日志级别
    logging.getLogger('uvicorn').setLevel(logging.INFO)
    logging.getLogger('uvicorn.access').setLevel(logging.WARNING)
    logging.getLogger('fastapi').setLevel(logging.INFO)
    logging.getLogger('pandasai').setLevel(logging.WARNING)
    logging.getLogger('openai').setLevel(logging.WARNING)
    logging.getLogger('psycopg2').setLevel(logging.WARNING)

def log_function_call(func):
    """
    函数调用日志装饰器
    """
    def wrapper(*args, **kwargs):
        logger = get_logger(func.__module__)
        logger.debug(f"调用函数: {func.__name__}(args={len(args)}, kwargs={list(kwargs.keys())})")
        
        try:
            result = func(*args, **kwargs)
            logger.debug(f"函数 {func.__name__} 执行成功")
            return result
        except Exception as e:
            logger.error(f"函数 {func.__name__} 执行失败: {str(e)}", exc_info=True)
            raise
    
    return wrapper

def log_execution_time(func):
    """
    执行时间日志装饰器
    """
    import time
    
    def wrapper(*args, **kwargs):
        logger = get_logger(func.__module__)
        start_time = time.time()
        
        try:
            result = func(*args, **kwargs)
            execution_time = time.time() - start_time
            logger.info(f"函数 {func.__name__} 执行时间: {execution_time:.2f}秒")
            return result
        except Exception as e:
            execution_time = time.time() - start_time
            logger.error(f"函数 {func.__name__} 执行失败 (耗时: {execution_time:.2f}秒): {str(e)}")
            raise
    
    return wrapper

class LogContext:
    """日志上下文管理器"""
    
    def __init__(self, logger, message: str, level: int = logging.INFO):
        self.logger = logger
        self.message = message
        self.level = level
        self.start_time = None
    
    def __enter__(self):
        self.start_time = datetime.now()
        self.logger.log(self.level, f"开始: {self.message}")
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        duration = datetime.now() - self.start_time
        if exc_type is None:
            self.logger.log(self.level, f"完成: {self.message} (耗时: {duration.total_seconds():.2f}秒)")
        else:
            self.logger.error(f"失败: {self.message} (耗时: {duration.total_seconds():.2f}秒) - {exc_val}")

def log_exception(logger, message: str = "", **context):
    """
    记录异常的便捷函数
    
    Args:
        logger: 日志器实例
        message: 附加消息
        **context: 额外的上下文信息
    """
    import sys
    
    # 获取当前异常信息
    exc_info = sys.exc_info()
    if exc_info[0] is None:
        logger.error("调用 log_exception 但没有活跃的异常")
        return
    
    # 创建带有上下文的日志记录
    extra = {'extra_context': context} if context else {}
    logger.error(message or "发生异常", exc_info=True, extra=extra)

# 导出主要接口
__all__ = [
    'get_logger',
    'setup_api_logging', 
    'log_function_call',
    'log_execution_time',
    'log_exception',
    'LogContext',
    'logger_manager'
]