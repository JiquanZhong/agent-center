"""
树节点管理API路由

提供数据中心树节点的管理功能
"""

from fastapi import APIRouter, Depends, HTTPException, status
from typing import List, Optional, Dict, Any
from datetime import datetime

from ..models import (
    TreeNodeCreateRequest, TreeNodeUpdateRequest, TreeNode,
    StandardResponse
)
from ..dependencies import get_database
from ...utils.schema_database import SchemaDatabase
from ...utils.logger import get_logger

router = APIRouter(prefix="/tree-nodes", tags=["树节点管理"])
logger = get_logger(__name__)

@router.post("", response_model=StandardResponse, summary="新增节点")
async def create_tree_node(
    node: TreeNodeCreateRequest,
    db: SchemaDatabase = Depends(get_database)
):
    """创建新的树节点"""
    try:
        # 验证父节点是否存在（如果指定了父节点）
        if node.pid and node.pid != "0":
            parent_node = db.get_tree_node(node.pid)
            if not parent_node:
                return StandardResponse(
                    success=False,
                    message="父节点不存在",
                    error=f"父节点 {node.pid} 不存在",
                    timestamp=datetime.utcnow().isoformat()
                )
        
        # 创建节点
        node_id = db.create_tree_node(
            name=node.name,
            description=node.description,
            pid=node.pid,
            sort_order=node.sort_order or 0
        )
        
        if node_id:
            # 获取创建的节点信息
            created_node = db.get_tree_node(node_id)
            return StandardResponse(
                success=True,
                message="节点创建成功",
                data=created_node,
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="节点创建失败",
                error="数据库操作失败",
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"创建树节点失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="创建节点失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/{node_id}", response_model=StandardResponse, summary="获取节点详情")
async def get_tree_node(
    node_id: str,
    db: SchemaDatabase = Depends(get_database)
):
    """获取指定节点的详细信息"""
    try:
        node = db.get_tree_node(node_id)
        if not node:
            return StandardResponse(
                success=False,
                message="节点不存在",
                error=f"节点 {node_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        return StandardResponse(
            success=True,
            message="获取节点成功",
            data=node,
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"获取树节点失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="获取节点失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.put("/{node_id}", response_model=StandardResponse, summary="编辑节点")
async def update_tree_node(
    node_id: str,
    node: TreeNodeUpdateRequest,
    db: SchemaDatabase = Depends(get_database)
):
    """更新指定节点的信息（只能编辑name、description、sort_order）"""
    try:
        # 验证节点是否存在
        existing_node = db.get_tree_node(node_id)
        if not existing_node:
            return StandardResponse(
                success=False,
                message="节点不存在",
                error=f"节点 {node_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 更新节点
        success = db.update_tree_node(
            node_id=node_id,
            name=node.name,
            description=node.description,
            sort_order=node.sort_order
        )
        
        if success:
            # 获取更新后的节点信息
            updated_node = db.get_tree_node(node_id)
            return StandardResponse(
                success=True,
                message="节点更新成功",
                data=updated_node,
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="节点更新失败",
                error="数据库操作失败",
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"更新树节点失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="更新节点失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.delete("/{node_id}", response_model=StandardResponse, summary="删除节点")
async def delete_tree_node(
    node_id: str,
    db: SchemaDatabase = Depends(get_database)
):
    """删除指定的节点（级联删除子节点）"""
    try:
        # 验证节点是否存在
        existing_node = db.get_tree_node(node_id)
        if not existing_node:
            return StandardResponse(
                success=False,
                message="节点不存在",
                error=f"节点 {node_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 删除节点（会级联删除子节点）
        success = db.delete_tree_node(node_id)
        if success:
            return StandardResponse(
                success=True,
                message="节点删除成功",
                data={
                    "deleted_node_id": node_id,
                    "deleted_at": datetime.utcnow().isoformat()
                },
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="节点删除失败",
                error="数据库操作失败",
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"删除树节点失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="删除节点失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("", response_model=StandardResponse, summary="查看所有节点")
async def get_all_tree_nodes(
    db: SchemaDatabase = Depends(get_database)
):
    """获取所有节点的层级结构"""
    try:
        # 确保表存在
        try:
            db._init_tree_node_table()
        except Exception as init_e:
            logger.warning(f"初始化树节点表时出现警告: {init_e}")
        
        # 获取树形结构
        tree_data = db.get_all_tree_nodes()
        
        return StandardResponse(
            success=True,
            message="获取节点树成功",
            data=tree_data,
            timestamp=datetime.utcnow().isoformat()
        )
        
    except Exception as e:
        logger.error(f"获取树节点失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="获取节点树失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.post("/init-table", response_model=StandardResponse, summary="初始化树节点表")
async def init_tree_node_table(
    db: SchemaDatabase = Depends(get_database)
):
    """手动初始化树节点表（仅用于调试和维护）"""
    try:
        db._init_tree_node_table()
        return StandardResponse(
            success=True,
            message="树节点表初始化成功",
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"初始化树节点表失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="初始化树节点表失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        ) 