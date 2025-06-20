"""
语义层配置数据库管理模块

提供语义层配置的PostgreSQL数据库存储和管理功能
"""

import json
import psycopg2
from psycopg2.extras import RealDictCursor
from psycopg2 import sql
from typing import Dict, List, Any, Optional, Union
from datetime import datetime
import yaml
import os
# 处理相对导入问题
try:
    from .logger import get_logger, LogContext
except ImportError:
    # 当作为脚本独立运行时，创建简单的日志器
    import logging
    
    def get_logger(name=None):
        logger = logging.getLogger(name or __name__)
        if not logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter(
                '%(asctime)s | %(levelname)s | %(name)s | %(message)s'
            )
            handler.setFormatter(formatter)
            logger.addHandler(handler)
            logger.setLevel(logging.INFO)
        return logger
    
    class LogContext:
        def __init__(self, logger, message, level=logging.INFO):
            self.logger = logger
            self.message = message
        
        def __enter__(self):
            self.logger.info(f"开始: {self.message}")
            return self
        
        def __exit__(self, exc_type, exc_val, exc_tb):
            if exc_type is None:
                self.logger.info(f"完成: {self.message}")
            else:
                self.logger.error(f"失败: {self.message} - {exc_val}")

class SchemaDatabase:
    """语义层配置PostgreSQL数据库管理类"""
    
    def __init__(self, connection_params: Union[Dict[str, str], 'Settings'] = None):
        """
        初始化数据库连接
        
        Args:
            connection_params: 数据库连接参数字典或Settings对象
                如果是字典:
                {
                    'host': 'localhost',
                    'port': '5432',
                    'database': 'ask_data',
                    'user': 'username',
                    'password': 'password'
                }
                如果是Settings对象，将自动获取PostgreSQL配置
        """
        self.logger = get_logger(__name__)
        if connection_params is None:
            # 如果没有提供配置，尝试从环境变量创建Settings
            from ..config.settings import Settings
            settings = Settings()
            self.connection_params = settings.get_postgres_config()
        elif hasattr(connection_params, 'get_postgres_config'):
            # 如果是Settings对象
            self.connection_params = connection_params.get_postgres_config()
        else:
            # 如果是字典
            self.connection_params = connection_params
        
        self.logger.info(f"初始化数据库连接 - 主机: {self.connection_params.get('host')}")
        self._init_database()
    
    def _get_connection(self):
        """获取数据库连接"""
        # 确保数据库名称不包含注释
        if 'database' in self.connection_params:
            self.connection_params['database'] = self.connection_params['database'].split('#')[0].strip()
        return psycopg2.connect(**self.connection_params)
    
    def test_connection(self):
        """测试数据库连接"""
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT 1")
            cursor.fetchone()
            cursor.close()
            conn.close()
            self.logger.debug("数据库连接测试成功")
            return True
        except Exception as e:
            self.logger.error(f"数据库连接测试失败: {e}")
            raise
    
    def _init_database(self):
        """初始化数据库表结构（仅在表不存在时创建）"""
        self.logger.debug("开始检查数据库表结构")
        
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            with LogContext(self.logger, "检查并创建数据库表结构"):
                # 数据集主表
                cursor.execute('''
                CREATE TABLE IF NOT EXISTS ask_data_datasets (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    file_path VARCHAR(500),
                    original_filename VARCHAR(255),
                    original_file_type VARCHAR(10),
                    actual_data_path VARCHAR(500),
                    data_file_type VARCHAR(10),
                    is_converted BOOLEAN DEFAULT FALSE,
                    conversion_info JSONB,
                    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'draft', 'pending')),
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by VARCHAR(100)
                )
                ''')
            
                # 列配置表
                cursor.execute('''
                CREATE TABLE IF NOT EXISTS ask_data_dataset_columns (
                    id BIGSERIAL PRIMARY KEY,
                    dataset_id BIGINT NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    type VARCHAR(50) NOT NULL CHECK (type IN ('integer', 'float', 'string', 'datetime', 'boolean')),
                    description TEXT,
                    alias VARCHAR(255),
                    sort_order INTEGER DEFAULT 0,
                    is_required BOOLEAN DEFAULT FALSE,
                    default_value VARCHAR(255),
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    
                    CONSTRAINT fk_ask_data_dataset_columns_dataset 
                        FOREIGN KEY (dataset_id) REFERENCES ask_data_datasets(id) ON DELETE CASCADE,
                    CONSTRAINT uk_ask_data_dataset_columns_dataset_name 
                        UNIQUE (dataset_id, name)
                )
                ''')
                
                # 数据转换配置表
                cursor.execute('''
                CREATE TABLE IF NOT EXISTS ask_data_dataset_transformations (
                    id BIGSERIAL PRIMARY KEY,
                    dataset_id BIGINT NOT NULL,
                    transformation_name VARCHAR(255) NOT NULL,
                    transformation_type VARCHAR(100) NOT NULL CHECK (transformation_type IN (
                        'to_lowercase', 'to_uppercase', 'strip', 'truncate', 'pad', 'extract',
                        'round_numbers', 'scale', 'clip', 'normalize', 'standardize', 'ensure_positive', 'bin',
                        'convert_timezone', 'format_date', 'to_datetime', 'validate_date_range',
                        'fill_na', 'replace', 'remove_duplicates', 'normalize_phone',
                        'encode_categorical', 'map_values', 'standardize_categories', 'rename',
                        'validate_email', 'validate_foreign_key', 'anonymize', 'to_numeric'
                    )),
                    target_column VARCHAR(255),
                    parameters JSONB,
                    sort_order INTEGER DEFAULT 0,
                    is_enabled BOOLEAN DEFAULT TRUE,
                    description TEXT,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    
                    CONSTRAINT fk_ask_data_dataset_transformations_dataset 
                        FOREIGN KEY (dataset_id) REFERENCES ask_data_datasets(id) ON DELETE CASCADE
                )
                ''')
            
                # 创建索引
                indexes = [
                    'CREATE INDEX IF NOT EXISTS idx_ask_data_datasets_status ON ask_data_datasets(status)',
                    'CREATE INDEX IF NOT EXISTS idx_ask_data_dataset_columns_dataset_id ON ask_data_dataset_columns(dataset_id)',
                    'CREATE INDEX IF NOT EXISTS idx_ask_data_dataset_columns_type ON ask_data_dataset_columns(type)',
                    'CREATE INDEX IF NOT EXISTS idx_ask_data_dataset_transformations_dataset_id ON ask_data_dataset_transformations(dataset_id)',
                    'CREATE INDEX IF NOT EXISTS idx_ask_data_dataset_transformations_type ON ask_data_dataset_transformations(transformation_type)',
                    'CREATE INDEX IF NOT EXISTS idx_ask_data_dataset_transformations_enabled ON ask_data_dataset_transformations(is_enabled)',
                ]
                
                for index in indexes:
                    cursor.execute(index)
                
                conn.commit()
                self.logger.info("✅ PostgreSQL数据库表结构检查完成（仅创建不存在的表）")
                
        except Exception as e:
            conn.rollback()
            self.logger.error(f"❌ 数据库初始化失败: {str(e)}")
            raise
        finally:
            cursor.close()
            conn.close()
    
    def save_schema_from_yaml(self, yaml_path: str, created_by: str = None) -> int:
        """
        从YAML文件保存语义层配置到数据库
        
        Args:
            yaml_path: YAML配置文件路径
            created_by: 创建者
            
        Returns:
            数据集ID
        """
        with open(yaml_path, 'r', encoding='utf-8') as f:
            schema_config = yaml.safe_load(f)
        
        return self.save_schema(schema_config, yaml_path, created_by)
    
    def save_schema(self, schema_config: Dict[str, Any], file_path: str = None, created_by: str = None, dataset_id: str = None) -> int:
        """
        保存语义层配置到数据库
        
        Args:
            schema_config: 语义层配置字典
            file_path: 关联的文件路径
            created_by: 创建者
            dataset_id: 数据集ID
            
        Returns:
            数据集ID
        """
        conn = self._get_connection()
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        
        try:
            # 保存数据集基本信息
            dataset_name = os.path.splitext(os.path.basename(file_path))[0] if file_path else 'unknown_dataset'
            description = schema_config.get('description', '')
            
            cursor.execute('''
                INSERT INTO ask_data_datasets (name, description, file_path, created_by, updated_at)
                VALUES (%s, %s, %s, %s, %s)
                RETURNING id
            ''', (dataset_name, description, file_path, created_by, datetime.now()))
            
            result = cursor.fetchone()
            dataset_id = result['id']
            
            # 清理现有配置
            cursor.execute('DELETE FROM ask_data_dataset_columns WHERE dataset_id = %s', (dataset_id,))
            
            # 保存列配置
            columns = schema_config.get('columns', [])
            for i, column in enumerate(columns):
                cursor.execute('''
                    INSERT INTO ask_data_dataset_columns 
                    (dataset_id, name, type, description, alias, sort_order, is_required, default_value)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                ''', (
                    dataset_id,
                    column['name'],
                    column['type'],
                    column.get('description', ''),
                    column.get('alias', None),
                    i,
                    column.get('is_required', False),
                    column.get('default_value')
                ))
            
            # 保存业务指标
            metrics = schema_config.get('metrics', {})
            for metric_key, metric_config in metrics.items():
                cursor.execute('''
                    INSERT INTO ask_data_dataset_metrics 
                    (dataset_id, name, description, expression)
                    VALUES (%s, %s, %s, %s)
                ''', (
                    dataset_id,
                    metric_key,
                    metric_config.get('description', ''),
                    metric_config.get('expression', '')
                ))
            
            # 保存查询模板
            query_templates = schema_config.get('query_templates', [])
            for template in query_templates:
                cursor.execute('''
                    INSERT INTO ask_data_dataset_query_templates 
                    (dataset_id, name, description, example, category)
                    VALUES (%s, %s, %s, %s, %s)
                ''', (
                    dataset_id,
                    template['name'],
                    template.get('description', ''),
                    template.get('example', ''),
                    template.get('category', 'general')
                ))
            
            # 保存transformations配置
            if schema_config.get('transformations'):
                for i, transformation in enumerate(schema_config['transformations']):
                    cursor.execute('''
                        INSERT INTO ask_data_dataset_transformations (
                            dataset_id, transformation_name, transformation_type, 
                            target_column, parameters, sort_order, is_enabled, description
                        ) VALUES (
                            %s, %s, %s, %s, %s, %s, %s, %s
                        )
                    ''', (
                        dataset_id,
                        transformation.get('name', f"transformation_{i+1}"),
                        transformation['type'],
                        transformation.get('params', {}).get('column'),
                        json.dumps(transformation.get('params', {})),
                        i,
                        transformation.get('enabled', True),
                        transformation.get('description', '')
                    ))
            
            conn.commit()
            self.logger.info(f"✅ 语义层配置已保存到PostgreSQL数据库，数据集ID: {dataset_id}")
            return dataset_id
            
        except Exception as e:
            conn.rollback()
            self.logger.error(f"❌ 保存语义层配置失败: {str(e)}")
            raise
        finally:
            cursor.close()
            conn.close()
    
    def load_schema(self, dataset_id: str) -> Optional[Dict[str, Any]]:
        """
        从数据库加载语义层配置
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            语义层配置字典
        """
        conn = self._get_connection()
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        
        try:
            # 获取数据集基本信息
            # 只支持通过id查询
            dataset_id_int = int(dataset_id)
            cursor.execute('SELECT * FROM ask_data_datasets WHERE id = %s AND status = %s', 
                         (dataset_id_int, 'active'))
            dataset = cursor.fetchone()
            if not dataset:
                return None
            
            dataset_id = dataset['id']
            
            # 构建配置字典
            schema_config = {
                'name': dataset['name'],
                'description': dataset['description'],
                'columns': []
            }
            
            # 加载列配置
            cursor.execute('''
                SELECT * FROM ask_data_dataset_columns 
                WHERE dataset_id = %s 
                ORDER BY sort_order
            ''', (dataset_id,))
            
            for column in cursor.fetchall():
                column_config = {
                    'name': column['name'],
                    'type': column['type'],
                    'description': column['description']
                }
                if column['alias']:
                    column_config['alias'] = column['alias']
                schema_config['columns'].append(column_config)
            
            return schema_config
            
        except Exception as e:
            self.logger.error(f"加载语义配置失败: {e}")
            return None
        finally:
            cursor.close()
            conn.close()
    
    def list_datasets(self, tree_node_id: str = None) -> List[Dict[str, Any]]:
        """
        列出数据集，支持按树节点筛选
        
        Args:
            tree_node_id: 树节点ID，如果指定则返回该节点及其子节点下的数据集
                        如果为None或"0"，则返回所有数据集
        
        Returns:
            数据集列表
        """
        conn = self._get_connection()
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        
        try:
            # 构建基础查询
            base_query = '''
                SELECT d.*, 
                       COUNT(DISTINCT c.id) as column_count
                FROM ask_data_datasets d
                LEFT JOIN ask_data_dataset_columns c ON d.id = c.dataset_id
                WHERE d.status = 'active'
            '''
            
            query_params = []
            
            # 如果指定了tree_node_id，添加节点筛选条件
            if tree_node_id and tree_node_id != "0":
                # 获取指定节点及其所有子节点的ID列表
                node_ids = self.get_node_and_children_ids(tree_node_id)
                
                if node_ids:
                    # 添加IN条件筛选
                    placeholders = ','.join(['%s'] * len(node_ids))
                    base_query += f' AND d.tree_node_id IN ({placeholders})'
                    query_params.extend(node_ids)
                else:
                    # 如果没有找到节点，返回空结果
                    base_query += ' AND 1=0'
            
            # 添加分组和排序
            final_query = base_query + '''
                GROUP BY d.id
                ORDER BY d.updated_at DESC
            '''
            
            self.logger.debug(f"执行查询: {final_query}")
            self.logger.debug(f"查询参数: {query_params}")
            
            cursor.execute(final_query, query_params)
            
            datasets = []
            for row in cursor.fetchall():
                datasets.append({
                    'id': row['id'],
                    'name': row['name'],
                    'description': row['description'],
                    'file_path': row['file_path'],
                    'tree_node_id': row.get('tree_node_id'),  # 添加tree_node_id字段
                    'column_count': row['column_count'],
                    'created_at': row['created_at'].isoformat() if row['created_at'] else None,
                    'updated_at': row['updated_at'].isoformat() if row['updated_at'] else None
                })
            
            self.logger.info(f"查询到 {len(datasets)} 个数据集 (tree_node_id: {tree_node_id})")
            return datasets
            
        except Exception as e:
            self.logger.error(f"获取数据集列表失败: {str(e)}")
            return []
        finally:
            cursor.close()
            conn.close()
    
    def delete_dataset(self, dataset_id: str) -> bool:
        """
        删除数据集配置（软删除）
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            是否删除成功
        """
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            # 只支持通过id删除
            dataset_id_int = int(dataset_id)
            cursor.execute(
                'UPDATE ask_data_datasets SET status = %s, updated_at = %s WHERE id = %s', 
                ('inactive', datetime.now(), dataset_id_int)
            )
            conn.commit()
            
            if cursor.rowcount > 0:
                self.logger.info(f"✅ 数据集已删除: {dataset_id}")
                return True
            else:
                self.logger.warning(f"⚠️ 未找到数据集: {dataset_id}")
                return False
                
        except Exception as e:
            conn.rollback()
            self.logger.error(f"❌ 删除数据集失败: {str(e)}")
            return False
        finally:
            cursor.close()
            conn.close()
    
    def export_to_yaml(self, dataset_path: str, output_path: str) -> bool:
        """
        将数据库中的配置导出为YAML文件
        
        Args:
            dataset_path: 数据集路径
            output_path: 输出YAML文件路径
            
        Returns:
            是否导出成功
        """
        schema_config = self.load_schema(dataset_path)
        if not schema_config:
            print(f"❌ 未找到数据集配置: {dataset_path}")
            return False
        
        try:
            with open(output_path, 'w', encoding='utf-8') as f:
                yaml.dump(schema_config, f, default_flow_style=False, allow_unicode=True)
            
            print(f"✅ 配置已导出到: {output_path}")
            return True
            
        except Exception as e:
            print(f"❌ 导出YAML失败: {str(e)}")
            return False
    
    def get_database_stats(self) -> Dict[str, int]:
        """
        获取数据库统计信息
        
        Returns:
            统计信息字典
        """
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            # 获取数据集总数
            cursor.execute("SELECT COUNT(*) FROM ask_data_datasets WHERE status = 'active'")
            total_datasets = cursor.fetchone()[0]
            
            # 获取列总数
            cursor.execute("""
                SELECT COUNT(*) FROM ask_data_dataset_columns c
                JOIN ask_data_datasets d ON c.dataset_id = d.id
                WHERE d.status = 'active'
            """)
            total_columns = cursor.fetchone()[0]
            
            # 获取指标总数
            cursor.execute("""
                SELECT COUNT(*) FROM ask_data_dataset_metrics m
                JOIN ask_data_datasets d ON m.dataset_id = d.id
                WHERE d.status = 'active' AND m.is_enabled = true
            """)
            total_metrics = cursor.fetchone()[0]
            
            return {
                'total_datasets': total_datasets,
                'total_columns': total_columns,
                'total_metrics': total_metrics,
                'total_queries': 0  # 查询历史功能未实现
            }
            
        except Exception as e:
            self.logger.error(f"获取数据库统计失败: {e}")
            return {
                'total_datasets': 0,
                'total_columns': 0,
                'total_metrics': 0,
                'total_queries': 0
            }
        finally:
            cursor.close()
            conn.close()
    
    def init_database(self, force_recreate: bool = False) -> bool:
        """
        初始化数据库表结构
        
        Args:
            force_recreate: 是否强制重新创建表（已禁用，安全考虑）
            
        Returns:
            是否初始化成功
        """
        try:
            if force_recreate:
                self.logger.error("❌ 强制重新创建表结构已被禁用，安全考虑")
                return False
            
            self.logger.info("✅ 数据库表结构已存在，无需初始化")
            return True
            
        except Exception as e:
            self.logger.error(f"初始化数据库失败: {e}", exc_info=True)
            return False
    
    def get_dataset_by_id(self, dataset_id: int) -> Optional[Dict[str, Any]]:
        """
        根据数据集ID获取数据集信息
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            数据集信息字典，如果不存在返回None
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 根据自增ID查询
            cursor.execute(
                "SELECT * FROM ask_data_datasets WHERE id = %s AND status != 'inactive'",
                (dataset_id,)
            )
            result = cursor.fetchone()
            
            cursor.close()
            conn.close()
            
            if result:
                return dict(result)
            else:
                self.logger.warning(f"未找到数据集: {dataset_id}")
                return None
                
        except Exception as e:
            self.logger.error(f"获取数据集信息失败: {e}")
            return None
    
    def get_dataset_schema(self, dataset_id: str) -> Optional[Dict[str, Any]]:
        """
        获取数据集的语义配置
        
        Args:
            dataset_id: 数据集ID (支持int和str类型)
            
        Returns:
            Optional[Dict[str, Any]]: 语义配置字典，如果不存在则返回None
        """
        with LogContext(self.logger, f"获取数据集语义配置: {dataset_id}"):
            conn = None
            try:
                conn = self._get_connection()
                cursor = conn.cursor(cursor_factory=RealDictCursor)
                
                # 1. 获取数据集基本信息和ID
                # 只支持通过id查询
                dataset_id_int = int(dataset_id)
                cursor.execute('''
                    SELECT id, name, description FROM ask_data_datasets 
                    WHERE id = %s AND status = 'active'
                ''', (dataset_id_int,))
                
                result = cursor.fetchone()
                if not result:
                    return None
                    
                dataset_db_id = result['id']
                dataset_name = result['name']
                dataset_description = result['description']
                
                # 2. 获取列配置
                cursor.execute('''
                    SELECT 
                        name, type, description, alias,
                        sort_order, is_required, default_value
                    FROM ask_data_dataset_columns
                    WHERE dataset_id = %s
                    ORDER BY sort_order, name
                ''', (dataset_db_id,))
                
                columns = cursor.fetchall()
                
                # 3. 获取transformations配置
                cursor.execute('''
                    SELECT 
                        transformation_name, transformation_type, target_column,
                        parameters, sort_order, is_enabled, description
                    FROM ask_data_dataset_transformations
                    WHERE dataset_id = %s AND is_enabled = TRUE
                    ORDER BY sort_order
                ''', (dataset_db_id,))
                
                transformations = cursor.fetchall()
                
                # 4. 组装配置
                schema_config = {
                    'name': dataset_name,
                    'description': dataset_description,
                    'columns': [dict(col) for col in columns]
                }
                
                # 添加transformations配置
                if transformations:
                    schema_config['transformations'] = []
                    for trans in transformations:
                        trans_dict = dict(trans)
                        # 解析JSON参数 - 检查是否已经是dict类型
                        params = {}
                        if trans_dict['parameters']:
                            if isinstance(trans_dict['parameters'], dict):
                                # 如果已经是dict，直接使用
                                params = trans_dict['parameters']
                            elif isinstance(trans_dict['parameters'], str):
                                # 如果是字符串，则解析JSON
                                params = json.loads(trans_dict['parameters'])
                            else:
                                # 其他类型，尝试转换为dict
                                try:
                                    params = dict(trans_dict['parameters'])
                                except:
                                    params = {}
                        
                        schema_config['transformations'].append({
                            'name': trans_dict['transformation_name'],
                            'type': trans_dict['transformation_type'],
                            'params': params,
                            'enabled': trans_dict['is_enabled'],
                            'description': trans_dict.get('description', '')
                        })
                
                conn.commit()  # 确保事务正确结束
                return schema_config
                
            except Exception as e:
                if conn:
                    conn.rollback()  # 回滚事务
                self.logger.error(f"获取数据集语义配置失败: {e}")
                return None
                
            finally:
                if cursor:
                    cursor.close()
                if conn:
                    conn.close()
    
    def list_all_datasets(self, tree_node_id: str = None) -> List[Dict[str, Any]]:
        """
        获取所有数据集列表，支持按树节点筛选
        
        Args:
            tree_node_id: 树节点ID，如果指定则返回该节点及其子节点下的数据集
                        如果为None或"0"，则返回所有数据集
        
        Returns:
            List[Dict[str, Any]]: 数据集列表
        """
        with LogContext(self.logger, f"获取数据集列表 (tree_node_id: {tree_node_id})"):
            conn = None
            try:
                conn = self._get_connection()
                cursor = conn.cursor(cursor_factory=RealDictCursor)
                
                # 构建基础查询
                base_query = '''
                    SELECT 
                        id,
                        name,
                        description,
                        file_path,
                        original_filename,
                        original_file_type,
                        actual_data_path,
                        data_file_type,
                        is_converted,
                        conversion_info,
                        tree_node_id,
                        status,
                        created_at,
                        updated_at,
                        created_by
                    FROM ask_data_datasets 
                    WHERE status != 'inactive'
                '''
                
                query_params = []
                
                # 如果指定了tree_node_id，添加节点筛选条件
                if tree_node_id and tree_node_id != "0":
                    # 获取指定节点及其所有子节点的ID列表
                    node_ids = self.get_node_and_children_ids(tree_node_id)
                    
                    if node_ids:
                        # 添加IN条件筛选
                        placeholders = ','.join(['%s'] * len(node_ids))
                        base_query += f' AND tree_node_id IN ({placeholders})'
                        query_params.extend(node_ids)
                    else:
                        # 如果没有找到节点，返回空结果
                        base_query += ' AND 1=0'
                
                # 添加排序
                final_query = base_query + ' ORDER BY created_at DESC'
                
                cursor.execute(final_query, query_params)
                
                datasets = cursor.fetchall()
                cursor.close()
                conn.commit()  # 确保事务正确结束
                
                return [dict(dataset) for dataset in datasets]
                
            except Exception as e:
                if conn:
                    conn.rollback()  # 回滚事务
                self.logger.error(f"获取数据集列表失败: {e}")
                return []  # 返回空列表而不是抛出异常
                
            finally:
                if conn:
                    conn.close()
    
    def save_schema_with_dataset_id(self, dataset_id: str, name: str, description: str, 
                                   file_path: str, schema_config: Dict[str, Any] = None,
                                   created_by: str = "system") -> int:
        """
        使用dataset_id保存数据集和schema配置
        
        Args:
            dataset_id: 数据集ID
            name: 数据集名称
            description: 描述
            file_path: 文件路径
            schema_config: schema配置
            created_by: 创建者
            
        Returns:
            数据集数据库ID
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            # 插入数据集基本信息
            import os
            original_filename = os.path.basename(file_path) if file_path else "unknown"
            file_extension = os.path.splitext(original_filename)[1].lower().lstrip('.') if original_filename else "csv"
            
            cursor.execute(
                """
                INSERT INTO ask_data_datasets 
                (name, description, file_path, status, created_by, 
                 original_filename, original_file_type, actual_data_path, 
                 data_file_type, is_converted, conversion_info)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                RETURNING id
                """,
                (name, description, file_path, 'active', created_by,
                 original_filename, file_extension, file_path, 'csv', True, {})
            )
            
            dataset_db_id = cursor.fetchone()[0]
            
            # 如果有schema配置，插入列信息
            if schema_config and schema_config.get('columns'):
                for i, column in enumerate(schema_config['columns']):
                    cursor.execute(
                        """
                        INSERT INTO ask_data_dataset_columns
                        (dataset_id, name, type, description, sort_order)
                        VALUES (%s, %s, %s, %s, %s)
                        """,
                        (dataset_db_id, column['name'], column['type'], 
                         column.get('description', ''), i)
                    )
            
            conn.commit()
            cursor.close()
            conn.close()
            
            self.logger.info(f"✅ 数据集保存成功: {dataset_id} (DB ID: {dataset_db_id})")
            return dataset_db_id
            
        except Exception as e:
            self.logger.error(f"保存数据集失败: {e}")
            raise
    
    def update_dataset(self, dataset_id: int, updates: Dict[str, Any]) -> bool:
        """
        更新数据集信息
        
        Args:
            dataset_id: 数据集ID
            updates: 要更新的字段
            
        Returns:
            是否更新成功
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            # 构建更新SQL
            set_clauses = []
            values = []
            
            for field, value in updates.items():
                if field in ['name', 'description', 'file_path', 'actual_data_path', 'status']:
                    set_clauses.append(f"{field} = %s")
                    values.append(value)
            
            if not set_clauses:
                return True  # 没有需要更新的字段
            
            # 添加updated_at
            set_clauses.append("updated_at = CURRENT_TIMESTAMP")
            values.append(dataset_id)
            
            sql = f"""
                UPDATE ask_data_datasets 
                SET {', '.join(set_clauses)}
                WHERE id = %s
            """
            
            cursor.execute(sql, values)
            rows_affected = cursor.rowcount
            
            conn.commit()
            cursor.close()
            conn.close()
            
            return rows_affected > 0
            
        except Exception as e:
            self.logger.error(f"更新数据集失败: {e}")
            return False
    
    def delete_dataset_by_id(self, dataset_id: int) -> bool:
        """
        删除数据集（软删除）
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            是否删除成功
        """
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                """
                UPDATE ask_data_datasets 
                SET status = 'inactive', updated_at = CURRENT_TIMESTAMP
                WHERE id = %s AND status != 'inactive'
                """,
                (dataset_id,)
            )
            
            rows_affected = cursor.rowcount
            conn.commit()
            cursor.close()
            conn.close()
            
            if rows_affected > 0:
                self.logger.info(f"✅ 数据集已删除: {dataset_id}")
                return True
            else:
                self.logger.warning(f"⚠️ 数据集未找到或已删除: {dataset_id}")
                return False
                
        except Exception as e:
            self.logger.error(f"删除数据集失败: {e}")
            return False
    
    def save_dataset(self, dataset: Dict[str, Any]) -> int:
        """
        保存数据集信息
        
        Args:
            dataset: 数据集信息字典，包含：
                - name: 数据集名称
                - description: 数据集描述
                - file_path: 原始文件路径
                - original_filename: 原始文件名
                - original_file_type: 原始文件类型 (.csv, .shp)
                - actual_data_path: 实际数据文件路径（用于查询）
                - data_file_type: 实际数据文件类型
                - is_converted: 是否经过转换
                - conversion_info: 转换信息（JSON格式）
                - status: 数据集状态
                - created_at: 创建时间
                - updated_at: 更新时间
                
        Returns:
            int: 数据集ID
        """
        with LogContext(self.logger, f"保存数据集: {dataset.get('name')}"):
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            try:
                # 1. 插入数据集记录
                cursor.execute('''
                                    INSERT INTO ask_data_datasets (
                    name, description, file_path, 
                    original_filename, original_file_type, actual_data_path, data_file_type,
                    is_converted, conversion_info, tree_node_id,
                    status, created_at, updated_at
                ) VALUES (
                    %(name)s, %(description)s, %(file_path)s,
                    %(original_filename)s, %(original_file_type)s, %(actual_data_path)s, %(data_file_type)s,
                    %(is_converted)s, %(conversion_info)s, %(tree_node_id)s,
                    %(status)s, to_timestamp(%(created_at)s), to_timestamp(%(updated_at)s)
                ) RETURNING id
                ''', dataset)
                
                dataset_db_id = cursor.fetchone()['id']
                conn.commit()
                
                self.logger.info(f"数据集保存成功: ID={dataset_db_id}")
                return dataset_db_id
                
            except Exception as e:
                conn.rollback()
                self.logger.error(f"保存数据集失败: {e}")
                raise
            finally:
                cursor.close()
                conn.close()

    def update_dataset_status(self, dataset_id: str, status: str) -> bool:
        """
        更新数据集状态
        
        Args:
            dataset_id: 数据集ID
            status: 新状态
            
        Returns:
            bool: 是否更新成功
        """
        with LogContext(self.logger, f"更新数据集状态: {dataset_id} -> {status}"):
            conn = self._get_connection()
            cursor = conn.cursor()
            
            try:
                cursor.execute('''
                    UPDATE ask_data_datasets 
                    SET status = %s, updated_at = CURRENT_TIMESTAMP
                    WHERE id = %s
                ''', (status, int(dataset_id)))
                
                success = cursor.rowcount > 0
                conn.commit()
                
                if success:
                    self.logger.info(f"数据集状态更新成功: {dataset_id} -> {status}")
                else:
                    self.logger.warning(f"数据集不存在: {dataset_id}")
                
                return success
                
            except Exception as e:
                conn.rollback()
                self.logger.error(f"更新数据集状态失败: {e}")
                raise
            finally:
                cursor.close()
                conn.close()

    def save_schema(self, dataset_id: int, schema_config: Dict[str, Any]) -> bool:
        """
        保存数据集的语义配置
        
        Args:
            dataset_id: 数据集ID
            schema_config: 语义配置字典
            
        Returns:
            bool: 是否保存成功
        """
        with LogContext(self.logger, f"保存语义配置: {dataset_id}"):
            conn = self._get_connection()
            cursor = conn.cursor()
            
            try:
                # 1. 验证数据集存在
                cursor.execute('''
                    SELECT id FROM ask_data_datasets WHERE id = %s
                ''', (dataset_id,))
                
                result = cursor.fetchone()
                if not result:
                    raise ValueError(f"数据集不存在: {dataset_id}")
                
                dataset_db_id = dataset_id
                
                # 2. 删除现有配置
                cursor.execute('''
                    DELETE FROM ask_data_dataset_columns WHERE dataset_id = %s
                ''', (dataset_db_id,))
                
                # 3. 保存列配置
                if schema_config.get('columns'):
                    for i, column in enumerate(schema_config['columns']):
                        cursor.execute('''
                            INSERT INTO ask_data_dataset_columns (
                                dataset_id, name, type, description, 
                                alias, sort_order, is_required, default_value
                            ) VALUES (
                                %s, %s, %s, %s, %s, %s, %s, %s
                            )
                        ''', (
                            dataset_db_id,
                            column['name'],
                            column['type'],
                            column.get('description'),
                            column.get('alias'),
                            i,
                            column.get('is_required', False),
                            column.get('default_value')
                        ))
                
                # 4. 保存transformations配置
                if schema_config.get('transformations'):
                    for i, transformation in enumerate(schema_config['transformations']):
                        cursor.execute('''
                            INSERT INTO ask_data_dataset_transformations (
                                dataset_id, transformation_name, transformation_type, 
                                target_column, parameters, sort_order, is_enabled, description
                            ) VALUES (
                                %s, %s, %s, %s, %s, %s, %s, %s
                            )
                        ''', (
                            dataset_db_id,
                            transformation.get('name', f"transformation_{i+1}"),
                            transformation['type'],
                            transformation.get('params', {}).get('column'),
                            json.dumps(transformation.get('params', {})),
                            i,
                            transformation.get('enabled', True),
                            transformation.get('description', '')
                        ))
                
                conn.commit()
                self.logger.info(f"语义配置保存成功: {dataset_id}")
                return True
                
            except Exception as e:
                conn.rollback()
                self.logger.error(f"保存语义配置失败: {e}")
                raise
            finally:
                cursor.close()
                conn.close()

    def delete_schema(self, dataset_id: str) -> bool:
        """
        删除数据集的语义配置
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            bool: 是否删除成功
        """
        with LogContext(self.logger, f"删除语义配置: {dataset_id}"):
            conn = self._get_connection()
            cursor = conn.cursor()
            
            try:
                # 1. 获取数据集数据库ID
                cursor.execute('''
                    SELECT id FROM ask_data_datasets WHERE id = %s
                ''', (int(dataset_id),))
                
                result = cursor.fetchone()
                if not result:
                    self.logger.warning(f"数据集不存在: {dataset_id}")
                    return False
                
                dataset_db_id = result[0]
                
                # 2. 删除列配置
                cursor.execute('''
                    DELETE FROM ask_data_dataset_columns WHERE dataset_id = %s
                ''', (dataset_db_id,))
                
                conn.commit()
                self.logger.info(f"语义配置删除成功: {dataset_id}")
                return True
                
            except Exception as e:
                conn.rollback()
                self.logger.error(f"删除语义配置失败: {e}")
                raise
            finally:
                cursor.close()
                conn.close()

    def delete_dataset(self, dataset_id: str) -> bool:
        """
        删除数据集及其所有相关配置
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            bool: 是否删除成功
        """
        with LogContext(self.logger, f"删除数据集: {dataset_id}"):
            conn = self._get_connection()
            cursor = conn.cursor()
            
            try:
                # 删除数据集记录（级联删除会自动删除相关配置）
                cursor.execute('''
                    DELETE FROM ask_data_datasets WHERE id = %s
                ''', (int(dataset_id),))
                
                success = cursor.rowcount > 0
                conn.commit()
                
                if success:
                    self.logger.info(f"数据集删除成功: {dataset_id}")
                else:
                    self.logger.warning(f"数据集不存在: {dataset_id}")
                
                return success
                
            except Exception as e:
                conn.rollback()
                self.logger.error(f"删除数据集失败: {e}")
                raise
            finally:
                cursor.close()
                conn.close()



    # ====================== Transformations 管理方法 ======================
    
    def save_transformations(self, dataset_id: str, transformations: List[Dict[str, Any]]) -> bool:
        """
        保存数据集的transformations配置
        
        Args:
            dataset_id: 数据集ID
            transformations: transformations配置列表
            
        Returns:
            是否保存成功
        """
        conn = None
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 获取数据集的数据库ID
            cursor.execute('SELECT id FROM ask_data_datasets WHERE id = %s', (int(dataset_id),))
            result = cursor.fetchone()
            if not result:
                self.logger.error(f"数据集不存在: {dataset_id}")
                return False
                
            dataset_db_id = result['id']
            
            # 清理现有的transformations
            cursor.execute('DELETE FROM ask_data_dataset_transformations WHERE dataset_id = %s', (dataset_db_id,))
            
            # 保存新的transformations
            for i, transformation in enumerate(transformations):
                cursor.execute('''
                    INSERT INTO ask_data_dataset_transformations (
                        dataset_id, transformation_name, transformation_type, 
                        target_column, parameters, sort_order, is_enabled, description
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s, %s, %s
                    )
                ''', (
                    dataset_db_id,
                    transformation.get('name', f"transformation_{i+1}"),
                    transformation['type'],
                    transformation.get('params', {}).get('column'),
                    json.dumps(transformation.get('params', {})),
                    i,
                    transformation.get('enabled', True),
                    transformation.get('description', '')
                ))
            
            conn.commit()
            self.logger.info(f"✅ Transformations保存成功: {dataset_id}, 共{len(transformations)}个转换")
            return True
            
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 保存transformations失败: {e}")
            return False
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def get_transformations(self, dataset_id: str) -> List[Dict[str, Any]]:
        """
        获取数据集的transformations配置
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            transformations配置列表
        """
        conn = None
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 直接使用传入的dataset_id查询transformations
            cursor.execute('''
                SELECT 
                    id, dataset_id, transformation_name, transformation_type, target_column,
                    parameters, sort_order, is_enabled, description
                FROM ask_data_dataset_transformations
                WHERE dataset_id = %s
                ORDER BY sort_order
            ''', (dataset_id,))
            
            transformations = cursor.fetchall()
            
            result = []
            for trans in transformations:
                trans_dict = dict(trans)
                # 解析JSON参数 - 检查是否已经是dict类型
                params = {}
                if trans_dict['parameters']:
                    if isinstance(trans_dict['parameters'], dict):
                        # 如果已经是dict，直接使用
                        params = trans_dict['parameters']
                    elif isinstance(trans_dict['parameters'], str):
                        # 如果是字符串，则解析JSON
                        params = json.loads(trans_dict['parameters'])
                    else:
                        # 其他类型，尝试转换为dict
                        try:
                            params = dict(trans_dict['parameters'])
                        except:
                            params = {}
                
                result.append({
                    'id': trans_dict['id'],
                    'dataset_id': trans_dict['dataset_id'],
                    'name': trans_dict['transformation_name'],
                    'type': trans_dict['transformation_type'],
                    'params': params,
                    'enabled': trans_dict['is_enabled'],
                    'description': trans_dict.get('description', '')
                })
            
            return result
            
        except Exception as e:
            self.logger.error(f"获取transformations失败: {e}")
            return []
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def add_transformation(self, dataset_id: str, transformation: Dict[str, Any]) -> bool:
        """
        为数据集添加单个transformation
        
        Args:
            dataset_id: 数据集ID
            transformation: transformation配置
            
        Returns:
            是否添加成功
        """
        conn = None
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 获取数据集的数据库ID
            cursor.execute('SELECT id FROM ask_data_datasets WHERE id = %s', (int(dataset_id),))
            result = cursor.fetchone()
            if not result:
                self.logger.error(f"数据集不存在: {dataset_id}")
                return False
                
            dataset_db_id = result['id']
            
            # 获取下一个sort_order
            cursor.execute(
                'SELECT COALESCE(MAX(sort_order), -1) + 1 FROM ask_data_dataset_transformations WHERE dataset_id = %s',
                (dataset_db_id,)
            )
            next_order = cursor.fetchone()[0]
            
            # 添加transformation
            cursor.execute('''
                INSERT INTO ask_data_dataset_transformations (
                    dataset_id, transformation_name, transformation_type, 
                    target_column, parameters, sort_order, is_enabled, description
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s
                )
            ''', (
                dataset_db_id,
                transformation.get('name', f"transformation_{next_order+1}"),
                transformation['type'],
                transformation.get('params', {}).get('column'),
                json.dumps(transformation.get('params', {})),
                next_order,
                transformation.get('enabled', True),
                transformation.get('description', '')
            ))
            
            conn.commit()
            self.logger.info(f"✅ 添加transformation成功: {dataset_id}")
            return True
            
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 添加transformation失败: {e}")
            return False
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()

    def create_transformation(self, dataset_id: str, transformation: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        创建新的transformation并返回创建的记录
        
        Args:
            dataset_id: 数据集ID
            transformation: transformation配置
            
        Returns:
            创建的transformation记录，失败返回None
        """
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 获取数据集的数据库ID
            self.logger.info(f"正在创建transformation，数据集ID: {dataset_id}")
            cursor.execute('SELECT id FROM ask_data_datasets WHERE id = %s', (dataset_id,))
            result = cursor.fetchone()
            if not result:
                self.logger.error(f"数据集不存在: {dataset_id}")
                return None
                
            dataset_db_id = result['id']
            self.logger.info(f"找到数据集，数据库ID: {dataset_db_id}")
            
            # 获取下一个sort_order
            cursor.execute(
                'SELECT COALESCE(MAX(sort_order), -1) + 1 as next_order FROM ask_data_dataset_transformations WHERE dataset_id = %s',
                (dataset_db_id,)
            )
            result = cursor.fetchone()
            next_order = result['next_order'] if result else 0
            self.logger.info(f"计算得到的next_order: {next_order}")
            
            # 添加transformation并返回创建的记录
            transformation_name = transformation.get('name', f"transformation_{next_order+1}")
            transformation_type = transformation['type']
            # 优先使用target_column字段，如果没有则从params.column获取
            target_column = transformation.get('target_column') or transformation.get('params', {}).get('column')
            parameters_json = json.dumps(transformation.get('params', {}))
            enabled = transformation.get('enabled', True)
            description = transformation.get('description', '')
            
            self.logger.info(f"准备插入transformation: name={transformation_name}, type={transformation_type}, target_column={target_column}")
            self.logger.info(f"参数详情: params={transformation.get('params', {})}, enabled={enabled}, description={description}")
            
            cursor.execute('''
                INSERT INTO ask_data_dataset_transformations (
                    dataset_id, transformation_name, transformation_type, 
                    target_column, parameters, sort_order, is_enabled, description
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s, %s
                )
                RETURNING id, transformation_name, transformation_type, target_column,
                         parameters, sort_order, is_enabled, description, 
                         created_at, updated_at
            ''', (
                dataset_db_id,
                transformation_name,
                transformation_type,
                target_column,
                parameters_json,
                next_order,
                enabled,
                description
            ))
            
            created_record = cursor.fetchone()
            self.logger.info(f"插入操作完成，返回记录: {created_record}")
            conn.commit()
            
            if created_record:
                # 解析JSON参数
                params = {}
                if created_record['parameters']:
                    if isinstance(created_record['parameters'], dict):
                        params = created_record['parameters']
                    elif isinstance(created_record['parameters'], str):
                        params = json.loads(created_record['parameters'])
                    else:
                        try:
                            params = dict(created_record['parameters'])
                        except:
                            params = {}
                
                result_dict = {
                    'id': created_record['id'],
                    'dataset_id': dataset_id,
                    'name': created_record['transformation_name'],
                    'type': created_record['transformation_type'],
                    'target_column': created_record['target_column'],
                    'params': params,
                    'sort_order': created_record['sort_order'],
                    'enabled': created_record['is_enabled'],
                    'description': created_record['description'] or '',
                    'created_at': created_record['created_at'].isoformat() if created_record['created_at'] else None,
                    'updated_at': created_record['updated_at'].isoformat() if created_record['updated_at'] else None
                }
                
                self.logger.info(f"✅ 创建transformation成功: {dataset_id}/{result_dict['name']}")
                return result_dict
            else:
                return None
            
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 创建transformation失败: {e}")
            self.logger.error(f"错误类型: {type(e).__name__}")
            self.logger.error(f"错误详情: {str(e)}")
            import traceback
            self.logger.error(f"完整错误栈: {traceback.format_exc()}")
            return None
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def save_transformation(self, transformation: Dict[str, Any]) -> bool:
        """
        保存单个transformation配置
        
        Args:
            transformation: transformation配置，必须包含dataset_id
            
        Returns:
            是否保存成功
        """
        dataset_id = transformation.get('dataset_id')
        if not dataset_id:
            self.logger.error("transformation配置缺少dataset_id")
            return False
        
        return self.add_transformation(dataset_id, transformation)
    
    def delete_transformations_by_dataset(self, dataset_id: str) -> bool:
        """
        删除数据集的所有transformations配置
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            是否删除成功
        """
        conn = None
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 获取数据集的数据库ID
            cursor.execute('SELECT id FROM ask_data_datasets WHERE id = %s', (int(dataset_id),))
            result = cursor.fetchone()
            if not result:
                self.logger.error(f"数据集不存在: {dataset_id}")
                return False
                
            dataset_db_id = result['id']
            
            # 删除所有transformations
            cursor.execute('DELETE FROM ask_data_dataset_transformations WHERE dataset_id = %s', (dataset_db_id,))
            
            conn.commit()
            self.logger.info(f"✅ 删除数据集 {dataset_id} 的所有transformations成功")
            return True
            
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 删除transformations失败: {e}")
            return False
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def delete_transformation(self, dataset_id: str, transformation_name: str) -> bool:
        """
        删除指定的transformation
        
        Args:
            dataset_id: 数据集ID
            transformation_name: transformation名称
            
        Returns:
            是否删除成功
        """
        conn = None
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 获取数据集的数据库ID
            cursor.execute('SELECT id FROM ask_data_datasets WHERE id = %s', (int(dataset_id),))
            result = cursor.fetchone()
            if not result:
                self.logger.error(f"数据集不存在: {dataset_id}")
                return False
                
            dataset_db_id = result['id']
            
            # 删除transformation
            cursor.execute('''
                DELETE FROM ask_data_dataset_transformations 
                WHERE dataset_id = %s AND transformation_name = %s
            ''', (dataset_db_id, transformation_name))
            
            if cursor.rowcount > 0:
                conn.commit()
                self.logger.info(f"✅ 删除transformation成功: {dataset_id}/{transformation_name}")
                return True
            else:
                self.logger.warning(f"⚠️ Transformation不存在: {dataset_id}/{transformation_name}")
                return False
            
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 删除transformation失败: {e}")
            return False
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def enable_transformation(self, dataset_id: str, transformation_name: str, enabled: bool = True) -> bool:
        """
        启用或禁用指定的transformation
        
        Args:
            dataset_id: 数据集ID
            transformation_name: transformation名称
            enabled: 是否启用
            
        Returns:
            是否操作成功
        """
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('''
                UPDATE ask_data_dataset_transformations 
                SET is_enabled = %s, updated_at = %s
                WHERE dataset_id = %s AND transformation_name = %s
            ''', (enabled, datetime.now(), int(dataset_id), transformation_name))
            
            conn.commit()
            
            if cursor.rowcount > 0:
                self.logger.info(f"✅ 转换 {transformation_name} {'启用' if enabled else '禁用'}成功")
                return True
            else:
                self.logger.warning(f"⚠️ 未找到转换: {transformation_name}")
                return False
                
        except Exception as e:
            conn.rollback()
            self.logger.error(f"❌ 更新转换状态失败: {str(e)}")
            return False
        finally:
            cursor.close()
            conn.close()

    def toggle_transformation_by_id(self, transformation_id: int, enabled: bool) -> bool:
        """
        通过ID启用或禁用transformation
        
        Args:
            transformation_id: transformation的数据库ID
            enabled: 是否启用
            
        Returns:
            是否操作成功
        """
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            cursor.execute('''
                UPDATE ask_data_dataset_transformations 
                SET is_enabled = %s, updated_at = %s
                WHERE id = %s
            ''', (enabled, datetime.now(), transformation_id))
            
            conn.commit()
            
            if cursor.rowcount > 0:
                self.logger.info(f"✅ 转换配置 {transformation_id} {'启用' if enabled else '禁用'}成功")
                return True
            else:
                self.logger.warning(f"⚠️ 未找到转换配置: {transformation_id}")
                return False
                
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 更新转换配置状态失败: {str(e)}")
            return False
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()

    def get_transformation_by_id(self, transformation_id: int) -> Optional[Dict[str, Any]]:
        """
        通过ID获取单个transformation配置
        
        Args:
            transformation_id: transformation的数据库ID
            
        Returns:
            transformation配置信息，如果不存在则返回None
        """
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            cursor.execute('''
                SELECT 
                    id, dataset_id, transformation_name, transformation_type, target_column,
                    parameters, sort_order, is_enabled, description,
                    created_at, updated_at
                FROM ask_data_dataset_transformations
                WHERE id = %s
            ''', (transformation_id,))
            
            trans = cursor.fetchone()
            
            if not trans:
                return None
                
            trans_dict = dict(trans)
            
            # 解析JSON参数
            params = {}
            if trans_dict['parameters']:
                if isinstance(trans_dict['parameters'], dict):
                    params = trans_dict['parameters']
                elif isinstance(trans_dict['parameters'], str):
                    params = json.loads(trans_dict['parameters'])
                else:
                    try:
                        params = dict(trans_dict['parameters'])
                    except:
                        params = {}
            
            return {
                'id': trans_dict['id'],
                'dataset_id': trans_dict['dataset_id'],
                'name': trans_dict['transformation_name'],
                'type': trans_dict['transformation_type'],
                'target_column': trans_dict['target_column'],
                'params': params,
                'sort_order': trans_dict['sort_order'],
                'enabled': trans_dict['is_enabled'],
                'description': trans_dict.get('description', ''),
                'created_at': trans_dict['created_at'].isoformat() if trans_dict['created_at'] else None,
                'updated_at': trans_dict['updated_at'].isoformat() if trans_dict['updated_at'] else None
            }
            
        except Exception as e:
            self.logger.error(f"获取transformation失败: {e}")
            return None
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()

    def delete_transformation_by_id(self, transformation_id: int) -> bool:
        """
        通过ID删除transformation配置
        
        Args:
            transformation_id: transformation的数据库ID
            
        Returns:
            是否删除成功
        """
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            cursor.execute('''
                DELETE FROM ask_data_dataset_transformations 
                WHERE id = %s
            ''', (transformation_id,))
            
            conn.commit()
            
            if cursor.rowcount > 0:
                self.logger.info(f"✅ 删除转换配置 {transformation_id} 成功")
                return True
            else:
                self.logger.warning(f"⚠️ 未找到转换配置: {transformation_id}")
                return False
                
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 删除转换配置失败: {str(e)}")
            return False
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()

    # ====================== 列配置管理方法 ======================
    
    def create_column_config(self, dataset_id: str, column_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        创建列配置
        
        Args:
            dataset_id: 数据集ID
            column_data: 列配置数据
            
        Returns:
            创建的列配置信息
        """
        conn = self._get_connection()
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        
        try:
            # 直接使用传入的dataset_id作为外键
            
            cursor.execute('''
                INSERT INTO ask_data_dataset_columns 
                (dataset_id, name, type, description, alias, sort_order, is_required, default_value)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                RETURNING id, name, type, description, alias, sort_order, is_required, default_value, created_at, updated_at
            ''', (
                int(dataset_id),
                column_data['name'],
                column_data['type'],
                column_data.get('description', ''),
                column_data.get('alias'),
                column_data.get('sort_order', 0),
                column_data.get('is_required', False),
                column_data.get('default_value')
            ))
            
            result = cursor.fetchone()
            conn.commit()
            
            return {
                'id': result['id'],
                'dataset_id': dataset_id,
                'name': result['name'],
                'type': result['type'],
                'description': result['description'],
                'alias': result['alias'],
                'sort_order': result['sort_order'],
                'is_required': result['is_required'],
                'default_value': result['default_value'],
                'created_at': result['created_at'].isoformat() if result['created_at'] else None,
                'updated_at': result['updated_at'].isoformat() if result['updated_at'] else None
            }
            
        except Exception as e:
            conn.rollback()
            self.logger.error(f"创建列配置失败: {str(e)}")
            return None
        finally:
            cursor.close()
            conn.close()
    
    def get_column_config(self, column_id: int) -> Optional[Dict[str, Any]]:
        """
        获取列配置详情
        
        Args:
            column_id: 列配置ID
            
        Returns:
            列配置信息
        """
        conn = self._get_connection()
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        
        try:
            cursor.execute('''
                SELECT c.*, c.dataset_id
                FROM ask_data_dataset_columns c
                WHERE c.id = %s
            ''', (column_id,))
            
            result = cursor.fetchone()
            if not result:
                return None
            
            return {
                'id': result['id'],
                'dataset_id': result['dataset_id'],
                'name': result['name'],
                'type': result['type'],
                'description': result['description'],
                'alias': result['alias'],
                'sort_order': result['sort_order'],
                'is_required': result['is_required'],
                'default_value': result['default_value'],
                'created_at': result['created_at'].isoformat() if result['created_at'] else None,
                'updated_at': result['updated_at'].isoformat() if result['updated_at'] else None
            }
            
        except Exception as e:
            self.logger.error(f"获取列配置失败: {str(e)}")
            return None
        finally:
            cursor.close()
            conn.close()
    
    def update_column_config(self, column_id: int, updates: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        更新列配置
        
        Args:
            column_id: 列配置ID
            updates: 更新数据
            
        Returns:
            更新后的列配置信息
        """
        conn = self._get_connection()
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        
        try:
            # 构建更新语句
            update_fields = []
            update_values = []
            
            for field, value in updates.items():
                if field in ['name', 'type', 'description', 'alias', 'sort_order', 'is_required', 'default_value']:
                    update_fields.append(f"{field} = %s")
                    update_values.append(value)
            
            if not update_fields:
                return self.get_column_config(column_id)
            
            update_fields.append("updated_at = %s")
            update_values.append(datetime.now())
            update_values.append(column_id)
            
            cursor.execute(f'''
                UPDATE ask_data_dataset_columns 
                SET {', '.join(update_fields)}
                WHERE id = %s
            ''', update_values)
            
            conn.commit()
            
            if cursor.rowcount > 0:
                return self.get_column_config(column_id)
            else:
                return None
                
        except Exception as e:
            conn.rollback()
            self.logger.error(f"更新列配置失败: {str(e)}")
            return None
        finally:
            cursor.close()
            conn.close()
    
    def delete_column_config(self, column_id: int) -> bool:
        """
        删除列配置
        
        Args:
            column_id: 列配置ID
            
        Returns:
            是否删除成功
        """
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('DELETE FROM ask_data_dataset_columns WHERE id = %s', (column_id,))
            conn.commit()
            
            return cursor.rowcount > 0
            
        except Exception as e:
            conn.rollback()
            self.logger.error(f"删除列配置失败: {str(e)}")
            return False
        finally:
            cursor.close()
            conn.close()
    
    def list_dataset_columns(self, dataset_id: str) -> List[Dict[str, Any]]:
        """
        获取数据集的所有列配置
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            列配置列表
        """
        conn = self._get_connection()
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        
        try:
            # 直接使用传入的dataset_id查询列配置
            cursor.execute('''
                SELECT * FROM ask_data_dataset_columns
                WHERE dataset_id = %s
                ORDER BY sort_order, name
            ''', (int(dataset_id),))
            
            columns = []
            for row in cursor.fetchall():
                columns.append({
                    'id': row['id'],
                    'dataset_id': dataset_id,  # 返回传入的dataset_id
                    'name': row['name'],
                    'type': row['type'],
                    'description': row['description'],
                    'alias': row['alias'],
                    'sort_order': row['sort_order'],
                    'is_required': row['is_required'],
                    'default_value': row['default_value'],
                    'created_at': row['created_at'].isoformat() if row['created_at'] else None,
                    'updated_at': row['updated_at'].isoformat() if row['updated_at'] else None
                })
            
            return columns
            
        except Exception as e:
            self.logger.error(f"获取数据集列配置失败: {str(e)}")
            return []
        finally:
            cursor.close()
            conn.close()

    # ====================== 树节点管理方法 ======================
    
    def _init_tree_node_table(self):
        """初始化树节点表"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            self._init_tree_node_table_in_connection(cursor, conn)
        finally:
            cursor.close()
            conn.close()
    
    def _init_tree_node_table_in_connection(self, cursor, conn):
        """在现有连接中初始化树节点表（仅在表不存在时创建）"""
        try:
            with LogContext(self.logger, "检查并创建树节点表结构"):
                # 检查表是否已存在
                cursor.execute("""
                    SELECT EXISTS (
                        SELECT FROM information_schema.tables 
                        WHERE table_name = 'ask_data_dataset_tree_node'
                    )
                """)
                
                table_exists = cursor.fetchone()[0]
                if table_exists:
                    self.logger.info("树节点表已存在，跳过创建")
                    return
                
                self.logger.info("正在创建树节点表...")
                cursor.execute('''
                    CREATE TABLE IF NOT EXISTS ask_data_dataset_tree_node (
                        id VARCHAR NOT NULL PRIMARY KEY,
                        name VARCHAR,
                        description VARCHAR,
                        pid VARCHAR,
                        level INTEGER,
                        sort_order INTEGER DEFAULT 0,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                ''')
                
                # 创建索引
                indexes = [
                    'CREATE INDEX IF NOT EXISTS ask_data_dataset_tree_node_sort_order_index ON ask_data_dataset_tree_node (sort_order)',
                    'CREATE INDEX IF NOT EXISTS ask_data_dataset_tree_node_pid_index ON ask_data_dataset_tree_node (pid)'
                ]
                
                for index in indexes:
                    cursor.execute(index)
                
                conn.commit()
                self.logger.info("✅ 树节点表结构初始化完成")
                
        except Exception as e:
            conn.rollback()
            self.logger.error(f"❌ 树节点表初始化失败: {str(e)}")
            raise
    
    def create_tree_node(self, name: str, description: str = None, pid: str = None, sort_order: int = 0) -> Optional[str]:
        """
        创建树节点
        
        Args:
            name: 节点名称
            description: 节点描述
            pid: 父节点ID
            sort_order: 排序顺序
            
        Returns:
            创建的节点ID，失败返回None
        """
        import uuid
        
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 生成节点ID
            node_id = str(uuid.uuid4()).replace('-', '')
            
            # 计算层级
            level = 1
            if pid and pid != "0":
                cursor.execute('SELECT level FROM ask_data_dataset_tree_node WHERE id = %s', (pid,))
                parent = cursor.fetchone()
                if parent:
                    level = parent['level'] + 1
                else:
                    self.logger.warning(f"父节点不存在: {pid}")
                    return None
            
            # 插入节点
            cursor.execute('''
                INSERT INTO ask_data_dataset_tree_node 
                (id, name, description, pid, level, sort_order, create_time, update_time)
                VALUES (%s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ''', (node_id, name, description, pid, level, sort_order))
            
            conn.commit()
            self.logger.info(f"✅ 创建树节点成功: {node_id} - {name}")
            return node_id
            
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 创建树节点失败: {e}")
            return None
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def get_tree_node(self, node_id: str) -> Optional[Dict[str, Any]]:
        """
        获取单个树节点
        
        Args:
            node_id: 节点ID
            
        Returns:
            节点信息字典，如果不存在返回None
        """
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            cursor.execute('''
                SELECT id, name, description, pid, level, sort_order, 
                       create_time, update_time
                FROM ask_data_dataset_tree_node 
                WHERE id = %s
            ''', (node_id,))
            
            result = cursor.fetchone()
            if result:
                node = dict(result)
                # 转换时间格式
                if node['create_time']:
                    node['create_time'] = node['create_time'].isoformat()
                if node['update_time']:
                    node['update_time'] = node['update_time'].isoformat()
                return node
            else:
                return None
                
        except Exception as e:
            self.logger.error(f"获取树节点失败: {e}")
            return None
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def update_tree_node(self, node_id: str, name: str = None, description: str = None, sort_order: int = None) -> bool:
        """
        更新树节点
        
        Args:
            node_id: 节点ID
            name: 新的节点名称
            description: 新的节点描述
            sort_order: 新的排序顺序
            
        Returns:
            是否更新成功
        """
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            # 构建更新字段
            update_fields = []
            values = []
            
            if name is not None:
                update_fields.append("name = %s")
                values.append(name)
            if description is not None:
                update_fields.append("description = %s")
                values.append(description)
            if sort_order is not None:
                update_fields.append("sort_order = %s")
                values.append(sort_order)
            
            if not update_fields:
                return True  # 没有需要更新的字段
            
            # 添加更新时间
            update_fields.append("update_time = CURRENT_TIMESTAMP")
            values.append(node_id)
            
            sql = f"""
                UPDATE ask_data_dataset_tree_node 
                SET {', '.join(update_fields)}
                WHERE id = %s
            """
            
            cursor.execute(sql, values)
            rows_affected = cursor.rowcount
            
            conn.commit()
            
            if rows_affected > 0:
                self.logger.info(f"✅ 更新树节点成功: {node_id}")
                return True
            else:
                self.logger.warning(f"⚠️ 树节点不存在: {node_id}")
                return False
                
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 更新树节点失败: {e}")
            return False
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def delete_tree_node(self, node_id: str) -> bool:
        """
        删除树节点（级联删除子节点）
        
        Args:
            node_id: 节点ID
            
        Returns:
            是否删除成功
        """
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor()
            
            # 递归删除子节点
            def delete_node_and_children(current_id):
                # 查找所有子节点
                cursor.execute(
                    'SELECT id FROM ask_data_dataset_tree_node WHERE pid = %s',
                    (current_id,)
                )
                children = cursor.fetchall()
                
                # 递归删除子节点
                for child in children:
                    delete_node_and_children(child[0])
                
                # 删除当前节点
                cursor.execute(
                    'DELETE FROM ask_data_dataset_tree_node WHERE id = %s',
                    (current_id,)
                )
                self.logger.info(f"删除节点: {current_id}")
            
            # 开始递归删除
            delete_node_and_children(node_id)
            
            conn.commit()
            self.logger.info(f"✅ 删除树节点成功: {node_id}")
            return True
            
        except Exception as e:
            if conn:
                conn.rollback()
            self.logger.error(f"❌ 删除树节点失败: {e}")
            return False
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
    
    def get_node_and_children_ids(self, node_id: str) -> List[str]:
        """
        获取指定节点及其所有子节点的ID列表（非递归实现，性能优化）
        
        Args:
            node_id: 节点ID
            
        Returns:
            包含指定节点及其所有子节点的ID列表
        """
        if not node_id or node_id == "0":
            return []  # 返回空列表表示查询所有
        
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 获取所有节点的父子关系
            cursor.execute('''
                SELECT id, pid FROM ask_data_dataset_tree_node 
                ORDER BY level
            ''')
            
            all_nodes = cursor.fetchall()
            
            # 构建父子关系映射
            children_map = {}
            for node in all_nodes:
                parent_id = node['pid']
                if parent_id not in children_map:
                    children_map[parent_id] = []
                children_map[parent_id].append(node['id'])
            
            # 使用队列进行广度优先搜索，收集所有子节点ID
            result_ids = [node_id]  # 包含自身
            queue = [node_id]
            
            while queue:
                current_id = queue.pop(0)
                if current_id in children_map:
                    for child_id in children_map[current_id]:
                        result_ids.append(child_id)
                        queue.append(child_id)
            
            self.logger.debug(f"节点 {node_id} 及其子节点ID列表: {result_ids}")
            return result_ids
            
        except Exception as e:
            self.logger.error(f"获取节点及子节点ID失败: {e}")
            return [node_id]  # 出错时至少返回自身ID
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()

    def get_all_tree_nodes(self) -> Dict[str, Any]:
        """
        获取所有树节点，构建层级结构
        
        Returns:
            树形结构的节点数据
        """
        conn = None
        cursor = None
        
        try:
            conn = self._get_connection()
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            cursor.execute('''
                SELECT id, name, description, pid, level, sort_order,
                       create_time, update_time
                FROM ask_data_dataset_tree_node 
                ORDER BY level, sort_order, name
            ''')
            
            all_nodes = cursor.fetchall()
            
            # 转换为字典格式
            nodes_dict = {}
            for node in all_nodes:
                node_dict = dict(node)
                # 转换时间格式
                if node_dict['create_time']:
                    node_dict['create_time'] = node_dict['create_time'].isoformat()
                if node_dict['update_time']:
                    node_dict['update_time'] = node_dict['update_time'].isoformat()
                node_dict['children'] = []
                nodes_dict[node_dict['id']] = node_dict
            
            # 构建树形结构
            def build_tree():
                # 创建虚拟根节点
                root = {
                    "id": "0",
                    "name": "全部",
                    "sortOrder": None,
                    "description": "全部",
                    "children": []
                }
                
                # 递归构建子树
                def add_children(parent_formatted):
                    parent_id = parent_formatted['id']
                    for node in nodes_dict.values():
                        # 对于根节点(id="0")，查找pid为None或"0"的节点
                        # 对于其他节点，查找pid等于当前节点id的节点
                        if ((parent_id == "0" and (node['pid'] is None or node['pid'] == "0")) or 
                            (parent_id != "0" and node['pid'] == parent_id)):
                            formatted_child = {
                                "id": node['id'],
                                "name": node['name'],
                                "sortOrder": node['sort_order'],
                                "description": node['description'] or "",
                                "children": []
                            }
                            parent_formatted['children'].append(formatted_child)
                            add_children(formatted_child)  # 递归添加子节点的子节点
                
                # 为根节点添加所有子节点
                add_children(root)
                return root
            
            tree = build_tree()
            return tree
            
        except Exception as e:
            self.logger.error(f"获取树节点失败: {e}")
            return {
                "id": "0",
                "name": "全部",
                "sortOrder": None,
                "description": "全部",
                "children": []
            }
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close() 