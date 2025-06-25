"""
字典映射管理器

处理extract transformations与字典映射的同步
"""

import json
from typing import Dict, List, Optional, Any
from src.utils.logger import get_logger
from src.utils.schema_database import SchemaDatabase
from src.core.dictionary_service import DictionaryService

class DictionaryMappingManager:
    """字典映射管理器"""
    
    def __init__(self, db: SchemaDatabase):
        self.db = db
        self.dictionary_service = DictionaryService()
        self.logger = get_logger(__name__)
    
    def update_column_dictionary_mapping(self, dataset_id: str, column_name: str, 
                                       dictionary_id: Optional[str]) -> bool:
        """
        更新列的字典映射
        
        Args:
            dataset_id: 数据集ID
            column_name: 列名
            dictionary_id: 字典ID
            
        Returns:
            是否更新成功
        """
        try:
            if not dictionary_id:
                self.logger.info(f"列 {column_name} 的字典ID为空，跳过字典映射更新")
                return True
            
            self.logger.info(f"🔄 开始更新列 {column_name} 的字典映射 (字典ID: {dictionary_id})")
            
            # 1. 获取字典数据
            dictionary_mapping = self.dictionary_service.get_dictionary_data(dictionary_id)
            if not dictionary_mapping:
                self.logger.error(f"❌ 无法获取字典数据 (字典ID: {dictionary_id})")
                return False
            
            # 2. 查找基于当前列的extract transformations
            extract_transformations = self._find_extract_transformations(dataset_id, column_name)
            
            if not extract_transformations:
                self.logger.info(f"未找到基于列 {column_name} 的extract transformations")
                return True
            
            # 3. 为每个extract transformation生成的新列更新映射
            for extract_transform in extract_transformations:
                new_column = self._get_new_column_from_extract(extract_transform)
                if new_column:
                    self._update_mapping_for_column(dataset_id, new_column, dictionary_mapping, 
                                                  extract_transform['sort_order'])
            
            self.logger.info(f"✅ 成功更新列 {column_name} 的字典映射")
            return True
            
        except Exception as e:
            self.logger.error(f"❌ 更新列字典映射失败: {str(e)}")
            return False
    
    def _find_extract_transformations(self, dataset_id: str, target_column: str) -> List[Dict[str, Any]]:
        """
        查找指定列的extract类型transformations
        
        Args:
            dataset_id: 数据集ID
            target_column: 目标列名
            
        Returns:
            extract transformations列表
        """
        try:
            all_transformations = self.db.get_transformations(dataset_id)
            
            extract_transformations = []
            for transform in all_transformations:
                if (transform.get('transformation_type') == 'extract' and 
                    transform.get('target_column') == target_column):
                    extract_transformations.append(transform)
            
            self.logger.info(f"找到 {len(extract_transformations)} 个基于列 {target_column} 的extract transformations")
            return extract_transformations
            
        except Exception as e:
            self.logger.error(f"查找extract transformations失败: {str(e)}")
            return []
    
    def _get_new_column_from_extract(self, extract_transform: Dict[str, Any]) -> Optional[str]:
        """
        从extract transformation中获取新列名
        
        Args:
            extract_transform: extract transformation配置
            
        Returns:
            新列名或None
        """
        try:
            parameters = extract_transform.get('parameters', {})
            if isinstance(parameters, str):
                parameters = json.loads(parameters)
            
            return parameters.get('new_column')
            
        except Exception as e:
            self.logger.error(f"解析extract transformation参数失败: {str(e)}")
            return None
    
    def _update_mapping_for_column(self, dataset_id: str, column_name: str, 
                                 dictionary_mapping: Dict[str, str], base_sort_order: int):
        """
        为指定列更新映射
        
        Args:
            dataset_id: 数据集ID
            column_name: 列名
            dictionary_mapping: 字典映射
            base_sort_order: 基础排序顺序
        """
        try:
            # 1. 删除现有的map_values transformations
            self._delete_existing_mapping_transformations(dataset_id, column_name)
            
            # 2. 创建新的映射transformation
            target_column = self.dictionary_service.generate_mapping_column_name(column_name)
            
            mapping_transform = self.dictionary_service.create_mapping_transformation(
                dataset_id=dataset_id,
                source_column=column_name,
                target_column=target_column,
                mapping=dictionary_mapping,
                sort_order=base_sort_order + 100  # 确保在extract之后执行
            )
            
            # 3. 保存新的mapping transformation
            result = self.db.create_transformation(dataset_id, mapping_transform)
            
            if result:
                self.logger.info(f"✅ 成功创建列 {column_name} 的映射transformation -> {target_column}")
            else:
                self.logger.error(f"❌ 创建列 {column_name} 的映射transformation失败")
                
        except Exception as e:
            self.logger.error(f"更新列 {column_name} 的映射失败: {str(e)}")
    
    def _delete_existing_mapping_transformations(self, dataset_id: str, source_column: str):
        """
        删除现有的映射transformations
        
        Args:
            dataset_id: 数据集ID
            source_column: 源列名
        """
        try:
            all_transformations = self.db.get_transformations(dataset_id)
            
            for transform in all_transformations:
                if (transform.get('transformation_type') == 'map_values' and 
                    transform.get('target_column') == source_column):
                    
                    transform_id = transform.get('id')
                    if transform_id:
                        success = self.db.delete_transformation_by_id(transform_id)
                        if success:
                            self.logger.info(f"✅ 删除现有映射transformation: {transform.get('transformation_name')}")
                        else:
                            self.logger.error(f"❌ 删除映射transformation失败: {transform.get('transformation_name')}")
                            
        except Exception as e:
            self.logger.error(f"删除现有映射transformations失败: {str(e)}")
    
    def sync_all_dictionary_mappings(self, dataset_id: str) -> bool:
        """
        同步数据集中所有列的字典映射
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            是否同步成功
        """
        try:
            self.logger.info(f"🔄 开始同步数据集 {dataset_id} 的所有字典映射")
            
            # 获取数据集的所有列配置
            columns = self.db.list_dataset_columns(dataset_id)
            
            success_count = 0
            total_count = 0
            
            for column in columns:
                column_name = column.get('name')
                dictionary_id = column.get('dictionary_id')
                
                if dictionary_id:
                    total_count += 1
                    if self.update_column_dictionary_mapping(dataset_id, column_name, dictionary_id):
                        success_count += 1
            
            self.logger.info(f"✅ 字典映射同步完成: {success_count}/{total_count} 成功")
            return success_count == total_count
            
        except Exception as e:
            self.logger.error(f"❌ 同步所有字典映射失败: {str(e)}")
            return False 