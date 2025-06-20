"""
API响应工具类

统一API响应格式，确保所有接口返回格式一致
"""

from typing import Any, Optional, List, Dict
from datetime import datetime
from .models import StandardResponse, PaginatedResponse

class ResponseFormatter:
    """响应格式化工具类"""
    
    @staticmethod
    def success(
        message: str,
        data: Optional[Any] = None,
        timestamp: Optional[str] = None
    ) -> StandardResponse:
        """
        创建成功响应
        
        Args:
            message: 成功消息
            data: 响应数据
            timestamp: 时间戳
            
        Returns:
            StandardResponse: 标准成功响应
        """
        if timestamp is None:
            timestamp = datetime.utcnow().isoformat() + "Z"
            
        return StandardResponse(
            success=True,
            message=message,
            data=data,
            error=None,
            timestamp=timestamp
        )
    
    @staticmethod
    def error(
        message: str,
        error: Optional[str] = None,
        data: Optional[Any] = None,
        timestamp: Optional[str] = None
    ) -> StandardResponse:
        """
        创建错误响应
        
        Args:
            message: 错误消息
            error: 详细错误信息
            data: 响应数据
            timestamp: 时间戳
            
        Returns:
            StandardResponse: 标准错误响应
        """
        if timestamp is None:
            timestamp = datetime.utcnow().isoformat() + "Z"
            
        return StandardResponse(
            success=False,
            message=message,
            data=data,
            error=error,
            timestamp=timestamp
        )
    
    @staticmethod
    def paginated(
        message: str,
        data: List[Any],
        total: int,
        page: int = 1,
        per_page: int = 20,
        timestamp: Optional[str] = None
    ) -> PaginatedResponse:
        """
        创建分页响应
        
        Args:
            message: 响应消息
            data: 数据列表
            total: 总记录数
            page: 当前页码
            per_page: 每页记录数
            timestamp: 时间戳
            
        Returns:
            PaginatedResponse: 分页响应
        """
        if timestamp is None:
            timestamp = datetime.utcnow().isoformat() + "Z"
            
        pages = (total + per_page - 1) // per_page  # 向上取整
        
        return PaginatedResponse(
            success=True,
            message=message,
            data=data,
            pagination={
                "total": total,
                "page": page,
                "per_page": per_page,
                "pages": pages,
                "has_next": page < pages,
                "has_prev": page > 1
            },
            error=None,
            timestamp=timestamp
        )

# 快捷方法
def success_response(message: str, data: Optional[Any] = None) -> StandardResponse:
    """快捷创建成功响应"""
    return ResponseFormatter.success(message, data)

def error_response(message: str, error: Optional[str] = None, data: Optional[Any] = None) -> StandardResponse:
    """快捷创建错误响应"""
    return ResponseFormatter.error(message, error, data)

def paginated_response(message: str, data: List[Any], total: int, page: int = 1, per_page: int = 20) -> PaginatedResponse:
    """快捷创建分页响应"""
    return ResponseFormatter.paginated(message, data, total, page, per_page)

# 常用响应消息
class Messages:
    """常用响应消息常量"""
    
    # 成功消息
    UPLOAD_SUCCESS = "文件上传成功"
    UPDATE_SUCCESS = "更新成功"
    DELETE_SUCCESS = "删除成功"
    CREATE_SUCCESS = "创建成功"
    GET_SUCCESS = "获取成功"
    OPERATION_SUCCESS = "操作成功"
    
    # 错误消息
    NOT_FOUND = "资源不存在"
    INVALID_REQUEST = "请求参数无效"
    INTERNAL_ERROR = "服务器内部错误"
    PERMISSION_DENIED = "权限不足"
    VALIDATION_ERROR = "数据验证失败"
    
    # 数据集相关
    DATASET_NOT_FOUND = "数据集不存在"
    DATASET_CREATE_SUCCESS = "数据集创建成功"
    DATASET_UPDATE_SUCCESS = "数据集更新成功"
    DATASET_DELETE_SUCCESS = "数据集删除成功"
    DATASET_LIST_SUCCESS = "获取数据集列表成功"
    
    # Schema相关
    SCHEMA_UPDATE_SUCCESS = "语义配置更新成功"
    SCHEMA_GET_SUCCESS = "获取语义配置成功"
    SCHEMA_NOT_FOUND = "语义配置不存在"
    
    # Transformations相关
    TRANSFORMATIONS_GET_SUCCESS = "获取转换配置成功"
    TRANSFORMATIONS_UPDATE_SUCCESS = "转换配置更新成功"
    TRANSFORMATION_ADD_SUCCESS = "转换配置添加成功"
    TRANSFORMATION_DELETE_SUCCESS = "转换配置删除成功"
    TRANSFORMATION_TOGGLE_SUCCESS = "转换配置状态切换成功"
    TRANSFORMATIONS_AUTO_GENERATE_SUCCESS = "自动生成转换配置成功"
    
    # 查询相关
    QUERY_SUCCESS = "查询执行成功"
    QUERY_ERROR = "查询执行失败" 