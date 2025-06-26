"""
查询API路由 - 简化版

只需要dataset_id即可查询，系统自动管理文件路径和配置
"""

from fastapi import APIRouter, Depends, HTTPException, status
from typing import Optional
import time
import uuid
import traceback
import os
import base64
from datetime import datetime

from ..models import QueryRequest, QueryResponse, StandardResponse
from ..dependencies import get_database, get_settings, get_llm
from ...core.query_engine import QueryEngine
from ...utils.schema_database import SchemaDatabase
from ...config.settings import Settings
from ...core.llm_adapter import CustomOpenAI
from ...utils.logger import get_logger, LogContext

# 导入字体配置，解决matplotlib中文字体问题
try:
    from ...utils.font_config import auto_configure
    auto_configure()  # 自动配置字体
except ImportError as e:
    get_logger(__name__).warning(f"字体配置模块导入失败: {e}")

router = APIRouter(prefix="/query", tags=["数据查询"])
logger = get_logger(__name__)

def check_and_encode_chart(query_id):
    """
    检查是否生成了图表文件，并返回 base64 编码
    
    Args:
        query_id: 查询ID
        
    Returns:
        tuple: (have_chart: bool, chart_base64: str or None)
    """
    chart_path = os.path.join("exports", "charts", f"{query_id}.png")
    
    if os.path.exists(chart_path):
        try:
            with open(chart_path, "rb") as image_file:
                image_data = image_file.read()
                chart_base64 = base64.b64encode(image_data).decode('utf-8')
                return True, chart_base64
        except Exception as e:
            logger.warning(f"读取图表文件失败: {str(e)}")
            return True, None  # 文件存在但读取失败
    
    return False, None

@router.post("/", response_model=QueryResponse, summary="执行数据查询")
async def execute_query(
    query_request: QueryRequest,
    db: SchemaDatabase = Depends(get_database),
    settings: Settings = Depends(get_settings),
    llm: CustomOpenAI = Depends(get_llm)
):
    """
    执行自然语言数据查询
    
    只需要提供dataset_id和问题，系统自动：
    1. 根据dataset_id查找数据文件路径
    2. 加载对应的schema配置
    3. 执行查询并返回结果
    """
    start_time = time.time()
    # 使用传入的 query_id，如果没有提供则生成一个
    query_id = query_request.query_id or str(uuid.uuid4())
    
    logger.info(f"🔍 开始执行查询 - ID: {query_id}")
    logger.info(f"📋 数据集ID: {query_request.dataset_id}")
    logger.info(f"❓ 查询问题: {query_request.question}")
    
    try:
        # 1. 根据dataset_id获取数据集信息
        with LogContext(logger, f"获取数据集信息: {query_request.dataset_id}"):
            dataset_info = db.get_dataset_by_id(query_request.dataset_id)
            
            if not dataset_info:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail=f"数据集不存在: {query_request.dataset_id}"
                )
        
        # 2. 验证数据文件是否存在
        # 优先使用actual_data_path，如果不存在则使用file_path（兼容旧数据）
        actual_data_path = dataset_info.get('actual_data_path')
        file_path = actual_data_path if actual_data_path else dataset_info['file_path']
        
        if not os.path.exists(file_path):
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"数据文件不存在: {file_path}"
            )
        
        # 记录文件转换信息
        if dataset_info.get('is_converted'):
            logger.info(f"📄 检测到文件转换: {dataset_info.get('original_file_type')} -> {dataset_info.get('data_file_type')}")
            logger.info(f"🗂️ 原始文件: {dataset_info.get('file_path')}")
            logger.info(f"📊 数据文件: {file_path}")
        else:
            logger.info(f"📁 数据文件: {file_path}")
        
        # 3. 加载schema配置
        with LogContext(logger, "加载schema配置"):
            schema_config = db.get_dataset_schema(query_request.dataset_id)
            if not schema_config:
                logger.warning("未找到schema配置，将使用自动生成模式")
        
        # 4. 初始化查询引擎
        with LogContext(logger, "初始化查询引擎"):
            query_engine = QueryEngine(
                llm=llm,
                data_path=file_path,
                use_semantic_layer=True if schema_config else False,
                settings=settings,
                dataset_id=query_request.dataset_id,
                schema_config=schema_config,
                db=db  # 传递数据库连接，避免重复创建
            )
        
        # 5. 执行查询
        with LogContext(logger, f"执行查询: {query_request.question}"):
            result = query_engine.query(query_request.question, query_id=query_id)
        
        execution_time = time.time() - start_time
        
        logger.info(f"✅ 查询执行成功 - ID: {query_id}, 耗时: {execution_time:.2f}秒")
        
        # 6. 确定结果类型
        result_type = "string"
        if isinstance(result, (int, float)):
            result_type = "number"
        elif hasattr(result, 'to_dict') and not isinstance(result, str):  # DataFrame
            result_type = "dataframe"
            try:
                import pandas as pd
                if isinstance(result, pd.DataFrame):
                    result = result.to_dict('records')
                else:
                    result = str(result)
            except Exception:
                result = str(result)
        elif isinstance(result, str) and any(keyword in result.lower() for keyword in ['图', 'chart', 'plot']):
            result_type = "chart"
        
        # 7. 检查是否生成了图表并获取 base64 编码
        have_chart, chart_base64 = check_and_encode_chart(query_id)
        
        # 8. 收集数据集元数据
        dataset_metadata = {
            "dataset_id": query_request.dataset_id,
            "dataset_name": dataset_info.get('name'),
            "file_path": file_path,
            "row_count": query_engine.get_data_info().get('shape', [0, 0])[0],
            "column_count": query_engine.get_data_info().get('shape', [0, 0])[1]
        }
        
        # 9. 获取执行的SQL查询字符串
        executed_sqls_string = query_engine.get_executed_sqls_string()
        
        return QueryResponse(
            success=True,
            message="查询执行成功",
            data={
                "result": result,
                "result_type": result_type,
                "execution_time": execution_time,
                "query_id": query_id,
                "have_chart": have_chart,
                "chart_base64": chart_base64,
                "dataset_info": dataset_metadata,
                "sql_queries": executed_sqls_string
            },
            timestamp=datetime.utcnow().isoformat()
        )
        
    except HTTPException:
        raise
    except Exception as e:
        execution_time = time.time() - start_time
        error_detail = f"查询执行失败: {str(e)}"
        
        # 记录详细错误信息
        logger.error(f"❌ 查询执行失败 - ID: {query_id}")
        logger.error(f"数据集: {query_request.dataset_id}")
        logger.error(f"问题: {query_request.question}")
        logger.error(f"错误: {error_detail}")
        logger.error(f"详细信息: {traceback.format_exc()}")
        
        # 即使出错也检查是否有图表生成
        have_chart, chart_base64 = check_and_encode_chart(query_id)
        
        # 尝试获取SQL查询字符串（如果查询引擎已创建）
        executed_sqls_string = None
        try:
            if 'query_engine' in locals():
                executed_sqls_string = query_engine.get_executed_sqls_string()
        except:
            pass
        
        return QueryResponse(
            success=False,
            message="查询执行失败",
            data={
                "result": error_detail,
                "result_type": "error",
                "execution_time": execution_time,
                "query_id": query_id,
                "have_chart": have_chart,
                "chart_base64": chart_base64,
                "dataset_info": {"dataset_id": query_request.dataset_id},
                "sql_queries": executed_sqls_string
            },
            error=error_detail,
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/datasets", summary="获取可用数据集列表")
async def list_datasets(
    tree_node_id: Optional[str] = None,
    db: SchemaDatabase = Depends(get_database)
):
    """
    获取所有可用的数据集列表
    
    Args:
        tree_node_id: 树节点ID，如果指定则返回该节点及其子节点下的数据集
                     如果不传或传"0"，则返回所有数据集
    """
    try:
        with LogContext(logger, f"获取数据集列表 (tree_node_id: {tree_node_id})"):
            datasets = db.list_all_datasets(tree_node_id=tree_node_id)
        
        # 转换为API响应格式
        dataset_list = []
        for dataset in datasets:
            dataset_list.append({
                "id": dataset.get("id") or dataset.get("path", "").split("/")[-1],
                "name": dataset.get("name", ""),
                "description": dataset.get("description", ""),
                "file_path": dataset.get("file_path", ""),
                "tree_node_id": dataset.get("tree_node_id"),
                "status": dataset.get("status", "active"),
                "created_at": dataset.get("created_at", "").isoformat() if dataset.get("created_at") else None
            })
        
        return StandardResponse(
            success=True,
            message=f"成功获取{len(dataset_list)}个数据集 (tree_node_id: {tree_node_id or 'all'})",
            data={
                "datasets": dataset_list,
                "total": len(dataset_list)
            },
            timestamp=datetime.utcnow().isoformat()
        )
        
    except Exception as e:
        logger.error(f"获取数据集列表失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取数据集列表失败: {str(e)}"
        )

@router.get("/datasets/{dataset_id}", summary="获取数据集详细信息")
async def get_dataset_info(
    dataset_id: str,
    db: SchemaDatabase = Depends(get_database)
):
    """
    获取指定数据集的详细信息
    """
    try:
        with LogContext(logger, f"获取数据集信息: {dataset_id}"):
            dataset_info = db.get_dataset_by_id(dataset_id)
            
            if not dataset_info:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail=f"数据集不存在: {dataset_id}"
                )
            
            # 获取schema配置
            schema_config = db.get_dataset_schema(dataset_id)
            
            # 如果文件存在，获取数据统计信息
            data_stats = {}
            # 优先使用actual_data_path，如果不存在则使用file_path（兼容旧数据）
            actual_data_path = dataset_info.get('actual_data_path')
            analysis_file_path = actual_data_path if actual_data_path else dataset_info.get('file_path')
            
            if analysis_file_path and os.path.exists(analysis_file_path):
                try:
                    from ...core.data_analyzer import DataAnalyzer
                    df = DataAnalyzer.load_data(analysis_file_path)
                    data_info = DataAnalyzer.analyze_structure(df)
                    data_stats = {
                        "row_count": data_info['shape'][0],
                        "column_count": data_info['shape'][1],
                        "columns": data_info['columns'],
                        "date_columns": data_info.get('date_columns', [])
                    }
                    
                    # 添加文件转换信息
                    if dataset_info.get('is_converted'):
                        data_stats["conversion_info"] = {
                            "is_converted": True,
                            "original_type": dataset_info.get('original_file_type'),
                            "data_type": dataset_info.get('data_file_type'),
                            "original_file": dataset_info.get('file_path'),
                            "data_file": actual_data_path
                        }
                except Exception as e:
                    logger.error(f"分析数据文件失败: {str(e)}")
            
            # 构建响应数据
            response_data = {
                "id": dataset_info.get("id") or dataset_id,
                "name": dataset_info.get("name", ""),
                "description": dataset_info.get("description", ""),
                "file_path": dataset_info.get("file_path", ""),
                "status": dataset_info.get("status", "active"),
                "created_at": dataset_info.get("created_at", "").isoformat() if dataset_info.get("created_at") else None,
                "updated_at": dataset_info.get("updated_at", "").isoformat() if dataset_info.get("updated_at") else None,
                "has_schema": bool(schema_config),
                "data_stats": data_stats
            }
            
            return StandardResponse(
                success=True,
                message="成功获取数据集信息",
                data=response_data,
                timestamp=datetime.utcnow().isoformat()
            )
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"获取数据集信息失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取数据集信息失败: {str(e)}"
        )

@router.get("/history", summary="获取查询历史")
async def get_query_history(
    dataset_id: str = None,
    limit: int = 10,
    db: SchemaDatabase = Depends(get_database)
):
    """
    获取查询历史记录
    """
    try:
        # 暂时返回空列表（查询历史功能待实现）
        return StandardResponse(
            success=True,
            message="查询历史功能待实现",
            data={
                "history": [],
                "total": 0,
                "note": "查询历史功能将在后续版本中实现"
            },
            timestamp=datetime.utcnow().isoformat()
        )
        
    except Exception as e:
        logger.error(f"获取查询历史失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="获取查询历史失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        ) 