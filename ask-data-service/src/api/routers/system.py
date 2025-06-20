"""
系统管理API路由
"""

from fastapi import APIRouter, Depends, HTTPException, status
from datetime import datetime
import psutil
import os

from ..models import SystemStatus, DatabaseInfo, StandardResponse
from ..dependencies import get_database, get_settings
from ...utils.schema_database import SchemaDatabase
from ...config.settings import Settings
from ...utils.logger import get_logger, LogContext

router = APIRouter(prefix="/system", tags=["系统管理"])
logger = get_logger(__name__)

@router.get("/status", response_model=StandardResponse, summary="获取系统状态")
async def get_system_status(
    settings: Settings = Depends(get_settings)
):
    """
    获取系统运行状态信息
    """
    try:
        # 获取系统运行时间
        process = psutil.Process(os.getpid())
        uptime = datetime.fromtimestamp(process.create_time()).strftime("%Y-%m-%d %H:%M:%S")
        
        # 获取内存使用情况
        memory = psutil.virtual_memory()
        memory_info = {
            "total": f"{memory.total / (1024*1024*1024):.2f}GB",
            "used": f"{memory.used / (1024*1024*1024):.2f}GB",
            "free": f"{memory.free / (1024*1024*1024):.2f}GB",
            "percent": f"{memory.percent}%"
        }
        
        # 获取CPU使用情况
        cpu_info = {
            "percent": f"{psutil.cpu_percent()}%",
            "count": psutil.cpu_count()
        }
        
        # 获取磁盘使用情况
        disk = psutil.disk_usage('/')
        disk_info = {
            "total": f"{disk.total / (1024*1024*1024):.2f}GB",
            "used": f"{disk.used / (1024*1024*1024):.2f}GB",
            "free": f"{disk.free / (1024*1024*1024):.2f}GB",
            "percent": f"{disk.percent}%"
        }
        
        status_info = SystemStatus(
            status="running",
            uptime=uptime,
            version=settings.version,
            environment=settings.environment
        )
        
        return StandardResponse(
            success=True,
            message="成功获取系统状态",
            data={
                "status": status_info.dict(),
                "system": {
                    "memory": memory_info,
                    "cpu": cpu_info,
                    "disk": disk_info
                }
            },
            timestamp=datetime.utcnow().isoformat()
        )
        
    except Exception as e:
        logger.error(f"获取系统状态失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取系统状态失败: {str(e)}"
        )

@router.get("/config", response_model=StandardResponse, summary="获取系统配置")
async def get_system_config(
    settings: Settings = Depends(get_settings)
):
    """
    获取系统配置信息
    """
    try:
        # 构建配置信息
        config_info = {
            "version": settings.version,
            "environment": settings.environment,
            "debug": settings.debug,
            "log_level": settings.log_level,
            "api_prefix": settings.api_prefix,
            "allowed_hosts": settings.allowed_hosts,
            "database": {
                "type": settings.database_type,
                "path": settings.database_path
            },
            "llm": {
                "provider": settings.llm_provider,
                "model": settings.llm_model,
                "temperature": settings.llm_temperature,
                "max_tokens": settings.llm_max_tokens
            },
            "data": {
                "upload_dir": settings.upload_dir,
                "max_file_size": settings.max_file_size,
                "allowed_extensions": settings.allowed_extensions
            }
        }
        
        return StandardResponse(
            success=True,
            message="成功获取系统配置",
            data=config_info,
            timestamp=datetime.utcnow().isoformat()
        )
        
    except Exception as e:
        logger.error(f"获取系统配置失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取系统配置失败: {str(e)}"
        )

@router.get("/database", response_model=StandardResponse, summary="获取数据库信息")
async def get_database_info(
    db: SchemaDatabase = Depends(get_database)
):
    """
    获取数据库连接和状态信息
    """
    try:
        with LogContext(logger, "获取数据库信息"):
            # 获取数据库状态
            try:
                db.test_connection()
                db_status = True
            except Exception:
                db_status = False
            
            # 获取数据集统计信息
            datasets = db.list_datasets()
            dataset_count = len(datasets)
            
            # 获取schema配置统计信息
            schema_count = sum(1 for d in datasets if db.get_dataset_schema(d.get('id')))
            
            db_info = DatabaseInfo(
                status="connected" if db_status else "disconnected",
                connection="ok" if db_status else "error",
                datasets_count=dataset_count
            )
            
            return StandardResponse(
                success=True,
                message="成功获取数据库信息",
                data={
                    "status": db_info.dict(),
                    "statistics": {
                        "total_datasets": dataset_count,
                        "datasets_with_schema": schema_count,
                        "datasets_without_schema": dataset_count - schema_count
                    }
                },
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"获取数据库信息失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取数据库信息失败: {str(e)}"
        )

@router.get("/health", summary="健康检查")
async def health_check():
    """
    简单的健康检查端点
    """
    return {"status": "ok", "message": "服务正常运行"} 