"""
数据集管理API路由

提供数据集的上传、配置和查询功能
"""

from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form
from typing import List, Optional, Dict, Any
import os
import uuid
import time
import json
import aiofiles
from pathlib import Path
from datetime import datetime

from ..models import (
    DatasetCreateRequest, DatasetUpdateRequest, DatasetInfo,
    StandardResponse, PaginatedResponse
)
from ..dependencies import get_database, get_settings
from ...utils.schema_database import SchemaDatabase
from ...config.settings import Settings
from ...core.data_analyzer import DataAnalyzer
from ...core.embedding_service import EmbeddingService
from ...core.vector_search_service import VectorSearchService
from ...core.intent_engine import IntentRecognitionEngine
from ...utils.logger import get_logger, LogContext
from ...utils.geo_converter import GeoConverter  # 导入地理数据转换器

router = APIRouter(prefix="/datasets", tags=["数据集管理"])
logger = get_logger(__name__)

# 全局变量用于缓存服务实例
_embedding_service = None
_vector_service = None
_intent_engine = None

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

async def sync_dataset_to_es(dataset_id: str, db: SchemaDatabase, 
                           intent_engine: IntentRecognitionEngine,
                           operation: str = "index") -> bool:
    """
    同步单个数据集到ES
    
    Args:
        dataset_id: 数据集ID
        db: 数据库服务
        intent_engine: 意图识别引擎
        operation: 操作类型 ("index", "update", "delete")
        
    Returns:
        bool: 是否成功
    """
    try:
        if operation == "delete":
            # 删除ES中的数据集索引
            success = intent_engine.vector_service.delete_dataset(dataset_id)
            if success:
                logger.info(f"✅ 数据集 {dataset_id} 已从ES中删除")
            else:
                logger.warning(f"⚠️ 数据集 {dataset_id} ES删除失败")
            return success
        
        # 获取数据集信息
        dataset = db.get_dataset_by_id(dataset_id)
        if not dataset:
            logger.error(f"数据集 {dataset_id} 不存在，无法同步到ES")
            return False
        
        # 获取列信息构建完整的数据集信息
        columns = db.list_dataset_columns(dataset_id)
        columns_info = ", ".join([f"{col['name']}({col['type']})" for col in columns])
        
        # 构建完整的数据集信息
        dataset_info = {
            "name": dataset.get('name', ''),
            "description": dataset.get('description', ''),
            "keywords": intent_engine._generate_keywords_from_dataset(dataset, columns),
            "domain": intent_engine._infer_domain_from_dataset(dataset, columns),
            "data_summary": intent_engine._generate_data_summary(dataset, columns),
            "columns_info": columns_info,
            "tree_node_id": dataset.get('tree_node_id', ''),
            "file_path": dataset.get('actual_data_path') or dataset.get('file_path', ''),
            "status": dataset.get('status', 'active'),
            "created_at": dataset.get('created_at'),
            "updated_at": dataset.get('updated_at')
        }
        
        # 生成向量
        embedding = intent_engine.embedding_service.generate_dataset_embedding(dataset_info)
        
        # 根据操作类型执行相应的ES操作
        if operation == "index":
            success = intent_engine.vector_service.index_dataset(dataset_id, dataset_info, embedding)
        elif operation == "update":
            success = intent_engine.vector_service.update_dataset(dataset_id, dataset_info, embedding)
        else:
            success = intent_engine.vector_service.index_dataset(dataset_id, dataset_info, embedding)
        
        if success:
            logger.info(f"✅ 数据集 {dataset_id} ({dataset.get('name')}) 已同步到ES ({operation})")
        else:
            logger.error(f"❌ 数据集 {dataset_id} 同步到ES失败 ({operation})")
        
        return success
        
    except Exception as e:
        logger.error(f"❌ 数据集 {dataset_id} 同步到ES异常: {e}")
        return False

def sanitize_filename(filename: str) -> str:
    """
    清理文件名，移除危险字符，保持原始名称的可读性
    
    Args:
        filename: 原始文件名
        
    Returns:
        str: 清理后的安全文件名
    """
    import re
    
    # 移除路径分隔符和其他危险字符
    dangerous_chars = r'[<>:"/\\|?*\x00-\x1f]'
    safe_filename = re.sub(dangerous_chars, '_', filename)
    
    # 移除多余的空格和点
    safe_filename = re.sub(r'\.+$', '', safe_filename.strip())
    
    # 确保文件名不为空
    if not safe_filename:
        safe_filename = "unnamed_file"
    
    # 限制文件名长度（Windows限制为255字符）
    if len(safe_filename) > 200:
        name_part = safe_filename[:190]
        ext_part = safe_filename[190:]
        safe_filename = name_part + ext_part
    
    return safe_filename

@router.post("/upload", summary="上传数据文件")
async def upload_dataset(
    file: UploadFile = File(..., description="数据文件(CSV/Excel)"),
    name: str = Form(..., description="数据集名称"),
    description: Optional[str] = Form(None, description="数据集描述"),
    tree_node_id: Optional[str] = Form(None, description="所属节点ID"),
    auto_analyze: bool = Form(True, description="是否自动分析生成语义配置"),
    db: SchemaDatabase = Depends(get_database),
    intent_engine: IntentRecognitionEngine = Depends(get_intent_engine)
):
    """
    上传数据文件并创建数据集
    
    流程：
    1. 验证文件格式
    2. 验证树节点（如果指定）
    3. 保存文件
    4. 如果auto_analyze为True，自动分析数据结构并生成语义配置
    5. 创建数据集记录
    """
    
    try:
        with LogContext(logger, f"上传数据文件: {file.filename}"):
            # 1. 验证文件格式
            allowed_extensions = ['.csv', '.shp', '.zip']
            file_ext = Path(file.filename).suffix.lower()
            
            if file_ext not in allowed_extensions:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"不支持的文件格式: {file_ext}。支持格式: {', '.join(allowed_extensions)}\n\n💡 提示：\n- CSV文件：直接上传\n- SHP文件：需要上传包含.shp、.shx、.dbf等完整文件的ZIP压缩包\n- 单独的.shp文件无法使用，请打包后上传"
                )
            
            # 2. 验证树节点（如果指定）
            if tree_node_id and tree_node_id != "0":
                existing_node = db.get_tree_node(tree_node_id)
                if not existing_node:
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail=f"指定的树节点不存在: {tree_node_id}"
                    )
                logger.info(f"数据集将归属到节点: {existing_node['name']} (ID: {tree_node_id})")
            
            # 2. 生成临时目录（先创建数据集记录获取自增ID）
            temp_id = uuid.uuid4()  # 临时用于目录创建
            upload_dir = Path("datasets") / str(temp_id)
            upload_dir.mkdir(parents=True, exist_ok=True)
            
            # 3. 保存文件（保留原始文件名）
            timestamp = int(time.time())
            safe_filename = sanitize_filename(file.filename)
            
            # 检查文件是否已存在，如果存在则添加时间戳避免冲突
            file_path = upload_dir / safe_filename
            if file_path.exists():
                name_part = Path(safe_filename).stem
                ext_part = Path(safe_filename).suffix
                safe_filename = f"{name_part}_{timestamp}{ext_part}"
                file_path = upload_dir / safe_filename
            
            logger.info(f"保存文件到: {file_path}")
            
            async with aiofiles.open(file_path, 'wb') as f:
                content = await file.read()
                await f.write(content)
            
            # 3.5. 处理ZIP文件（可能包含SHP文件）
            if file_ext == '.zip':
                logger.info("检测到ZIP文件，开始解压")
                try:
                    import zipfile
                    
                    # 解压ZIP文件，处理中文编码问题
                    with zipfile.ZipFile(file_path, 'r') as zip_ref:
                        # 显示ZIP文件内容用于调试
                        file_list = zip_ref.namelist()
                        logger.info(f"ZIP文件包含 {len(file_list)} 个文件:")
                        for file_name in file_list[:10]:  # 只显示前10个文件
                            logger.info(f"  - {file_name}")
                        if len(file_list) > 10:
                            logger.info(f"  ... 还有 {len(file_list) - 10} 个文件")
                        
                        # 处理中文编码问题
                        for member in zip_ref.infolist():
                            # 尝试用GBK编码解码文件名（处理中文乱码）
                            try:
                                # 先尝试UTF-8解码
                                member.filename = member.filename.encode('cp437').decode('utf-8')
                            except (UnicodeDecodeError, UnicodeEncodeError):
                                try:
                                    # 如果UTF-8失败，尝试GBK解码
                                    member.filename = member.filename.encode('cp437').decode('gbk')
                                    logger.debug(f"使用GBK编码解码文件名: {member.filename}")
                                except (UnicodeDecodeError, UnicodeEncodeError):
                                    # 如果都失败，保持原始文件名
                                    logger.warning(f"无法解码文件名，保持原始: {member.filename}")
                            
                            # 解压单个文件
                            zip_ref.extract(member, upload_dir)
                    
                    # 递归查找SHP文件（支持子目录）
                    shp_files = list(upload_dir.rglob("*.shp"))
                    logger.info(f"在解压目录中找到 {len(shp_files)} 个SHP文件")
                    
                    if not shp_files:
                        # 尝试查找不同大小写的扩展名
                        shp_files_upper = list(upload_dir.rglob("*.SHP"))
                        if shp_files_upper:
                            shp_files = shp_files_upper
                            logger.info(f"找到大写扩展名的SHP文件: {len(shp_files)} 个")
                        else:
                            # 列出所有文件用于调试
                            all_files = list(upload_dir.rglob("*"))
                            logger.error(f"未找到SHP文件，解压后的所有文件:")
                            for f in all_files:
                                if f.is_file():
                                    logger.error(f"  - {f.relative_to(upload_dir)} (扩展名: {f.suffix})")
                            
                            raise HTTPException(
                                status_code=status.HTTP_400_BAD_REQUEST,
                                detail="ZIP文件中未找到SHP文件\n\n💡 请确保：\n1. ZIP文件中包含.shp文件\n2. 文件扩展名正确(.shp或.SHP)\n3. 文件没有损坏"
                            )
                    
                    if len(shp_files) > 1:
                        logger.warning(f"ZIP文件中包含多个SHP文件: {[f.name for f in shp_files]}")
                        # 选择第一个SHP文件，但给出警告
                        shp_file_path = shp_files[0]
                        logger.info(f"选择第一个SHP文件: {shp_file_path.name}")
                    else:
                        shp_file_path = shp_files[0]
                    
                    logger.info(f"选中的SHP文件: {shp_file_path}")
                    
                    # 验证必要的附属文件（在同一目录中查找）
                    shp_dir = shp_file_path.parent
                    base_name = shp_file_path.stem
                    required_files = ['.shx', '.dbf']
                    missing_files = []
                    found_files = []
                    
                    for ext in required_files:
                        # 尝试不同大小写
                        required_file_lower = shp_dir / f"{base_name}{ext}"
                        required_file_upper = shp_dir / f"{base_name}{ext.upper()}"
                        
                        if required_file_lower.exists():
                            found_files.append(ext)
                        elif required_file_upper.exists():
                            found_files.append(ext.upper())
                        else:
                            missing_files.append(ext)
                    
                    logger.info(f"找到的附属文件: {found_files}")
                    
                    if missing_files:
                        # 列出SHP文件同目录的所有文件
                        same_dir_files = list(shp_dir.glob(f"{base_name}.*"))
                        logger.error(f"SHP文件 '{base_name}' 同目录下的所有文件:")
                        for f in same_dir_files:
                            logger.error(f"  - {f.name}")
                        
                        raise HTTPException(
                            status_code=status.HTTP_400_BAD_REQUEST,
                            detail=f"ZIP文件中缺少必要的Shapefile附属文件: {', '.join(missing_files)}\n\n💡 完整的Shapefile需要包含:\n- {base_name}.shp (主文件)\n- {base_name}.shx (索引文件)\n- {base_name}.dbf (属性文件)\n\n当前只找到: {', '.join(found_files)}"
                        )
                    
                    # 更新file_path为SHP文件路径，并标记为从ZIP解压
                    file_path = shp_file_path
                    file_ext = '.shp'  # 后续按SHP文件处理
                    logger.info(f"ZIP文件解压成功，将按SHP文件处理: {file_path}")
                    
                except zipfile.BadZipFile:
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail="无效的ZIP文件格式"
                    )
                except Exception as e:
                    logger.error(f"ZIP文件处理失败: {str(e)}")
                    raise HTTPException(
                        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                        detail=f"ZIP文件处理失败: {str(e)}"
                    )

            # 3.6. 处理SHP文件（如果需要）
            converted_csv_path = None
            actual_data_path = str(file_path)  # 用于后续分析的实际数据文件路径
            conversion_info = {}  # 初始化转换信息
            
            if file_ext == '.shp':
                logger.info("检测到SHP文件，开始转换为CSV")
                try:
                    # 验证SHP文件
                    if not GeoConverter.validate_shp_file(str(file_path)):
                        raise HTTPException(
                            status_code=status.HTTP_400_BAD_REQUEST,
                            detail="SHP文件无效或缺少必要的附属文件（.shx, .dbf）"
                        )
                    
                    # 获取SHP文件信息
                    shp_info = GeoConverter.get_shp_info(str(file_path))
                    logger.info(f"SHP文件信息: {shp_info}")
                    
                    # 转换为CSV（基于原始文件名）
                    csv_filename = f"{Path(safe_filename).stem}.csv"
                    converted_csv_path = upload_dir / csv_filename
                    
                    # 转换SHP到CSV
                    GeoConverter.shp_to_csv(str(file_path), str(converted_csv_path))
                    logger.info(f"SHP文件已转换为CSV: {converted_csv_path}")
                    
                    # 更新实际数据文件路径
                    actual_data_path = str(converted_csv_path)
                    
                    # 设置转换信息
                    conversion_info = {
                        "shp_info": shp_info,
                        "to_format": "csv",
                        "from_format": "shp",
                        "original_file": str(file_path),
                        "converted_file": str(converted_csv_path),
                        "conversion_time": datetime.utcnow().timestamp()
                    }
                    
                except Exception as e:
                    logger.error(f"SHP文件转换失败: {str(e)}")
                    raise HTTPException(
                        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                        detail=f"SHP文件转换失败: {str(e)}"
                    )
            
            # 4. 先获取数据集ID（使用临时数据创建）
            temp_dataset_data = {
                'name': name,
                'description': description,
                'file_path': 'temp',  # 临时路径，稍后更新
                'original_filename': file.filename,
                'original_file_type': file_ext.lstrip('.'),
                'actual_data_path': 'temp',  # 临时路径，稍后更新
                'data_file_type': 'csv',
                'is_converted': file_ext != '.csv',
                'conversion_info': json.dumps(conversion_info) if conversion_info else json.dumps({}),
                'tree_node_id': tree_node_id,  # 添加树节点ID
                'status': 'active',
                'created_at': datetime.utcnow().timestamp(),
                'updated_at': datetime.utcnow().timestamp()
            }
            
            # 使用现有的save_dataset方法获取自增ID
            dataset_id = db.save_dataset(temp_dataset_data)
            logger.info(f"获取到数据集ID: {dataset_id}")
            
            # 5. 重命名目录为实际ID，并更新所有路径
            final_upload_dir = Path("datasets") / str(dataset_id)
            final_file_path = None
            final_actual_data_path = None
            
            try:
                # 在Windows上，先确保没有文件句柄占用
                import shutil
                
                # 如果目标目录已存在，先删除
                if final_upload_dir.exists():
                    shutil.rmtree(final_upload_dir)
                
                # 使用shutil.move代替rename，更可靠
                shutil.move(str(upload_dir), str(final_upload_dir))
                logger.info(f"重命名目录成功: {upload_dir} -> {final_upload_dir}")
                
                # 计算最终路径（使用字符串替换代替relative_to，更稳定）
                old_dir_str = str(upload_dir).replace('\\', '/')
                new_dir_str = str(final_upload_dir).replace('\\', '/')
                
                final_file_path = str(file_path).replace('\\', '/').replace(old_dir_str, new_dir_str)
                final_actual_data_path = str(actual_data_path).replace('\\', '/').replace(old_dir_str, new_dir_str)
                
                logger.info(f"最终文件路径: {final_file_path}")
                logger.info(f"最终数据路径: {final_actual_data_path}")
                
            except Exception as e:
                logger.warning(f"重命名目录失败，保持原目录: {e}")
                # 如果重命名失败，使用原路径
                final_file_path = str(file_path)
                final_actual_data_path = str(actual_data_path)
            
            # 6. 更新数据库中的正确路径
            try:
                db.update_dataset(dataset_id, {
                    'file_path': final_file_path,
                    'actual_data_path': final_actual_data_path
                })
                logger.info(f"数据库路径更新成功")
                logger.info(f"更新的file_path: {final_file_path}")
                logger.info(f"更新的actual_data_path: {final_actual_data_path}")
            except Exception as update_e:
                logger.error(f"更新数据库路径失败: {update_e}")
                raise HTTPException(
                    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    detail=f"更新数据库路径失败: {update_e}"
                )
            
            # 7. 如果启用了自动分析，生成语义配置
            if auto_analyze:
                try:
                    # 读取数据用于分析（使用最终的数据文件路径）
                    df = DataAnalyzer.load_data(final_actual_data_path)
                    
                    # 创建基础语义配置
                    schema = DataAnalyzer.create_semantic_schema(
                        file_path=final_actual_data_path,
                        organization="ask-data-ai",
                        dataset_name=name.lower().replace(' ', '-')
                    )
                    
                    # 自动生成transformations配置
                    transformations = DataAnalyzer.auto_generate_transformations(df)
                    if transformations:
                        schema['transformations'] = transformations
                        logger.info(f"自动生成了 {len(transformations)} 个transformation配置")
                    
                    # 保存语义配置
                    db.save_schema(dataset_id, schema)
                    logger.info("自动生成语义配置成功")
                    
                except Exception as e:
                    logger.error(f"自动生成语义配置失败: {str(e)}")
                    # 不影响数据集创建，只记录错误
            
            # 8. 同步数据集到ES向量存储
            try:
                logger.info(f"🔄 开始同步数据集 {dataset_id} 到ES")
                sync_success = await sync_dataset_to_es(
                    dataset_id=str(dataset_id), 
                    db=db, 
                    intent_engine=intent_engine,
                    operation="index"
                )
                if sync_success:
                    logger.info(f"✅ 数据集 {dataset_id} ES同步成功")
                else:
                    logger.warning(f"⚠️ 数据集 {dataset_id} ES同步失败")
            except Exception as sync_e:
                logger.error(f"❌ 数据集 {dataset_id} ES同步异常: {sync_e}")
                # ES同步失败不影响数据集创建
            
            return StandardResponse(
                success=True,
                message="数据集创建成功",
                data={
                    'id': dataset_id,
                    'name': name,
                    'description': description,
                    'file_path': final_file_path,
                    'actual_data_path': final_actual_data_path,
                    'original_filename': file.filename,
                    'tree_node_id': tree_node_id,
                    'is_converted': file_ext != '.csv',
                    'status': 'active',
                    'created_at': datetime.utcnow().isoformat()
                },
                timestamp=datetime.utcnow().isoformat()
            )
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"数据集创建失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="数据集创建失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.get("/", response_model=PaginatedResponse, summary="获取数据集列表")
async def list_datasets(
    page: int = 1,
    per_page: int = 20,
    tree_node_id: Optional[str] = None,
    db: SchemaDatabase = Depends(get_database)
):
    """
    获取数据集列表
    
    Args:
        page: 页码，从1开始
        per_page: 每页数量
        tree_node_id: 树节点ID，如果指定则返回该节点及其子节点下的数据集
                     如果不传或传"0"，则返回所有数据集
    """
    try:
        # 调用带tree_node_id参数的list_datasets方法
        all_datasets = db.list_datasets(tree_node_id=tree_node_id)
        
        # 手动实现分页
        total = len(all_datasets)
        start_idx = (page - 1) * per_page
        end_idx = start_idx + per_page
        datasets = all_datasets[start_idx:end_idx]
        
        return PaginatedResponse(
            success=True,
            message=f"获取数据集列表成功 (tree_node_id: {tree_node_id or 'all'})",
            data=datasets,
            pagination={
                "total": total,
                "page": page,
                "per_page": per_page,
                "pages": (total + per_page - 1) // per_page
            },
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"获取数据集列表失败: {str(e)}")
        # 错误时也要返回PaginatedResponse格式
        return PaginatedResponse(
            success=False,
            message="获取数据集列表失败",
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

@router.get("/{dataset_id}", response_model=StandardResponse, summary="获取数据集详细信息")
async def get_dataset(
    dataset_id: int,
    db: SchemaDatabase = Depends(get_database)
):
    """获取数据集的详细信息"""
    try:
        dataset = db.get_dataset_by_id(dataset_id)
        if not dataset:
            return StandardResponse(
                success=False,
                message="数据集不存在",
                error=f"数据集 {dataset_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        return StandardResponse(
            success=True,
            message="获取数据集成功",
            data=dataset,
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"获取数据集失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="获取数据集失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.put("/{dataset_id}", response_model=StandardResponse, summary="更新数据集信息")
async def update_dataset(
    dataset_id: int,
    dataset: DatasetUpdateRequest,
    db: SchemaDatabase = Depends(get_database),
    intent_engine: IntentRecognitionEngine = Depends(get_intent_engine)
):
    """更新数据集的信息"""
    try:
        # 验证数据集是否存在
        existing_dataset = db.get_dataset_by_id(dataset_id)
        if not existing_dataset:
            return StandardResponse(
                success=False,
                message="数据集不存在",
                error=f"数据集 {dataset_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 构建更新数据
        update_data = {}
        if dataset.name is not None:
            update_data['name'] = dataset.name
        if dataset.description is not None:
            update_data['description'] = dataset.description
        if dataset.file_path is not None:
            update_data['file_path'] = dataset.file_path
        if dataset.status is not None:
            update_data['status'] = dataset.status
        
        # 使用现有的update_dataset方法
        success = db.update_dataset(dataset_id, update_data)
        if success:
            # 同步更新到ES
            try:
                logger.info(f"🔄 开始同步更新数据集 {dataset_id} 到ES")
                sync_success = await sync_dataset_to_es(
                    dataset_id=str(dataset_id), 
                    db=db, 
                    intent_engine=intent_engine,
                    operation="update"
                )
                if sync_success:
                    logger.info(f"✅ 数据集 {dataset_id} ES更新同步成功")
                else:
                    logger.warning(f"⚠️ 数据集 {dataset_id} ES更新同步失败")
            except Exception as sync_e:
                logger.error(f"❌ 数据集 {dataset_id} ES更新同步异常: {sync_e}")
                # ES同步失败不影响数据集更新
            
            # 获取更新后的数据集信息
            updated_dataset = db.get_dataset_by_id(dataset_id)
            return StandardResponse(
                success=True,
                message="数据集更新成功",
                data=updated_dataset,
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="数据集更新失败",
                error="更新操作失败",
                timestamp=datetime.utcnow().isoformat()
            )
    except Exception as e:
        logger.error(f"更新数据集失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="更新数据集失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.delete("/{dataset_id}", response_model=StandardResponse, summary="删除数据集")
async def delete_dataset(
    dataset_id: int,
    delete_file: bool = True,
    db: SchemaDatabase = Depends(get_database),
    intent_engine: IntentRecognitionEngine = Depends(get_intent_engine)
):
    """删除数据集"""
    try:
        # 验证数据集是否存在
        dataset = db.get_dataset_by_id(dataset_id)
        if not dataset:
            return StandardResponse(
                success=False,
                message="数据集不存在",
                error=f"数据集 {dataset_id} 不存在",
                timestamp=datetime.utcnow().isoformat()
            )
        
        # 使用现有的delete_dataset_by_id方法
        success = db.delete_dataset_by_id(dataset_id)
        if success:
            # 同步删除ES中的索引
            try:
                logger.info(f"🔄 开始从ES中删除数据集 {dataset_id}")
                sync_success = await sync_dataset_to_es(
                    dataset_id=str(dataset_id), 
                    db=db, 
                    intent_engine=intent_engine,
                    operation="delete"
                )
                if sync_success:
                    logger.info(f"✅ 数据集 {dataset_id} ES删除同步成功")
                else:
                    logger.warning(f"⚠️ 数据集 {dataset_id} ES删除同步失败")
            except Exception as sync_e:
                logger.error(f"❌ 数据集 {dataset_id} ES删除同步异常: {sync_e}")
                # ES同步失败不影响数据集删除
            
            return StandardResponse(
                success=True,
                message="数据集删除成功",
                timestamp=datetime.utcnow().isoformat()
            )
        else:
            return StandardResponse(
                success=False,
                message="数据集删除失败",
                error="删除操作失败",
                timestamp=datetime.utcnow().isoformat()
            )
    except Exception as e:
        logger.error(f"删除数据集失败: {str(e)}")
        return StandardResponse(
            success=False,
            message="删除数据集失败",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        )

@router.post("/sync-to-es", response_model=StandardResponse, summary="手动同步数据集到ES")
async def sync_datasets_to_es(
    force_refresh: bool = False,
    dataset_ids: Optional[List[int]] = None,
    db: SchemaDatabase = Depends(get_database),
    intent_engine: IntentRecognitionEngine = Depends(get_intent_engine)
):
    """
    手动同步数据集到ES向量存储
    
    Args:
        force_refresh: 是否强制刷新所有数据
        dataset_ids: 指定要同步的数据集ID列表，如果为空则同步所有数据集
    """
    try:
        with LogContext(logger, "手动同步数据集到ES"):
            # 如果指定了特定的数据集ID，则只同步这些数据集
            if dataset_ids:
                success_count = 0
                failed_count = 0
                errors = []
                
                for dataset_id in dataset_ids:
                    try:
                        sync_success = await sync_dataset_to_es(
                            dataset_id=str(dataset_id), 
                            db=db, 
                            intent_engine=intent_engine,
                            operation="index"
                        )
                        if sync_success:
                            success_count += 1
                        else:
                            failed_count += 1
                            errors.append(f"数据集 {dataset_id} 同步失败")
                    except Exception as e:
                        failed_count += 1
                        errors.append(f"数据集 {dataset_id} 同步异常: {str(e)}")
                
                sync_result = {
                    "total_count": len(dataset_ids),
                    "success_count": success_count,
                    "failed_count": failed_count,
                    "errors": errors
                }
            else:
                # 使用意图引擎的批量同步功能
                sync_result = intent_engine.sync_datasets_to_vector_store(force_refresh)
            
            return StandardResponse(
                success=True,
                message=f"数据集ES同步完成: 成功{sync_result['success_count']}个, 失败{sync_result['failed_count']}个",
                data=sync_result,
                timestamp=datetime.utcnow().isoformat()
            )
            
    except Exception as e:
        logger.error(f"数据集ES同步失败: {e}")
        return StandardResponse(
            success=False,
            message=f"数据集ES同步失败: {str(e)}",
            error=str(e),
            timestamp=datetime.utcnow().isoformat()
        ) 