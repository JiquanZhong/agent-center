"""
列配置管理API路由

提供数据集列配置的管理功能
"""

from fastapi import APIRouter, Depends, HTTPException, status
from typing import List, Optional, Dict, Any
from datetime import datetime

from ..models import (
    ColumnConfigCreateRequest, ColumnConfigUpdateRequest, ColumnConfig,
    StandardResponse, PaginatedResponse
)
from ..dependencies import get_database
from ...utils.schema_database import SchemaDatabase
from ...utils.logger import get_logger

router = APIRouter(prefix="/columns", tags=["列配置管理"])
logger = get_logger(__name__)

@router.post("", response_model=StandardResponse, summary="创建列配置")
async def create_column_config(
    config: ColumnConfigCreateRequest,
    db: SchemaDatabase = Depends(get_database)
):
    """创建新的列配置"""
    try:
        # 验证数据集是否存在
        dataset = db.get_dataset_by_id(str(config.dataset_id))
        if not dataset:
            return StandardResponse(
                success=False,
                message="数据集不存在",
                error=f"数据集 {config.dataset_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 创建列配置
        column_data = {
            'name': config.name,
            'type': config.type,
            'description': config.description,
            'alias': config.alias,
            'sort_order': config.sort_order,
            'is_required': config.is_required,
            'default_value': config.default_value
        }
        
        column_config = db.create_column_config(str(config.dataset_id), column_data)
        if column_config:
            return StandardResponse(
                success=True,
                message="列配置创建成功",
                data=column_config,
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="列配置创建失败",
                error="数据库操作失败",
                timestamp=datetime.utcnow().isoformat()
            )
    except Exception as e:
        logger.error(f"创建列配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="创建列配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/{column_id}", response_model=StandardResponse, summary="获取列配置详情")
async def get_column_config(
    column_id: int,
    db: SchemaDatabase = Depends(get_database)
):
    """获取指定列配置的详细信息"""
    try:
        column_config = db.get_column_config(column_id)
        if not column_config:
            return StandardResponse(
                success=False,
                message="列配置不存在",
                error=f"列配置 {column_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        return StandardResponse(
            success=True,
            message="获取列配置成功",
            data=column_config,
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"获取列配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="获取列配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.put("/{column_id}", response_model=StandardResponse, summary="更新列配置")
async def update_column_config(
    column_id: int,
    config: ColumnConfigUpdateRequest,
    db: SchemaDatabase = Depends(get_database)
):
    """更新指定列配置的信息"""
    try:
        # 验证列配置是否存在
        existing_config = db.get_column_config(column_id)
        if not existing_config:
            return StandardResponse(
                success=False,
                message="列配置不存在",
                error=f"列配置 {column_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 构建更新数据
        update_data = {}
        if config.name is not None:
            update_data['name'] = config.name
        if config.type is not None:
            update_data['type'] = config.type
        if config.description is not None:
            update_data['description'] = config.description
        if config.alias is not None:
            update_data['alias'] = config.alias
        if config.sort_order is not None:
            update_data['sort_order'] = config.sort_order
        if config.is_required is not None:
            update_data['is_required'] = config.is_required
        if config.default_value is not None:
            update_data['default_value'] = config.default_value
        
        # 更新列配置
        updated_config = db.update_column_config(column_id, update_data)
        if updated_config:
            return StandardResponse(
                success=True,
                message="列配置更新成功",
                data=updated_config,
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="列配置更新失败",
                error="数据库操作失败",
                timestamp=datetime.utcnow().isoformat()
            )
    except Exception as e:
        logger.error(f"更新列配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="更新列配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.delete("/{column_id}", response_model=StandardResponse, summary="删除列配置")
async def delete_column_config(
    column_id: int,
    db: SchemaDatabase = Depends(get_database)
):
    """删除指定的列配置"""
    try:
        # 验证列配置是否存在
        existing_config = db.get_column_config(column_id)
        if not existing_config:
            return StandardResponse(
                success=False,
                message="列配置不存在",
                error=f"列配置 {column_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 删除列配置
        success = db.delete_column_config(column_id)
        if success:
            return StandardResponse(
                success=True,
                message="列配置删除成功",
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="列配置删除失败",
                error="数据库操作失败",
                timestamp=datetime.utcnow().isoformat()
            )
    except Exception as e:
        logger.error(f"删除列配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="删除列配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/dataset/{dataset_id}", response_model=PaginatedResponse, summary="获取数据集的所有列配置")
async def list_dataset_columns(
    dataset_id: str,
    page: int = 1,
    per_page: int = 20,
    db: SchemaDatabase = Depends(get_database)
):
    """获取指定数据集的所有列配置"""
    try:
        # 验证数据集是否存在
        dataset = db.get_dataset_by_id(str(dataset_id))
        if not dataset:
            return PaginatedResponse(
                success=False,
                message="数据集不存在",
                data=[],
                pagination={
                    "total": 0,
                    "page": page,
                    "per_page": per_page,
                    "pages": 0
                },
                error=f"数据集 {dataset_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 获取列配置列表
        all_columns = db.list_dataset_columns(str(dataset_id))
        
        # 手动实现分页
        total = len(all_columns)
        start_idx = (page - 1) * per_page
        end_idx = start_idx + per_page
        columns = all_columns[start_idx:end_idx]
        
        return PaginatedResponse(
            success=True,
            message="获取列配置列表成功",
            data=columns,
            pagination={
                "total": total,
                "page": page,
                "per_page": per_page,
                "pages": (total + per_page - 1) // per_page
            },
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"获取列配置列表失败: {str(e)}")
        return PaginatedResponse(
            success=False,
            message="获取列配置列表失败",
            data=[],
            pagination={
                "total": 0,
                "page": page,
                "per_page": per_page,
                "pages": 0
            },
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        ) 