"""
API数据模型定义

定义API请求和响应的数据结构
"""

from pydantic import BaseModel, Field
from typing import Dict, Any, Optional, List, Union
from enum import Enum
from datetime import datetime
import uuid

class DatasetStatus(str, Enum):
    """数据集状态枚举"""
    ACTIVE = "active"
    INACTIVE = "inactive"

class ColumnCategory(str, Enum):
    """列类型枚举"""
    METRIC = "metric"      # 指标
    DIMENSION = "dimension"  # 维度

# ====================== 标准响应模型 ======================

class StandardResponse(BaseModel):
    """标准响应模型 - 所有API统一使用此格式"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    data: Optional[Any] = Field(None, description="响应数据")
    error: Optional[str] = Field(None, description="错误信息")
    timestamp: Optional[str] = Field(None, description="响应时间戳")
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "操作成功",
                "data": {},
                "error": None,
                "timestamp": "2024-01-01T00:00:00Z"
            }
        }

class PaginatedResponse(BaseModel):
    """分页响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    data: List[Any] = Field(..., description="数据列表")
    pagination: Dict[str, Any] = Field(..., description="分页信息")
    error: Optional[str] = Field(None, description="错误信息")
    timestamp: Optional[str] = Field(None, description="响应时间戳")
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "获取列表成功",
                "data": [],
                "pagination": {
                    "total": 100,
                    "page": 1,
                    "per_page": 20,
                    "pages": 5
                },
                "error": None,
                "timestamp": "2024-01-01T00:00:00Z"
            }
        }

# ====================== 数据集相关模型 ======================

class QueryRequest(BaseModel):
    """查询请求模型"""
    dataset_id: str = Field(..., description="数据集ID")
    question: str = Field(..., description="自然语言查询问题")
    query_id: Optional[str] = Field(None, description="查询ID，用于标识此次查询")
    context: Optional[Dict[str, Any]] = Field(None, description="查询上下文")
    
    class Config:
        json_schema_extra = {
            "example": {
                "dataset_id": "23",
                "question": "我想查询2016年江西省耕地面积？",
                "query_id": "query_123456"
            }
        }

class QueryResponse(BaseModel):
    """查询响应模型"""
    success: bool = Field(..., description="查询是否成功")
    message: str = Field(..., description="响应消息")
    data: Dict[str, Any] = Field(..., description="查询结果数据")
    error: Optional[str] = Field(None, description="错误信息")
    timestamp: Optional[str] = Field(None, description="响应时间戳")
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "查询执行成功",
                "data": {
                    "result": "数据包含768行记录",
                    "result_type": "string",
                    "execution_time": 1.23,
                    "query_id": "query_123456",
                    "have_chart": True,
                    "chart_base64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                    "dataset_info": {
                        "dataset_id": "land-supply-provincial-monthly",
                        "row_count": 768,
                        "column_count": 18
                    }
                },
                "error": None,
                "timestamp": "2024-01-01T00:00:00Z"
            }
        }

class DatasetCreateRequest(BaseModel):
    """数据集创建请求模型"""
    name: str = Field(..., description="数据集名称")
    description: Optional[str] = Field(None, description="数据集描述")
    file_path: str = Field(..., description="数据文件路径")
    auto_generate_schema: bool = Field(True, description="是否自动生成schema配置")
    
    class Config:
        json_schema_extra = {
            "example": {
                "name": "土地供应数据",
                "description": "各省份土地供应统计数据",
                "file_path": "datasets/land_data.csv",
                "auto_generate_schema": True
            }
        }

class DatasetUpdateRequest(BaseModel):
    """数据集更新请求模型"""
    name: Optional[str] = Field(None, description="数据集名称")
    description: Optional[str] = Field(None, description="数据集描述")
    file_path: Optional[str] = Field(None, description="数据文件路径")
    status: Optional[DatasetStatus] = Field(None, description="数据集状态")

class DatasetInfo(BaseModel):
    """数据集信息模型"""
    id: str = Field(..., description="数据集ID")
    name: str = Field(..., description="数据集名称")
    description: Optional[str] = Field(None, description="数据集描述")
    file_path: Optional[str] = Field(None, description="数据文件路径")
    status: str = Field(..., description="数据集状态")
    has_schema: bool = Field(False, description="是否有语义配置")
    created_at: Optional[str] = Field(None, description="创建时间")
    updated_at: Optional[str] = Field(None, description="更新时间")
    created_by: Optional[str] = Field(None, description="创建者")
    
    class Config:
        json_schema_extra = {
            "example": {
                "id": "19",
                "name": "土地数据",
                "description": "包含土地利用信息的数据集",
                "file_path": "datasets/land_data.csv",
                "status": "active",
                "has_schema": True,
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-01T00:00:00Z",
                "created_by": "system"
            }
        }

# ====================== 列配置相关模型 ======================

class ColumnConfigCreateRequest(BaseModel):
    """列配置创建请求模型"""
    dataset_id: str = Field(..., description="数据集ID")
    name: str = Field(..., description="列名")
    type: str = Field(..., description="数据类型")
    description: Optional[str] = Field(None, description="列描述")
    alias: Optional[str] = Field(None, description="列别名")
    sort_order: Optional[int] = Field(0, description="排序顺序")
    is_required: Optional[bool] = Field(False, description="是否必填")
    default_value: Optional[str] = Field(None, description="默认值")
    column_category: Optional[ColumnCategory] = Field(ColumnCategory.METRIC, description="列类型（metric：指标，dimension：维度）")
    dictionary_id: Optional[str] = Field(None, description="字典ID")

class ColumnConfigUpdateRequest(BaseModel):
    """列配置更新请求模型"""
    name: Optional[str] = Field(None, description="列名")
    type: Optional[str] = Field(None, description="数据类型")
    description: Optional[str] = Field(None, description="列描述")
    alias: Optional[str] = Field(None, description="列别名")
    sort_order: Optional[int] = Field(None, description="排序顺序")
    is_required: Optional[bool] = Field(None, description="是否必填")
    default_value: Optional[str] = Field(None, description="默认值")
    column_category: Optional[ColumnCategory] = Field(None, description="列类型（metric：指标，dimension：维度）")
    dictionary_id: Optional[str] = Field(None, description="字典ID")

class ColumnConfig(BaseModel):
    """列配置模型"""
    id: str = Field(..., description="列配置ID")
    dataset_id: str = Field(..., description="数据集ID")
    name: str = Field(..., description="列名")
    type: str = Field(..., description="数据类型")
    description: Optional[str] = Field(None, description="列描述")
    alias: Optional[str] = Field(None, description="列别名")
    sort_order: Optional[int] = Field(0, description="排序顺序")
    is_required: Optional[bool] = Field(False, description="是否必填")
    default_value: Optional[str] = Field(None, description="默认值")
    column_category: Optional[ColumnCategory] = Field(ColumnCategory.METRIC, description="列类型（metric：指标，dimension：维度）")
    dictionary_id: Optional[str] = Field(None, description="字典ID")
    created_at: Optional[str] = Field(None, description="创建时间")
    updated_at: Optional[str] = Field(None, description="更新时间")

# ====================== 转换配置相关模型 ======================

class TransformationConfigCreateRequest(BaseModel):
    """转换配置创建请求模型"""
    dataset_id: str = Field(..., description="数据集ID")
    name: str = Field(..., description="转换名称")
    type: str = Field(..., description="转换类型")
    params: Dict[str, Any] = Field(..., description="转换参数")
    enabled: bool = Field(True, description="是否启用")
    description: Optional[str] = Field(None, description="转换描述")

class TransformationConfigUpdateRequest(BaseModel):
    """转换配置更新请求模型"""
    name: Optional[str] = Field(None, description="转换名称")
    type: Optional[str] = Field(None, description="转换类型")
    params: Optional[Dict[str, Any]] = Field(None, description="转换参数")
    enabled: Optional[bool] = Field(None, description="是否启用")
    description: Optional[str] = Field(None, description="转换描述")

class TransformationConfig(BaseModel):
    """转换配置模型"""
    id: str = Field(..., description="转换配置ID")
    dataset_id: str = Field(..., description="数据集ID")
    name: str = Field(..., description="转换名称")
    type: str = Field(..., description="转换类型")
    params: Dict[str, Any] = Field(..., description="转换参数")
    enabled: bool = Field(True, description="是否启用")
    description: Optional[str] = Field(None, description="转换描述")
    created_at: Optional[str] = Field(None, description="创建时间")
    updated_at: Optional[str] = Field(None, description="更新时间")
    
    class Config:
        json_schema_extra = {
            "example": {
                "id": "1",
                "dataset_id": "19",
                "name": "提取县级代码",
                "type": "extract",
                "params": {
                    "column": "BSM",
                    "pattern": "^(\\d{6})",
                    "new_column": "县级代码"
                },
                "enabled": True,
                "description": "从BSM中提取前6位作为县级代码",
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-01T00:00:00Z"
            }
        }

# ====================== 系统状态模型 ======================

class SystemStatus(BaseModel):
    """系统状态模型"""
    status: str = Field(..., description="系统状态")
    uptime: str = Field(..., description="运行时间")
    version: str = Field(..., description="版本信息")
    environment: str = Field(..., description="运行环境")
    
class DatabaseInfo(BaseModel):
    """数据库信息模型"""
    status: str = Field(..., description="数据库状态")
    connection: str = Field(..., description="连接状态")
    datasets_count: int = Field(..., description="数据集数量")

# ====================== 树节点相关模型 ======================

class TreeNodeCreateRequest(BaseModel):
    """树节点创建请求模型"""
    name: str = Field(..., description="节点名称")
    description: Optional[str] = Field(None, description="节点描述")
    pid: Optional[str] = Field(None, description="父节点ID")
    sort_order: Optional[int] = Field(0, description="排序顺序")
    
    class Config:
        json_schema_extra = {
            "example": {
                "name": "QA库",
                "description": "问答知识库",
                "pid": "0",
                "sort_order": 0
            }
        }

class TreeNodeUpdateRequest(BaseModel):
    """树节点更新请求模型"""
    name: Optional[str] = Field(None, description="节点名称")
    description: Optional[str] = Field(None, description="节点描述")
    sort_order: Optional[int] = Field(None, description="排序顺序")

class TreeNode(BaseModel):
    """树节点模型"""
    id: str = Field(..., description="节点ID")
    name: str = Field(..., description="节点名称")
    description: Optional[str] = Field(None, description="节点描述")
    pid: Optional[str] = Field(None, description="父节点ID")
    level: int = Field(..., description="节点层级")
    sort_order: int = Field(0, description="排序顺序")
    children: Optional[List['TreeNode']] = Field(None, description="子节点列表")
    create_time: Optional[str] = Field(None, description="创建时间")
    update_time: Optional[str] = Field(None, description="更新时间")
    
    class Config:
        json_schema_extra = {
            "example": {
                "id": "279d060b34298f838ffa28d6479644cd",
                "name": "QA库",
                "description": "问答知识库",
                "pid": "0",
                "level": 1,
                "sort_order": 0,
                "children": [],
                "create_time": "2024-01-01T00:00:00Z",
                "update_time": "2024-01-01T00:00:00Z"
            }
        }

# 解决前向引用问题
TreeNode.model_rebuild()

# ====================== 向后兼容 ======================

# 保留旧的模型名称以向后兼容
BaseResponse = StandardResponse
DatasetListResponse = PaginatedResponse
ErrorResponse = StandardResponse

# ====================== 数据分页查看相关模型 ======================

class DataPageRequest(BaseModel):
    """数据分页查看请求模型"""
    dataset_id: str = Field(..., description="数据集ID")
    page: int = Field(1, ge=1, description="页码，从1开始")
    per_page: int = Field(20, ge=1, le=200, description="每页记录数，最大200")
    
    class Config:
        json_schema_extra = {
            "example": {
                "dataset_id": "23",
                "page": 1,
                "per_page": 20
            }
        }

class DataPageInfo(BaseModel):
    """数据分页信息模型"""
    headers: List[str] = Field(..., description="CSV文件的列头")
    data: List[List[Any]] = Field(..., description="分页数据记录")
    total_rows: int = Field(..., description="总行数")
    total_columns: int = Field(..., description="总列数")
    current_page: int = Field(..., description="当前页码")
    per_page: int = Field(..., description="每页记录数")
    total_pages: int = Field(..., description="总页数")
    
    class Config:
        json_schema_extra = {
            "example": {
                "headers": ["列1", "列2", "列3"],
                "data": [
                    ["值1", "值2", "值3"],
                    ["值4", "值5", "值6"]
                ],
                "total_rows": 1000,
                "total_columns": 3,
                "current_page": 1,
                "per_page": 20,
                "total_pages": 50
            }
        }

class DataPageResponse(BaseModel):
    """数据分页查看响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    data: DataPageInfo = Field(..., description="分页数据信息")
    error: Optional[str] = Field(None, description="错误信息")
    timestamp: Optional[str] = Field(None, description="响应时间戳")
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "message": "获取数据成功",
                "data": {
                    "headers": ["列1", "列2", "列3"],
                    "data": [
                        ["值1", "值2", "值3"],
                        ["值4", "值5", "值6"]
                    ],
                    "total_rows": 1000,
                    "total_columns": 3,
                    "current_page": 1,
                    "per_page": 20,
                    "total_pages": 50
                },
                "error": None,
                "timestamp": "2024-01-01T00:00:00Z"
            }
        } 