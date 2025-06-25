"""
字典服务

处理字典API调用和数据转换
"""

import os
import json
import requests
from typing import Dict, List, Optional, Any
from src.utils.logger import get_logger

class DictionaryService:
    """字典服务类"""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        # 从环境变量获取配置
        self.docker_host_ip = os.getenv('DOCKER_HOST_IP', 'localhost')
        self.operation_backend_port = os.getenv('OPERATION_BACKEND_PORT', '8080')
        self.base_url = f"http://{self.docker_host_ip}:{self.operation_backend_port}"
    
    def get_dictionary_data(self, dic_id: str) -> Optional[Dict[str, str]]:
        """
        根据字典ID获取字典数据
        
        Args:
            dic_id: 字典ID
            
        Returns:
            字典映射 {code: name} 或 None
        """
        try:
            url = f"{self.base_url}/diit-system/dicEntry/getByDicId"
            params = {'dicId': dic_id}
            
            self.logger.info(f"🔍 调用字典API: {url}, 参数: {params}")
            
            response = requests.get(url, params=params, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            
            if not data.get('success', False):
                self.logger.error(f"❌ 字典API调用失败: {data.get('msg', '未知错误')}")
                return None
            
            # 提取字典数据
            dictionary_entries = data.get('data', [])
            mapping = {}
            
            for entry in dictionary_entries:
                code = entry.get('code')
                name = entry.get('name')
                if code and name:
                    mapping[code] = name
            
            self.logger.info(f"✅ 成功获取字典数据，共 {len(mapping)} 条记录")
            return mapping
            
        except requests.exceptions.RequestException as e:
            self.logger.error(f"❌ 字典API请求失败: {str(e)}")
            return None
        except Exception as e:
            self.logger.error(f"❌ 处理字典数据失败: {str(e)}")
            return None
    
    def create_mapping_transformation(self, dataset_id: str, source_column: str, 
                                    target_column: str, mapping: Dict[str, str], 
                                    sort_order: int = 0) -> Dict[str, Any]:
        """
        创建映射类型的transformation配置
        
        Args:
            dataset_id: 数据集ID
            source_column: 源列名
            target_column: 目标列名
            mapping: 映射字典
            sort_order: 排序顺序
            
        Returns:
            transformation配置
        """
        return {
            'dataset_id': dataset_id,
            'transformation_name': f"{source_column}映射",
            'transformation_type': 'map_values',
            'target_column': source_column,
            'parameters': {
                'column': source_column,
                'mapping': mapping,
                'new_column': target_column
            },
            'sort_order': sort_order,
            'is_enabled': True,
            'description': f"将{source_column}代码映射为名称"
        }
    
    def generate_level_column_name(self, base_column: str, level: str) -> str:
        """
        根据级别生成列名
        
        Args:
            base_column: 基础列名
            level: 级别（省级、市级、县级等）
            
        Returns:
            生成的列名
        """
        return f"{level}代码"
    
    def generate_mapping_column_name(self, level_column: str) -> str:
        """
        根据代码列名生成映射后的名称列名
        
        Args:
            level_column: 代码列名（如：省级代码）
            
        Returns:
            映射后的列名（如：省级名称）
        """
        if level_column.endswith('代码'):
            return level_column.replace('代码', '名称')
        return f"{level_column}名称" 