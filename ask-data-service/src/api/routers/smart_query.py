"""
智能查询API路由

基于意图识别的自动数据集匹配和查询
"""

from fastapi import APIRouter, Depends, HTTPException, status
from typing import List, Dict, Any, Optional
import time
import uuid
import os
import base64
from datetime import datetime

from ..models import StandardResponse
from ..dependencies import get_database, get_settings, get_llm
from ...config.settings import Settings
from ...utils.schema_database import SchemaDatabase
from ...core.llm_adapter import CustomOpenAI
from ...core.embedding_service import EmbeddingService
from ...core.vector_search_service import VectorSearchService
from ...core.intent_engine import IntentRecognitionEngine
from ...core.query_engine import QueryEngine
from ...utils.logger import get_logger, LogContext
from pydantic import BaseModel, Field

# 数据模型定义
class SmartQueryRequest(BaseModel):
    """智能查询请求模型"""
    question: str = Field(..., description="用户的自然语言问题", example="江西省2023年的土地面积是多少？")
    max_suggestions: int = Field(3, description="最多返回几个数据集建议", ge=1, le=10)
    auto_execute: bool = Field(True, description="是否自动使用最佳匹配执行查询")
    min_score: float = Field(0.3, description="最小匹配分数", ge=0.0, le=1.0)
    query_id: Optional[str] = Field(None, description="查询ID，用于标识此次查询")

class DatasetMatchResult(BaseModel):
    """数据集匹配结果模型"""
    dataset_id: str = Field(..., description="数据集ID")
    dataset_name: str = Field(..., description="数据集名称")
    description: str = Field("", description="数据集描述")
    match_score: float = Field(..., description="匹配分数")
    match_reason: str = Field(..., description="匹配原因")
    domain: Optional[str] = Field(None, description="业务领域")
    keywords: List[str] = Field([], description="关键词")

class SmartQueryResponse(BaseModel):
    """智能查询响应模型"""
    success: bool = Field(..., description="操作是否成功")
    message: str = Field(..., description="响应消息")
    data: Dict[str, Any] = Field(..., description="查询结果数据")
    error: Optional[str] = Field(None, description="错误信息")
    timestamp: str = Field(..., description="响应时间戳")

router = APIRouter(prefix="/smart-query", tags=["智能查询"])
logger = get_logger(__name__)

# 全局变量用于缓存服务实例
_embedding_service = None
_vector_service = None
_intent_engine = None

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

def get_embedding_service(settings: Settings = Depends(get_settings)) -> EmbeddingService:
    """获取远程embedding服务实例"""
    global _embedding_service
    if _embedding_service is None:
        logger.info("初始化远程embedding服务")
        embedding_config = settings.get_embedding_config()
        _embedding_service = EmbeddingService(
            api_key=settings.api_key,  # 使用OpenAI API Key作为认证
            base_url=embedding_config["base_url"],
            model_name=embedding_config["model"]
        )
    return _embedding_service

def get_vector_service(settings: Settings = Depends(get_settings)) -> VectorSearchService:
    """获取ES向量检索服务实例"""
    global _vector_service
    if _vector_service is None:
        logger.info("初始化ES向量检索服务")
        # 从设置中获取ES配置
        es_config = settings.get_elasticsearch_config()
        _vector_service = VectorSearchService(es_config, settings.elasticsearch_index)
    return _vector_service

def get_intent_engine(
    embedding_service: EmbeddingService = Depends(get_embedding_service),
    vector_service: VectorSearchService = Depends(get_vector_service),
    db: SchemaDatabase = Depends(get_database)
) -> IntentRecognitionEngine:
    """获取意图识别引擎实例"""
    global _intent_engine
    if _intent_engine is None:
        logger.info("初始化意图识别引擎")
        _intent_engine = IntentRecognitionEngine(
            embedding_service=embedding_service,
            vector_service=vector_service,
            db=db
        )
    return _intent_engine

@router.post("/query", response_model=SmartQueryResponse, summary="智能数据查询")
async def smart_query(
    request: SmartQueryRequest,
    intent_engine: IntentRecognitionEngine = Depends(get_intent_engine),
    db: SchemaDatabase = Depends(get_database),
    settings: Settings = Depends(get_settings),
    llm: CustomOpenAI = Depends(get_llm)
):
    """
    智能数据查询：根据用户问题自动识别数据集并执行查询
    
    工作流程：
    1. 分析用户问题，识别意图
    2. 基于向量相似度匹配数据集
    3. 返回匹配结果或自动执行查询
    """
    start_time = time.time()
    
    # 使用传入的 query_id，如果没有提供则生成一个
    query_id = request.query_id or str(uuid.uuid4())
    
    logger.info(f"🤖 智能查询开始: {request.question}")
    logger.info(f"🔍 查询ID: {query_id}")
    
    try:
        with LogContext(logger, "智能查询处理"):
            # 1. 意图识别和数据集匹配
            matches = intent_engine.recognize_intent(
                question=request.question,
                max_results=request.max_suggestions,
                min_score=request.min_score
            )
            
            # 记录匹配详情
            logger.info(f"🎯 意图识别结果: 共找到{len(matches)}个符合条件的数据集 (min_score={request.min_score})")
            for i, match in enumerate(matches[:3]):  # 只显示前3个
                logger.info(f"  [{i+1}] {match['dataset_name']} - 分数: {match['enhanced_score']:.3f}")
            
            # 2. 转换匹配结果格式
            matched_datasets = []
            for match in matches:
                dataset_match = DatasetMatchResult(
                    dataset_id=match["dataset_id"],
                    dataset_name=match["dataset_name"],
                    description=match.get("description", ""),
                    match_score=match["enhanced_score"],
                    match_reason=match["match_reason"],
                    domain=match.get("domain"),
                    keywords=match.get("keywords", [])
                )
                matched_datasets.append(dataset_match)
            
            # 3. 准备基础响应数据
            response_data = {
                "result": None,
                "result_type": "string",
                "execution_time": 0.0,
                "query_id": query_id,
                "have_chart": False,
                "chart_base64": None,
                "dataset_info": None,
                "matched_datasets": [match.dict() for match in matched_datasets],
                "auto_executed": False,
                "sql_queries": None
            }
            
            # 4. 如果启用自动执行且有匹配结果
            if request.auto_execute and matched_datasets:
                best_match = matched_datasets[0]
                
                # 检查最佳匹配的分数是否足够高
                if best_match.match_score >= 0:
                    try:
                        logger.info(f"🎯 自动执行查询 - 数据集: {best_match.dataset_name} (ID: {best_match.dataset_id})")
                        
                        # 执行查询 - 复用query的查询逻辑
                        query_result = await _execute_query_with_dataset(
                            dataset_id=best_match.dataset_id,
                            question=request.question,
                            query_id=query_id,
                            db=db,
                            settings=settings,
                            llm=llm
                        )
                        
                        # 更新响应数据
                        response_data.update({
                            "result": query_result["result"],
                            "result_type": query_result["result_type"],
                            "have_chart": query_result["have_chart"],
                            "chart_base64": query_result["chart_base64"],
                            "dataset_info": query_result["dataset_info"],
                            "auto_executed": True,
                            "sql_queries": query_result.get("sql_queries")
                        })
                        
                        # 记录查询工作流到数据库
                        try:
                            workflow_recorded = db.record_query_workflow(
                                work_flow_run_id=query_id,
                                query_text=request.question,
                                dataset_id=best_match.dataset_id
                            )
                            if workflow_recorded:
                                logger.info(f"✅ 查询工作流记录成功: {query_id} -> 数据集 {best_match.dataset_id}")
                            else:
                                logger.warning(f"⚠️ 查询工作流记录失败: {query_id}")
                        except Exception as wf_e:
                            logger.error(f"❌ 记录查询工作流异常: {wf_e}")
                        
                        message = f"查询执行成功"
                        
                    except Exception as e:
                        logger.error(f"自动执行查询失败: {e}")
                        response_data["result"] = f"找到匹配数据集，但自动执行查询失败: {str(e)}"
                        message = "查询执行失败"
                
                else:
                    response_data["result"] = f"找到匹配数据集，但置信度较低({best_match.match_score:.2f})，建议手动确认"
                    message = "智能查询完成"
            
            elif not matched_datasets:
                response_data["result"] = "未找到匹配的数据集，请尝试调整问题或检查数据集是否已同步"
                message = "未找到匹配数据集"
            else:
                response_data["result"] = f"找到{len(matched_datasets)}个匹配的数据集"
                message = "智能查询完成"
            
            response_data["execution_time"] = time.time() - start_time
            
            logger.info(f"✅ 智能查询完成: 找到{len(matched_datasets)}个匹配, 自动执行: {response_data['auto_executed']}")
            
            return SmartQueryResponse(
                success=True,
                message=message,
                data=response_data,
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        execution_time = time.time() - start_time
        logger.error(f"❌ 智能查询失败: {e}")
        
        # 检查是否有图表生成
        have_chart, chart_base64 = check_and_encode_chart(query_id)
        
        return SmartQueryResponse(
            success=False,
            message="查询执行失败",
            data={
                "result": f"智能查询失败: {str(e)}",
                "result_type": "string",
                "execution_time": execution_time,
                "query_id": query_id,
                "have_chart": have_chart,
                "chart_base64": chart_base64,
                "dataset_info": None,
                "matched_datasets": [],
                "auto_executed": False,
                "sql_queries": None
            },
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

async def _execute_query_with_dataset(
    dataset_id: str,
    question: str,
    query_id: str,
    db: SchemaDatabase,
    settings: Settings,
    llm: CustomOpenAI
) -> Dict[str, Any]:
    """
    使用指定数据集执行查询 - 复用query接口的完整逻辑
    
    Args:
        dataset_id: 数据集ID
        question: 用户问题
        query_id: 查询ID
        db: 数据库服务
        settings: 配置
        llm: 语言模型
        
    Returns:
        Dict: 查询结果
    """
    # 1. 获取数据集信息
    dataset_info = db.get_dataset_by_id(dataset_id)
    if not dataset_info:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"数据集不存在: {dataset_id}"
        )
    
    # 2. 验证数据文件
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
    schema_config = db.get_dataset_schema(dataset_id)
    if not schema_config:
        logger.warning("未找到schema配置，将使用自动生成模式")
    
    # 4. 创建查询引擎并执行查询
    query_engine = QueryEngine(
        llm=llm,
        data_path=file_path,
        use_semantic_layer=True if schema_config else False,
        settings=settings,
        dataset_id=dataset_id,
        schema_config=schema_config,
        db=db
    )
    
    # 执行查询
    result = query_engine.query(question, query_id=query_id)
    
    # 确定结果类型
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
    
    # 检查是否生成了图表并获取 base64 编码
    have_chart, chart_base64 = check_and_encode_chart(query_id)
    
    return {
        "result": result,
        "result_type": result_type,
        "have_chart": have_chart,
        "sql_queries": query_engine.get_executed_sqls_string(),
        "chart_base64": chart_base64,
        "dataset_info": {
            "dataset_id": dataset_id,
            "dataset_name": dataset_info.get('name'),
            "file_path": file_path,
            "row_count": query_engine.get_data_info().get('shape', [0, 0])[0],
            "column_count": query_engine.get_data_info().get('shape', [0, 0])[1]
        },
    }

@router.post("/sync-datasets", response_model=StandardResponse, summary="同步数据集到向量库")
async def sync_datasets_to_vector_store(
    force_refresh: bool = False,
    intent_engine: IntentRecognitionEngine = Depends(get_intent_engine)
):
    """
    同步数据集到向量存储
    
    Args:
        force_refresh: 是否强制刷新所有数据
    """
    try:
        with LogContext(logger, "同步数据集到向量库"):
            sync_result = intent_engine.sync_datasets_to_vector_store(force_refresh)
            
            return StandardResponse(
                success=True,
                message=f"数据集同步完成: 成功{sync_result['success_count']}个, 失败{sync_result['failed_count']}个",
                data=sync_result,
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"数据集同步失败: {e}")
        return StandardResponse(
            success=False,
            message=f"数据集同步失败: {str(e)}",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/vector-stats", response_model=StandardResponse, summary="获取向量库统计信息")
async def get_vector_stats(
    vector_service: VectorSearchService = Depends(get_vector_service)
):
    """获取向量库统计信息"""
    try:
        stats = vector_service.get_stats()
        
        return StandardResponse(
            success=True,
            message="获取向量库统计信息成功",
            data=stats,
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"获取向量库统计信息失败: {e}")
        return StandardResponse(
            success=False,
            message=f"获取向量库统计信息失败: {str(e)}",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/embedding-info", response_model=StandardResponse, summary="获取embedding模型信息")
async def get_embedding_info(
    embedding_service: EmbeddingService = Depends(get_embedding_service)
):
    """获取embedding模型信息"""
    try:
        model_info = embedding_service.get_model_info()
        
        return StandardResponse(
            success=True,
            message="获取embedding模型信息成功",
            data=model_info,
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"获取embedding模型信息失败: {e}")
        return StandardResponse(
            success=False,
            message=f"获取embedding模型信息失败: {str(e)}",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )



 