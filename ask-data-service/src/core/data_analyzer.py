"""
数据分析器模块

提供数据结构分析和预处理功能，支持PandasAI v3语义层
"""

import pandas as pd
import pandasai as pai
import json
import os
import re
import time
from typing import Dict, List, Any, Optional
from ..utils.logger import get_logger, LogContext
from ..utils.schema_database import SchemaDatabase
from ..utils.transformations_helper import TransformationsHelper
from ..config.settings import Settings

# 导入字体配置，解决matplotlib中文字体问题
try:
    from ..utils.font_config import auto_configure
    auto_configure()  # 自动配置字体
except ImportError as e:
    get_logger(__name__).warning(f"字体配置模块导入失败: {e}")

class DataAnalyzer:
    """数据结构分析器"""
    
    @staticmethod
    def analyze_structure(df):
        """分析数据结构，为AI提供更好的上下文"""
        logger = get_logger(__name__)
        
        # 获取原始pandas DataFrame
        raw_df = df._df if hasattr(df, '_df') else df
        logger.debug(f"分析数据结构 - 形状: {raw_df.shape}")
        
        # 基本信息  
        info = {
            "shape": raw_df.shape,
            "columns": list(raw_df.columns),
            "dtypes": {col: str(dtype) for col, dtype in raw_df.dtypes.to_dict().items()}
        }
        
        # 分析日期列  
        date_columns = []
        for col in raw_df.columns:
            # 检查是否为datetime类型、明确的日期列名或特定的年份字段
            if (pd.api.types.is_datetime64_any_dtype(raw_df[col]) or 
                'date' in col.lower() or 
                col == 'SJNF'):  # 特别识别SJNF（数据年份）字段
                date_columns.append(col)
                # 获取日期范围
                info[f"{col}_range"] = f"{raw_df[col].min()} 到 {raw_df[col].max()}"
        
        # 分析分类列
        categorical_info = {}
        for col in raw_df.columns:
            if raw_df[col].dtype == 'object' and col not in date_columns:
                unique_vals = raw_df[col].unique()
                if len(unique_vals) <= 20:  # 如果唯一值不多，展示所有值
                    categorical_info[col] = list(unique_vals)
                else:
                    categorical_info[col] = f"共{len(unique_vals)}个不同值"
        
        info["date_columns"] = date_columns
        info["categorical_info"] = categorical_info
        
        logger.debug(f"分析完成 - 日期列: {len(date_columns)}, 分类列: {len(categorical_info)}")
        return info
    
    @staticmethod
    def load_data(file_path, **kwargs):
        """智能加载数据，自动处理常见格式问题"""
        logger = get_logger(__name__)
        logger.info(f"加载数据文件: {file_path}")
        
        try:
            # 尝试自动检测日期列并优化解析
            if file_path.endswith('.csv'):
                # 先读取几行来检测日期列
                sample_df = pd.read_csv(file_path, nrows=5)
                
                # 检测可能的日期列
                date_columns = []
                for col in sample_df.columns:
                    if 'date' in col.lower():
                        date_columns.append(col)
                
                # 重新读取，优化日期解析
                if date_columns:
                    logger.debug(f"检测到日期列: {date_columns}")
                    df = pd.read_csv(file_path, 
                                   parse_dates=date_columns, 
                                   date_format='%Y-%m-%d',
                                   **kwargs)
                else:
                    df = pd.read_csv(file_path, **kwargs)
                
                logger.info(f"数据加载成功 - 形状: {df.shape}")
                return df
            else:
                # 其他格式的文件
                if file_path.endswith('.xlsx'):
                    return pd.read_excel(file_path, **kwargs)
                elif file_path.endswith('.json'):
                    return pd.read_json(file_path, **kwargs)
                else:
                    raise ValueError(f"不支持的文件格式: {file_path}")
                    
        except Exception as e:
            raise ValueError(f"数据加载失败: {str(e)}")
    
    @staticmethod
    def create_semantic_schema(file_path: str, organization: str = "ask-data-ai", 
                              dataset_name: str = "default") -> Dict[str, Any]:
        """
        为数据文件创建语义层配置
        """
        # 标准化组织名称：转小写，替换特殊字符为连字符
        def standardize_name(name: str) -> str:
            # 移除中文字符和特殊字符，只保留字母数字
            name = re.sub(r'[^\w\s-]', '', name)
            # 替换空格和下划线为连字符
            name = re.sub(r'[\s_]+', '-', name)
            # 转小写
            name = name.lower()
            # 移除多余的连字符
            name = re.sub(r'-+', '-', name)
            # 移除首尾连字符
            name = name.strip('-')
            # 如果为空，使用默认值
            return name if name else 'default'
        
        # 标准化名称
        organization = standardize_name(organization)
        dataset_name = standardize_name(dataset_name)
        
        # 先分析数据结构
        df = pd.read_csv(file_path)
        data_info = DataAnalyzer.analyze_structure(df)
        
        # 检查是否为土地数据（包含关键字段）
        is_land_data = any(field in data_info['columns'] for field in ['ZLDWDM', 'DLBM', 'TBMJ', 'QSXZ'])
        
        if is_land_data:
            # 使用标准土地数据字段配置
            columns = DataAnalyzer._generate_standard_land_columns(data_info['columns'])
        else:
            # 使用原有的通用配置逻辑
            columns = []
            for col in data_info['columns']:
                dtype = data_info['dtypes'][col]
                
                # 映射pandas数据类型到语义层类型
                if pd.api.types.is_datetime64_any_dtype(dtype):
                    semantic_type = "datetime"
                    description = f"日期时间列 - {col}"
                elif pd.api.types.is_integer_dtype(dtype):
                    semantic_type = "integer"
                    description = f"整数列 - {col}，表示数量或计数"
                elif pd.api.types.is_float_dtype(dtype):
                    semantic_type = "float"
                    description = f"浮点数列 - {col}，表示金额或比例"
                elif pd.api.types.is_bool_dtype(dtype):
                    semantic_type = "boolean"
                    description = f"布尔列 - {col}，表示是/否状态"
                else:
                    semantic_type = "string"
                    if col in data_info['categorical_info']:
                        categories = data_info['categorical_info'][col]
                        if isinstance(categories, list):
                            description = f"分类列 - {col}，可能值: {', '.join(map(str, categories[:5]))}"
                        else:
                            description = f"分类列 - {col}，{categories}"
                    else:
                        description = f"文本列 - {col}"
                
                columns.append({
                    "name": col,
                    "type": semantic_type,
                    "description": description
                })
        
        # 构建语义层配置
        schema = {
            "path": f"{organization}/{dataset_name}",
            "description": f"来自{os.path.basename(file_path)}的数据集，包含{data_info['shape'][0]}行记录",
            "columns": columns
        }
        
        # 如果有日期列，添加分组配置
        if data_info['date_columns']:
            schema["group_by"] = data_info['date_columns']
        
        return schema

    @staticmethod
    def _generate_standard_land_columns(actual_columns: List[str]) -> List[Dict[str, Any]]:
        """
        生成标准土地数据字段配置
        
        Args:
            actual_columns: 实际数据中的字段列表
            
        Returns:
            标准化的字段配置列表
        """
        # 标准土地数据字段配置映射表
        standard_fields = {
            'BSM': {
                'type': 'string',
                'description': '标识码 - 图斑的唯一标识符'
            },
            'YSDM': {
                'type': 'string', 
                'description': '要素代码 - 地理要素分类代码'
            },
            'TBYBH': {
                'type': 'string',
                'description': '图斑预编号 - 图斑的预分配编号'
            },
            'TBBH': {
                'type': 'string',
                'description': '图斑编号 - 图斑的正式编号'
            },
            'DLBM': {
                'type': 'string',
                'description': '地类编码 - 土地利用类型编码'
            },
            'DLMC': {
                'type': 'string',
                'description': '地类名称 - 土地利用类型名称'
            },
            'QSXZ': {
                'type': 'integer',
                'description': '权属性质 - 土地权属性质代码'
            },
            'QSDWDM': {
                'type': 'integer',
                'description': '权属单位代码 - 土地权属单位编码'
            },
            'QSDWMC': {
                'type': 'string',
                'description': '权属单位名称 - 土地权属单位名称'
            },
            'ZLDWDM': {
                'type': 'integer',
                'description': '坐落单位代码 - 图斑所在行政区划代码'
            },
            'ZLDWMC': {
                'type': 'string',
                'description': '坐落单位名称 - 图斑所在行政区划名称'
            },
            'TBMJ': {
                'type': 'float',
                'description': '图斑面积 - 图斑的实际面积（平方米）'
            },
            'KCDLBM': {
                'type': 'string',
                'description': '扣除地类编码 - 需要扣除部分的地类编码'
            },
            'KCXS': {
                'type': 'float',
                'description': '扣除地类系数 - 扣除面积的计算系数'
            },
            'KCMJ': {
                'type': 'float',
                'description': '扣除地类面积 - 需要扣除的面积（平方米）'
            },
            'TBDLMJ': {
                'type': 'float',
                'description': '图斑地类面积 - 扣除后的实际地类面积（平方米）'
            },
            'GDLX': {
                'type': 'string',
                'description': '耕地类型 - 耕地的具体类型分类'
            },
            'GDPDJB': {
                'type': 'string',
                'description': '耕地坡度级别 - 耕地的坡度等级'
            },
            'XZDWKD': {
                'type': 'float',
                'description': '线状地物宽度 - 线性地物的宽度（米）'
            },
            'TBXHDM': {
                'type': 'string',
                'description': '图斑细化代码 - 图斑的细化分类代码'
            },
            'TBXHMC': {
                'type': 'string',
                'description': '图斑细化名称 - 图斑的细化分类名称'
            },
            'ZZSXDM': {
                'type': 'string',
                'description': '种植属性代码 - 耕地种植属性分类代码'
            },
            'ZZSXMC': {
                'type': 'string',
                'description': '种植属性名称 - 耕地种植属性分类名称'
            },
            'GDDB': {
                'type': 'integer',
                'description': '耕地等别 - 耕地质量等级'
            },
            'FRDBS': {
                'type': 'string',
                'description': '飞入地标识 - 是否为飞入地的标识'
            },
            'CZCSXM': {
                'type': 'string',
                'description': '城镇村属性码 - 城镇村用地属性代码'
            },
            'SJNF': {
                'type': 'integer',
                'description': '数据年份 - 数据调查的年份'
            },
            'MSSM': {
                'type': 'string',
                'description': '描述说明 - 图斑的详细描述信息'
            },
            'HDMC': {
                'type': 'string',
                'description': '海岛名称 - 海岛地区的名称'
            },
            'BZ': {
                'type': 'string',
                'description': '备注 - 其他备注信息'
            },
            # 几何属性字段
            'Shape_Leng': {
                'type': 'float',
                'description': '形状长度 - 图斑边界的周长（米）'
            },
            'Shape_Area': {
                'type': 'float',
                'description': '形状面积 - 图斑的几何面积（平方米）'
            },
            'centroid_x': {
                'type': 'float',
                'description': '质心X坐标 - 图斑几何中心的X坐标'
            },
            'centroid_y': {
                'type': 'float',
                'description': '质心Y坐标 - 图斑几何中心的Y坐标'
            },
            'area': {
                'type': 'float',
                'description': '面积 - 图斑计算面积（平方米）'
            },
            'perimeter': {
                'type': 'float',
                'description': '周长 - 图斑边界周长（米）'
            },
            'min_x': {
                'type': 'float',
                'description': '最小X坐标 - 图斑边界框的最小X坐标'
            },
            'min_y': {
                'type': 'float',
                'description': '最小Y坐标 - 图斑边界框的最小Y坐标'
            },
            'max_x': {
                'type': 'float',
                'description': '最大X坐标 - 图斑边界框的最大X坐标'
            },
            'max_y': {
                'type': 'float',
                'description': '最大Y坐标 - 图斑边界框的最大Y坐标'
            }
        }
        
        logger = get_logger(__name__)
        columns = []
        
        # 为实际存在的字段生成配置
        for col in actual_columns:
            if col in standard_fields:
                # 使用标准配置
                config = standard_fields[col].copy()
                config['name'] = col
                columns.append(config)
                logger.debug(f"✅ 使用标准配置: {col}")
            else:
                # 使用默认推断逻辑
                columns.append({
                    'name': col,
                    'type': 'string',  # 默认为字符串类型
                    'description': f'字段 {col} - 自动推断字段'
                })
                logger.debug(f"⚠️ 使用默认配置: {col}")
        
        logger.info(f"📊 生成了{len(columns)}个字段配置，其中{sum(1 for col in actual_columns if col in standard_fields)}个使用标准配置")
        
        return columns
    
    @staticmethod
    def clear_semantic_cache(dataset_path: str = None):
        """
        清理PandasAI语义层缓存
        """
        logger = get_logger(__name__)
        
        try:
            # 清理PandasAI缓存目录
            cache_dir = "datasets"
            if os.path.exists(cache_dir):
                import shutil
                if dataset_path:
                    # 清理特定数据集的缓存
                    # 处理嵌套路径，如 'sementic/886e481d-7c9e-4310-be6e-82125c7b9f13'
                    if '/' in dataset_path:
                        # 对于嵌套路径，直接使用原路径构建缓存目录
                        dataset_cache_dir = os.path.join(cache_dir, *dataset_path.split('/'))
                    else:
                        # 对于简单路径，使用下划线替换
                        dataset_cache_dir = os.path.join(cache_dir, dataset_path.replace("/", "_"))
                    
                    if os.path.exists(dataset_cache_dir):
                        shutil.rmtree(dataset_cache_dir)
                        logger.info(f"🧹 已清理数据集缓存: {dataset_path} -> {dataset_cache_dir}")
                    else:
                        logger.warning(f"⚠️ 缓存目录不存在: {dataset_cache_dir}")
                else:
                    # 清理所有缓存
                    shutil.rmtree(cache_dir)
                    logger.info("🧹 已清理所有PandasAI缓存")
            
            # 尝试从PandasAI内部清理数据集注册
            try:
                if hasattr(pai, '_datasets') and dataset_path and dataset_path in pai._datasets:
                    del pai._datasets[dataset_path]
                    logger.info(f"🗑️ 已从内存清理数据集: {dataset_path}")
            except:
                pass  # 忽略内部清理错误
                
        except Exception as e:
            logger.warning(f"⚠️ 缓存清理失败: {str(e)}")
    
    @staticmethod
    def create_semantic_dataframe_from_config(file_path: str, dataset_id: str, db: SchemaDatabase = None) -> Any:
        """
        从数据库中的配置创建语义层DataFrame
        
        Args:
            file_path: 数据文件路径
            dataset_id: 数据集ID，用于从数据库获取配置
            db: 可选的数据库连接实例，如果提供则复用，否则创建新连接
        """
        logger = get_logger(__name__)
        
        # 从数据库获取语义配置
        try:
            # 如果没有提供数据库连接，则创建新的
            if db is None:
                settings = Settings()
                db = SchemaDatabase(settings)
                logger.debug("创建了新的数据库连接")
            else:
                logger.debug("复用现有的数据库连接")
                
            schema = db.get_dataset_schema(dataset_id)
            
            if not schema:
                logger.warning(f"⚠️ 未找到数据集配置: {dataset_id}，使用默认配置")
                schema = {"columns": []}
            else:
                logger.info(f"✅ 从数据库获取到语义配置: {dataset_id}")
                logger.debug(f"📋 配置内容: {schema}")
                
        except Exception as e:
            logger.error(f"❌ 从数据库获取配置失败: {str(e)}，使用默认配置")
            schema = {"columns": []}
        
        # 读取原始数据
        raw_df = DataAnalyzer.load_data(file_path)
        
        # 使用pai.create创建语义层DataFrame
        columns_config = schema.get("columns", [])
        # 使用符合PandasAI要求的路径格式：organization/dataset
        dataset_path = f"semantic/{dataset_id}"
        
        # 如果配置为空，生成标准配置
        if not columns_config:
            logger.warning("⚠️ 未找到有效的字段配置，生成标准配置")
            
            # 检查是否为土地数据
            is_land_data = any(field in raw_df.columns for field in ['ZLDWDM', 'DLBM', 'TBMJ', 'QSXZ'])
            
            if is_land_data:
                logger.info("🏞️ 检测到土地数据，使用标准土地字段配置")
                columns_config = DataAnalyzer._generate_standard_land_columns(list(raw_df.columns))
            else:
                # 使用默认配置
                columns_config = [
                    {"name": col, "type": str(raw_df[col].dtype), "description": f"字段 {col}"} 
                    for col in raw_df.columns
                ]
        
        logger.info(f"🔧 使用语义层路径: {dataset_path}")
        logger.info(f"📊 配置了{len(columns_config)}个字段的语义信息")
        
        # 打印详细的列配置信息，便于调试
        for i, col_config in enumerate(columns_config[:3]):  # 只打印前3个
            logger.debug(f"🔍 列配置{i+1}: {col_config}")
        
        try:
            # 先清理可能存在的缓存，然后创建
            DataAnalyzer.clear_semantic_cache(dataset_path)
            
            # 创建语义数据框
            semantic_df = pai.create(
                path=dataset_path,
                df=pai.DataFrame(raw_df, config={"description": schema.get("description", "")}),
                description=schema.get("description", ""),
                columns=columns_config
            )
            logger.info("✅ 语义数据框创建成功")
            return semantic_df
            
        except Exception as e:
            logger.error(f"❌ 创建语义数据框失败: {str(e)}")
            # 如果失败，直接使用原始DataFrame
            logger.warning("🔄 回退到原始DataFrame模式")
            return pai.DataFrame(raw_df)
    
    @staticmethod
    def create_semantic_dataframe(file_path: str, schema: Optional[Dict[str, Any]] = None) -> Any:
        """
        使用语义层创建PandasAI DataFrame（自动生成配置）
        """
        logger = get_logger(__name__)
        
        # 如果没有提供schema，自动生成
        if schema is None:
            # 从文件名推断数据集名称，确保符合命名规范
            dataset_name = os.path.splitext(os.path.basename(file_path))[0]
            # 标准化数据集名称
            dataset_name = re.sub(r'[^\w\s-]', '', dataset_name)
            dataset_name = re.sub(r'[\s_]+', '-', dataset_name).lower().strip('-')
            dataset_name = dataset_name if dataset_name else 'default-dataset'
            
            schema = DataAnalyzer.create_semantic_schema(file_path, dataset_name=dataset_name)
        
        # 读取原始数据
        raw_df = DataAnalyzer.load_data(file_path)
        
        # 使用pai.create创建语义层DataFrame
        columns_config = schema.get("columns", [])
        # 使用符合PandasAI要求的路径格式：organization/dataset
        dataset_path = f"ask-data/{dataset_name}"
        
        # 如果配置为空，使用默认配置
        if not columns_config:
            logger.warning("⚠️ 未找到有效的字段配置，使用默认配置")
            columns_config = [
                {"name": col, "type": str(raw_df[col].dtype), "description": f"字段 {col}"} 
                for col in raw_df.columns
            ]
        
        logger.info(f"🔧 使用语义层路径: {dataset_path}")
        logger.info(f"📊 配置了{len(columns_config)}个字段的语义信息")
        
        try:
            # 先清理可能存在的缓存，然后创建
            DataAnalyzer.clear_semantic_cache(dataset_path)
            
            # 创建语义数据框
            semantic_df = pai.create(
                path=dataset_path,
                df=pai.DataFrame(raw_df, config={"description": schema.get("description", "")}),
                description=schema.get("description", ""),
                columns=columns_config
            )
            logger.info("✅ 语义数据框创建成功")
            return semantic_df
            
        except Exception as e:
            logger.error(f"❌ 创建语义数据框失败: {str(e)}")
            # 如果失败，直接使用原始DataFrame
            logger.warning("🔄 回退到原始DataFrame模式")
            return pai.DataFrame(raw_df)
    
    @staticmethod
    def generate_context(data_info):
        """根据数据分析结果生成上下文信息"""
        
        # 检查是否包含ZLDWDM字段（土地数据的标识码）
        geo_context = ""
        if 'ZLDWDM' in data_info['columns']:
            geo_context = """
地理位置识别说明：
- ZLDWDM字段是标识码，包含完整的行政区划层级信息
- 行政区划代码结构：
  * 前2位：省级代码（36 = 江西省）
  * 前4位：市级代码（3607 = 赣州市）
  * 前6位：县级代码
  * 前12位：村级代码

- 查询语法示例：
  * 查询省级：WHERE SUBSTRING(CAST(ZLDWDM AS VARCHAR), 1, 2) = '36'
  * 查询市级：WHERE SUBSTRING(CAST(ZLDWDM AS VARCHAR), 1, 4) = '3607'
  * 查询县级：WHERE SUBSTRING(CAST(ZLDWDM AS VARCHAR), 1, 6) = '360726'
  
"""
        
        context = f"""
数据结构信息：
- 数据规模：{data_info['shape'][0]}行，{data_info['shape'][1]}列
- 日期列：{', '.join(data_info['date_columns'])} ({data_info.get(data_info['date_columns'][0] + '_range', '未知范围') if data_info['date_columns'] else '无'})
- 分类列信息：{json.dumps(data_info['categorical_info'], ensure_ascii=False, indent=2)}
{geo_context}
查询要求：
1. 必须使用execute_sql_query函数
2. 仔细分析用户问题，确保查询逻辑正确
3. 对于比较分析，需要分别查询不同条件的数据
4. 注意日期格式和字段名称的准确性
5. 注意考虑数据中没用户想要的数据的情况，比如用户想要查询2010的数据，但是数据中没有2010的土地数据，那么需要告诉用户数据中没有2010的土地数据
"""
        return context
    
    # ====================== Transformations 支持方法 ======================
    
    @staticmethod
    def auto_generate_transformations(df: pd.DataFrame, data_type: str = "auto") -> List[Dict[str, Any]]:
        """
        为DataFrame自动生成transformations配置
        
        Args:
            df: pandas DataFrame
            data_type: 数据类型提示
            
        Returns:
            生成的transformations配置列表
        """
        logger = get_logger(__name__)
        
        try:
            # 使用新的转换规则管理器
            from ..utils.transformation_rules import get_rule_manager
            
            rule_manager = get_rule_manager()
            transformations = rule_manager.generate_transformations_for_dataframe(df)
            
            if transformations:
                logger.info(f"✅ 使用规则管理器生成了{len(transformations)}个数据转换配置")
                return transformations
            else:
                # 如果没有匹配的规则，使用通用的transformations生成逻辑作为回退
                logger.warning("⚠️ 没有匹配的转换规则，使用通用生成逻辑")
                transformations = TransformationsHelper.generate_transformations_for_data(df, data_type)
                logger.info(f"✅ 自动生成{len(transformations)}个数据转换配置")
                return transformations
        except Exception as e:
            logger.error(f"❌ 自动生成transformations失败: {e}")
            return []
    
# 注意：原有的 _generate_land_data_transformations 方法已被新的规则管理器替代
    # 相关配置现在存储在 utils/transformation_rules.json 文件中
    
    @staticmethod
    def apply_transformations_to_dataframe(df: pd.DataFrame, transformations: List[Dict[str, Any]]) -> pd.DataFrame:
        """
        将transformations应用到DataFrame
        
        Args:
            df: 原始DataFrame
            transformations: transformations配置列表
            
        Returns:
            应用transformations后的DataFrame
        """
        logger = get_logger(__name__)
        result_df = df.copy()
        
        for i, transformation in enumerate(transformations):
            if not transformation.get('enabled', True):
                continue
                
            try:
                trans_type = transformation['type']
                params = transformation.get('params', {})
                
                logger.debug(f"应用转换 {i+1}: {transformation.get('name', trans_type)}")
                
                # 根据转换类型应用相应的操作
                if trans_type == 'extract':
                    result_df = DataAnalyzer._apply_extract(result_df, params)
                elif trans_type == 'to_numeric':
                    result_df = DataAnalyzer._apply_to_numeric(result_df, params)
                elif trans_type == 'ensure_positive':
                    result_df = DataAnalyzer._apply_ensure_positive(result_df, params)
                elif trans_type == 'map_values':
                    result_df = DataAnalyzer._apply_map_values(result_df, params)
                elif trans_type == 'round_numbers':
                    result_df = DataAnalyzer._apply_round_numbers(result_df, params)
                elif trans_type == 'strip':
                    result_df = DataAnalyzer._apply_strip(result_df, params)
                elif trans_type == 'fill_na':
                    result_df = DataAnalyzer._apply_fill_na(result_df, params)
                elif trans_type == 'format_date':
                    result_df = DataAnalyzer._apply_format_date(result_df, params)
                # 可以继续添加更多转换类型的支持
                else:
                    logger.warning(f"⚠️ 暂不支持的转换类型: {trans_type}")
                    
            except Exception as e:
                logger.error(f"❌ 应用转换失败 {transformation.get('name', trans_type)}: {e}")
                continue
        
        return result_df
    
    @staticmethod
    def _apply_extract(df: pd.DataFrame, params: Dict[str, Any]) -> pd.DataFrame:
        """应用extract转换"""
        column = params['column']
        pattern = params['pattern']
        new_column = params.get('new_column', f"{column}_extracted")
        
        if column in df.columns:
            df[new_column] = df[column].astype(str).str.extract(pattern, expand=False)
        
        return df
    
    @staticmethod
    def _apply_format_date(df: pd.DataFrame, params: Dict[str, Any]) -> pd.DataFrame:
        """应用format_date转换"""
        column = params['column']
        format_str = params.get('format', '%Y-%m-%d')
        new_column = params.get('new_column', column)  # 默认覆盖原列，除非指定新列名
        
        if column in df.columns:
            # 确保列是datetime类型
            if not pd.api.types.is_datetime64_any_dtype(df[column]):
                # 如果不是datetime类型，先尝试转换
                try:
                    df[column] = pd.to_datetime(df[column], errors='coerce')
                except:
                    logger = get_logger(__name__)
                    logger.warning(f"⚠️ 无法将列 {column} 转换为datetime类型")
                    return df
            
            # 应用日期格式化
            df[new_column] = df[column].dt.strftime(format_str)
        
        return df
    
    @staticmethod
    def _apply_to_numeric(df: pd.DataFrame, params: Dict[str, Any]) -> pd.DataFrame:
        """应用to_numeric转换"""
        column = params['column']
        errors = params.get('errors', 'coerce')
        
        if column in df.columns:
            df[column] = pd.to_numeric(df[column], errors=errors)
        
        return df
    
    @staticmethod
    def _apply_ensure_positive(df: pd.DataFrame, params: Dict[str, Any]) -> pd.DataFrame:
        """应用ensure_positive转换"""
        column = params['column']
        drop_negative = params.get('drop_negative', False)
        
        if column in df.columns:
            if drop_negative:
                df = df[df[column] >= 0]
            else:
                df[column] = df[column].abs()
        
        return df
    
    @staticmethod
    def _apply_map_values(df: pd.DataFrame, params: Dict[str, Any]) -> pd.DataFrame:
        """应用map_values转换"""
        column = params['column']
        mapping = params['mapping']
        new_column = params.get('new_column', f"{column}_mapped")
        
        if column in df.columns:
            df[new_column] = df[column].map(mapping).fillna(df[column])
        
        return df
    
    @staticmethod
    def _apply_round_numbers(df: pd.DataFrame, params: Dict[str, Any]) -> pd.DataFrame:
        """应用round_numbers转换"""
        column = params['column']
        decimals = params.get('decimals', 2)
        
        if column in df.columns and pd.api.types.is_numeric_dtype(df[column]):
            df[column] = df[column].round(decimals)
        
        return df
    
    @staticmethod
    def _apply_strip(df: pd.DataFrame, params: Dict[str, Any]) -> pd.DataFrame:
        """应用strip转换"""
        column = params['column']
        
        if column in df.columns and df[column].dtype == 'object':
            df[column] = df[column].astype(str).str.strip()
        
        return df
    
    @staticmethod
    def _apply_fill_na(df: pd.DataFrame, params: Dict[str, Any]) -> pd.DataFrame:
        """应用fill_na转换"""
        column = params['column']
        value = params['value']
        
        if column in df.columns:
            df[column] = df[column].fillna(value)
        
        return df
    
    @staticmethod
    def create_enhanced_semantic_dataframe(file_path: str, dataset_id: str, 
                                         db: SchemaDatabase = None, 
                                         auto_transformations: bool = True) -> Any:
        """
        创建增强的语义DataFrame，包含transformations支持
        
        Args:
            file_path: 数据文件路径
            dataset_id: 数据集ID
            db: 数据库连接
            auto_transformations: 是否自动生成transformations
            
        Returns:
            增强的语义DataFrame
        """
        logger = get_logger(__name__)
        
        try:
            # 1. 读取原始数据
            raw_df = DataAnalyzer.load_data(file_path)
            
            # 2. 从数据库获取schema配置
            if db is None:
                settings = Settings()
                db = SchemaDatabase(settings)
            
            schema = db.get_dataset_schema(dataset_id)
            
            # 3. 如果需要，自动生成transformations
            if auto_transformations and (not schema or not schema.get('transformations')):
                logger.info("🔄 自动生成transformations配置")
                auto_trans = DataAnalyzer.auto_generate_transformations(raw_df, "land")
                
                # 保存到数据库
                if auto_trans:
                    db.save_transformations(dataset_id, auto_trans)
                    logger.info(f"✅ 已保存{len(auto_trans)}个自动生成的transformations")
                    
                    # 重新获取schema（包含transformations）
                    schema = db.get_dataset_schema(dataset_id)
            
            # 4. 应用transformations到DataFrame
            transformed_df = raw_df.copy()
            if schema and schema.get('transformations'):
                logger.info(f"🔧 应用{len(schema['transformations'])}个transformations")
                transformed_df = DataAnalyzer.apply_transformations_to_dataframe(
                    transformed_df, 
                    schema['transformations']
                )
                logger.info(f"✅ Transformations应用完成，数据形状: {transformed_df.shape}")
            
            # 5. 构建列配置
            columns_config = []
            if schema and schema.get("columns"):
                columns_config = schema["columns"]
            else:
                # 使用默认配置
                logger.warning("⚠️ 未找到有效的字段配置，使用默认配置")
                for col in transformed_df.columns:
                    dtype = str(transformed_df[col].dtype)
                    if 'int' in dtype:
                        col_type = "integer"
                    elif 'float' in dtype:
                        col_type = "float"
                    elif 'datetime' in dtype:
                        col_type = "datetime"
                    elif 'bool' in dtype:
                        col_type = "boolean"
                    else:
                        col_type = "string"
                    
                    columns_config.append({
                        "name": col,
                        "type": col_type,
                        "description": f"字段 {col}"
                    })
            
            # 6. 构建PandasAI v3格式的schema
            dataset_path = f"semantic/{dataset_id}"
            description = schema.get("description", f"数据集 {dataset_id}") if schema else f"数据集 {dataset_id}"
            
            # 7. 清理缓存
            DataAnalyzer.clear_semantic_cache(dataset_path)
            
            # 8. 直接使用处理后的DataFrame创建语义DataFrame
            logger.info(f"📊 使用处理后的数据创建语义DataFrame，形状: {transformed_df.shape}")
            
            # 直接使用pai.DataFrame创建，不使用pai.create
            semantic_df = pai.DataFrame(transformed_df)
            
            logger.info("✅ 增强语义数据框创建成功（直接使用处理后的数据）")
            return semantic_df
            
        except Exception as e:
            logger.error(f"❌ 创建增强语义数据框失败: {e}")
            # 回退到基础模式
            return DataAnalyzer.create_semantic_dataframe_from_config(file_path, dataset_id, db)
 