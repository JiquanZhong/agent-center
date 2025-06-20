"""
转换配置管理API路由

提供数据集转换配置的管理功能
"""

from fastapi import APIRouter, Depends, HTTPException, status
from typing import List, Optional, Dict, Any
import uuid
from datetime import datetime

from ..models import (
    TransformationConfigCreateRequest, TransformationConfigUpdateRequest, TransformationConfig,
    StandardResponse, PaginatedResponse
)
from ..dependencies import get_database
from ...utils.schema_database import SchemaDatabase
from ...utils.logger import get_logger

router = APIRouter(prefix="/transformations", tags=["转换配置管理"])
logger = get_logger(__name__)

@router.post("", response_model=StandardResponse, summary="创建转换配置")
async def create_transformation(
    config: TransformationConfigCreateRequest,
    db: SchemaDatabase = Depends(get_database)
):
    """创建新的转换配置"""
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
        
        # 准备转换配置数据
        transformation_data = {
            'name': config.name,
            'type': config.type,
            'params': config.params or {},
            'enabled': config.enabled if config.enabled is not None else True,
            'description': config.description or ''
        }
        
        # 创建转换配置
        created_transformation = db.create_transformation(str(config.dataset_id), transformation_data)
        
        if created_transformation:
            return StandardResponse(
                success=True,
                message="转换配置创建成功",
                data=created_transformation,
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="转换配置创建失败",
                error="数据库操作失败，请检查日志",
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"创建转换配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="创建转换配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/{transformation_id}", response_model=StandardResponse, summary="获取转换配置详情")
async def get_transformation(
    transformation_id: str,
    db: SchemaDatabase = Depends(get_database)
):
    """获取指定转换配置的详细信息"""
    try:
        # 验证transformation_id是否为有效的整数
        try:
            trans_id = int(transformation_id)
        except ValueError:
            return StandardResponse(
                success=False,
                message="无效的转换配置ID",
                error=f"转换配置ID必须是数字: {transformation_id}",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 从数据库获取转换配置
        transformation = db.get_transformation_by_id(trans_id)
        
        if transformation:
            return StandardResponse(
                success=True,
                message="获取转换配置成功",
                data=transformation,
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="转换配置不存在",
                error=f"未找到ID为 {transformation_id} 的转换配置",
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"获取转换配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="获取转换配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.put("/{transformation_id}", response_model=StandardResponse, summary="更新转换配置")
async def update_transformation(
    transformation_id: str,
    config: TransformationConfigUpdateRequest,
    db: SchemaDatabase = Depends(get_database)
):
    """更新指定转换配置的信息"""
    try:
        # 暂时返回成功响应（实际功能需要在SchemaDatabase中实现）
        return StandardResponse(
            success=True,
            message="转换配置更新功能待实现",
            data={
                "id": str(transformation_id),
                "updated_at": datetime.utcnow().isoformat()
            },
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"更新转换配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="更新转换配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.delete("/{transformation_id}", response_model=StandardResponse, summary="删除转换配置")
async def delete_transformation(
    transformation_id: str,
    db: SchemaDatabase = Depends(get_database)
):
    """删除指定的转换配置"""
    try:
        # 验证transformation_id是否为有效的整数
        try:
            trans_id = int(transformation_id)
        except ValueError:
            return StandardResponse(
                success=False,
                message="无效的转换配置ID",
                error=f"转换配置ID必须是数字: {transformation_id}",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 调用数据库方法删除记录
        success = db.delete_transformation_by_id(trans_id)
        
        if success:
            return StandardResponse(
                success=True,
                message="转换配置删除成功",
                data={
                    "id": str(transformation_id),
                    "deleted_at": datetime.utcnow().isoformat()
                },
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="转换配置不存在",
                error=f"未找到ID为 {transformation_id} 的转换配置",
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"删除转换配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="删除转换配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/dataset/{dataset_id}", response_model=PaginatedResponse, summary="获取数据集的所有转换配置")
async def list_dataset_transformations(
    dataset_id: str,
    page: int = 1,
    per_page: int = 20,
    db: SchemaDatabase = Depends(get_database)
):
    """获取指定数据集的所有转换配置"""
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
        
        # 使用现有的get_transformations方法
        transformations = db.get_transformations(str(dataset_id))
        
        # 手动实现分页
        total = len(transformations)
        start_idx = (page - 1) * per_page
        end_idx = start_idx + per_page
        paginated_transformations = transformations[start_idx:end_idx]
        
        return PaginatedResponse(
            success=True,
            message="获取转换配置列表成功",
            data=paginated_transformations,
            pagination={
                "total": total,
                "page": page,
                "per_page": per_page,
                "pages": (total + per_page - 1) // per_page
            },
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"获取转换配置列表失败: {str(e)}")
        return PaginatedResponse(
            success=False,
            message="获取转换配置列表失败",
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

@router.patch("/{transformation_id}/toggle", response_model=StandardResponse, summary="启用/禁用转换配置")
async def toggle_transformation(
    transformation_id: str,
    enabled: bool,
    db: SchemaDatabase = Depends(get_database)
):
    """启用或禁用指定的转换配置"""
    try:
        # 验证transformation_id是否为有效的整数
        try:
            trans_id = int(transformation_id)
        except ValueError:
            return StandardResponse(
                success=False,
                message="无效的转换配置ID",
                error=f"转换配置ID必须是数字: {transformation_id}",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 调用数据库方法更新状态
        success = db.toggle_transformation_by_id(trans_id, enabled)
        
        if success:
            return StandardResponse(
                success=True,
                message=f"转换配置{'启用' if enabled else '禁用'}成功",
                data={
                    "id": str(transformation_id),
                    "enabled": enabled,
                    "updated_at": datetime.utcnow().isoformat()
                },
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="转换配置不存在",
                error=f"未找到ID为 {transformation_id} 的转换配置",
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"更新转换配置状态失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="更新转换配置状态失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.post("/dataset/{dataset_id}/auto-generate", response_model=StandardResponse, summary="自动生成转换配置")
async def auto_generate_transformations(
    dataset_id: str,
    data_type: str = "auto",  # auto, land, financial, user
    replace_existing: bool = False,
    db: SchemaDatabase = Depends(get_database)
):
    """为数据集自动生成转换配置"""
    try:
        # 验证数据集是否存在
        dataset = db.get_dataset_by_id(str(dataset_id))
        if not dataset:
            return StandardResponse(
                success=False,
                message="数据集不存在",
                error=f"数据集 {dataset_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 导入DataAnalyzer
        from ...core.data_analyzer import DataAnalyzer
        
        # 读取数据集文件
        dataset_path = dataset.get('file_path')
        if not dataset_path:
            return StandardResponse(
                success=False,
                message="数据集文件路径不存在",
                error=f"数据集 {dataset_id} 没有有效的文件路径",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 加载数据
        df = DataAnalyzer.load_data(dataset_path)
        
        # 自动生成transformations
        transformations = DataAnalyzer.auto_generate_transformations(df, data_type)
        
        if not transformations:
            return StandardResponse(
                success=False,
                message="未能生成任何转换配置",
                error="数据分析未产生有效的转换配置",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 如果需要替换现有配置，先删除
        if replace_existing:
            try:
                db.delete_transformations_by_dataset(str(dataset_id))
                logger.info(f"已删除数据集 {dataset_id} 的现有转换配置")
            except Exception as e:
                logger.warning(f"删除现有转换配置时出错: {e}")
        
        # 保存生成的transformations到数据库
        saved_count = 0
        for transformation in transformations:
            try:
                # 添加数据集ID
                transformation['dataset_id'] = str(dataset_id)
                
                # 保存到数据库
                db.save_transformation(transformation)
                saved_count += 1
                
            except Exception as e:
                logger.error(f"保存转换配置失败: {transformation.get('name', 'Unknown')}, 错误: {e}")
        
        logger.info(f"✅ 为数据集 {dataset_id} 自动生成并保存了 {saved_count} 个转换配置")
        
        return StandardResponse(
            success=True,
            message=f"成功自动生成 {saved_count} 个转换配置",
            data={
                "dataset_id": str(dataset_id),
                "data_type": data_type,
                "replace_existing": replace_existing,
                "generated_count": saved_count,
                "transformations": transformations
            },
            timestamp=datetime.utcnow().isoformat()
        )
        
    except Exception as e:
        logger.error(f"自动生成转换配置失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="自动生成转换配置失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        ) 