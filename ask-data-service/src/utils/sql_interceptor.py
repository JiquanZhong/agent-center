"""
SQL执行拦截器模块

用于拦截和记录PandasAI执行的SQL查询
"""

import ast
import re
import logging
import functools
from typing import List, Dict, Any, Optional, Callable
from datetime import datetime

logger = logging.getLogger(__name__)

class SQLInterceptor:
    """SQL执行拦截器类"""
    
    def __init__(self):
        self.current_sql: Optional[str] = None
        self.original_add_to_env = None
        self._enabled = False
        
    def start_intercepting(self):
        """开始拦截SQL执行"""
        if self._enabled:
            logger.debug("SQL拦截器已经启用")
            return
            
        try:
            # 拦截CodeExecutor.add_to_env方法来包装execute_sql_query函数
            from pandasai.core.code_execution.code_executor import CodeExecutor
            
            # 保存原始方法
            self.original_add_to_env = CodeExecutor.add_to_env
            
            # 创建拦截方法
            def intercepted_add_to_env(self, key: str, value):
                # 如果是execute_sql_query函数，包装它
                if key == "execute_sql_query" and callable(value):
                    def wrapped_execute_sql_query(sql_query: str):
                        # 记录SQL查询
                        _global_interceptor._record_sql(sql_query)
                        
                        # 调用原始函数
                        return value(sql_query)
                    
                    # 添加包装后的函数到环境
                    return _global_interceptor.original_add_to_env(self, key, wrapped_execute_sql_query)
                else:
                    # 其他变量正常添加
                    return _global_interceptor.original_add_to_env(self, key, value)
            
            # 替换方法
            CodeExecutor.add_to_env = intercepted_add_to_env
                
            self._enabled = True
            logger.debug("✅ SQL拦截器已启用")
            
        except Exception as e:
            logger.error(f"启用SQL拦截器失败: {e}")
            
    def stop_intercepting(self):
        """停止拦截SQL执行"""
        if not self._enabled:
            return
            
        try:
            if hasattr(self, 'original_add_to_env') and self.original_add_to_env:
                from pandasai.core.code_execution.code_executor import CodeExecutor
                CodeExecutor.add_to_env = self.original_add_to_env
                
            self._enabled = False
            logger.debug("✅ SQL拦截器已停用")
            
        except Exception as e:
            logger.error(f"停用SQL拦截器失败: {e}")
            
    def _record_sql(self, sql_query: str):
        """记录SQL查询（只保存最新的）"""
        self.current_sql = sql_query.strip()
        logger.debug(f"🔍 拦截到SQL查询: {sql_query[:100]}...")
        
    def get_current_query_sqls(self) -> List[Dict[str, Any]]:
        """获取当前查询执行的SQL列表（为了保持API兼容性）"""
        if self.current_sql:
            return [{
                "sql": self.current_sql,
                "timestamp": datetime.now().isoformat(),
                "execution_order": 1
            }]
        return []
        
    def get_latest_sql(self) -> Optional[str]:
        """获取最新执行的SQL"""
        return self.current_sql
        
    def clear_current_query(self):
        """清空当前查询的SQL记录"""
        self.current_sql = None
        logger.debug("📝 当前查询SQL记录已清空")
        
    def is_enabled(self) -> bool:
        """检查拦截器是否启用"""
        return self._enabled

class CodeSQLExtractor:
    """从生成的代码中提取SQL查询的工具类"""
    
    @staticmethod
    def extract_sql_from_code(code: str) -> List[str]:
        """
        从Python代码中提取SQL查询
        
        Args:
            code: Python代码字符串
            
        Returns:
            提取到的SQL查询列表
        """
        sqls = []
        
        try:
            # 解析代码为AST
            tree = ast.parse(code)
            
            # 遍历AST节点
            for node in ast.walk(tree):
                # 查找execute_sql_query函数调用
                if isinstance(node, ast.Call):
                    if (isinstance(node.func, ast.Name) and 
                        node.func.id == "execute_sql_query" and 
                        node.args):
                        
                        # 提取SQL字符串
                        sql_arg = node.args[0]
                        if isinstance(sql_arg, ast.Constant) and isinstance(sql_arg.value, str):
                            sqls.append(sql_arg.value.strip())
                            
                # 查找SQL赋值语句
                elif isinstance(node, ast.Assign):
                    for target in node.targets:
                        if (isinstance(target, ast.Name) and 
                            target.id in ['sql_query', 'query', 'sql'] and
                            isinstance(node.value, ast.Constant) and 
                            isinstance(node.value.value, str)):
                            
                            potential_sql = node.value.value.strip()
                            if CodeSQLExtractor._is_likely_sql(potential_sql):
                                sqls.append(potential_sql)
                                
        except Exception as e:
            logger.error(f"从代码提取SQL失败: {e}")
            
        return sqls
    
    @staticmethod
    def _is_likely_sql(text: str) -> bool:
        """
        判断文本是否可能是SQL查询
        
        Args:
            text: 要检查的文本
            
        Returns:
            是否可能是SQL
        """
        text_upper = text.upper().strip()
        sql_keywords = [
            'SELECT', 'FROM', 'WHERE', 'GROUP BY', 'ORDER BY',
            'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'ALTER', 'DROP'
        ]
        
        return any(keyword in text_upper for keyword in sql_keywords)

# 全局拦截器实例
_global_interceptor = SQLInterceptor()

def get_sql_interceptor() -> SQLInterceptor:
    """获取全局SQL拦截器实例"""
    return _global_interceptor

def start_sql_interception():
    """启动SQL拦截"""
    _global_interceptor.start_intercepting()

def stop_sql_interception():
    """停止SQL拦截"""
    _global_interceptor.stop_intercepting()

def get_current_query_sqls() -> List[Dict[str, Any]]:
    """获取当前查询执行的SQL列表"""
    return _global_interceptor.get_current_query_sqls()

def get_latest_sql() -> Optional[str]:
    """获取最新执行的SQL"""
    return _global_interceptor.get_latest_sql()

def clear_current_query():
    """清空当前查询的SQL记录"""
    _global_interceptor.clear_current_query() 