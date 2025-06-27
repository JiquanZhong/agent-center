"""
数据查看器模块

提供CSV文件的分页读取功能
"""

import pandas as pd
import os
from typing import Dict, List, Any, Tuple, Optional
from ..utils.logger import get_logger
from ..utils.schema_database import SchemaDatabase

class DataViewer:
    """数据查看器 - 提供CSV文件分页查看功能"""
    
    def __init__(self, db: SchemaDatabase = None):
        """
        初始化数据查看器
        
        Args:
            db: 数据库连接实例
        """
        self.logger = get_logger(__name__)
        self.db = db
    
    def get_paginated_data(self, dataset_id: str, page: int = 1, per_page: int = 20) -> Dict[str, Any]:
        """
        获取数据集的分页数据
        
        Args:
            dataset_id: 数据集ID
            page: 页码，从1开始
            per_page: 每页记录数
            
        Returns:
            包含分页数据的字典
        """
        try:
            # 1. 验证数据集是否存在
            dataset = self.db.get_dataset_by_id(dataset_id)
            if not dataset:
                raise ValueError(f"数据集 {dataset_id} 不存在")
            
            # 2. 获取文件路径
            file_path = self._get_actual_file_path(dataset)
            if not file_path or not os.path.exists(file_path):
                raise ValueError(f"数据文件不存在: {file_path}")
            
            self.logger.info(f"开始分页读取数据文件: {file_path}, 页码: {page}, 每页: {per_page}")
            
            # 3. 先读取CSV头部信息
            headers = self._get_csv_headers(file_path)
            
            # 4. 获取总行数（不包括表头）
            total_rows = self._get_total_rows(file_path)
            total_columns = len(headers)
            
            # 5. 计算分页参数
            total_pages = (total_rows + per_page - 1) // per_page
            start_row = (page - 1) * per_page
            
            # 6. 读取分页数据
            paginated_data = self._read_paginated_csv(file_path, start_row, per_page)
            
            # 7. 构建返回结果
            result = {
                "headers": headers,
                "data": paginated_data,
                "total_rows": total_rows,
                "total_columns": total_columns,
                "current_page": page,
                "per_page": per_page,
                "total_pages": total_pages
            }
            
            self.logger.info(f"分页数据获取成功 - 总行数: {total_rows}, 当前页: {page}/{total_pages}, 返回记录: {len(paginated_data)}")
            return result
            
        except Exception as e:
            self.logger.error(f"获取分页数据失败: {str(e)}")
            raise
    
    def _get_actual_file_path(self, dataset: Dict[str, Any]) -> str:
        """
        获取数据集的实际文件路径
        
        Args:
            dataset: 数据集信息
            
        Returns:
            实际的文件路径
        """
        # 优先使用实际数据路径，如果不存在则使用原始路径
        actual_data_path = dataset.get('actual_data_path')
        if actual_data_path and os.path.exists(actual_data_path):
            return actual_data_path
        
        file_path = dataset.get('file_path')
        if file_path and os.path.exists(file_path):
            return file_path
        
        # 如果都不存在，尝试在data目录中查找
        if file_path:
            filename = os.path.basename(file_path)
            data_dir_path = os.path.join("data", filename)
            if os.path.exists(data_dir_path):
                return data_dir_path
        
        return None
    
    def _get_csv_headers(self, file_path: str) -> List[str]:
        """
        获取CSV文件的列头
        
        Args:
            file_path: CSV文件路径
            
        Returns:
            列头列表
        """
        try:
            # 只读取第一行获取列头
            df_head = pd.read_csv(file_path, nrows=0)
            headers = df_head.columns.tolist()
            self.logger.debug(f"获取到CSV列头: {len(headers)}列")
            return headers
        except Exception as e:
            self.logger.error(f"读取CSV列头失败: {str(e)}")
            raise ValueError(f"无法读取CSV文件头部: {str(e)}")
    
    def _get_total_rows(self, file_path: str) -> int:
        """
        获取CSV文件的总行数（不包括表头）
        
        Args:
            file_path: CSV文件路径
            
        Returns:
            总行数
        """
        try:
            # 使用更高效的方式计算行数
            with open(file_path, 'r', encoding='utf-8') as f:
                total_lines = sum(1 for _ in f)
            
            # 减去表头行
            total_rows = total_lines - 1 if total_lines > 0 else 0
            self.logger.debug(f"CSV文件总行数: {total_rows}")
            return total_rows
        except Exception as e:
            self.logger.error(f"计算CSV行数失败: {str(e)}")
            raise ValueError(f"无法计算文件行数: {str(e)}")
    
    def _read_paginated_csv(self, file_path: str, start_row: int, per_page: int) -> List[List[Any]]:
        """
        读取CSV文件的分页数据
        
        Args:
            file_path: CSV文件路径
            start_row: 开始行号（从0开始，不包括表头）
            per_page: 每页记录数
            
        Returns:
            分页数据列表
        """
        try:
            # 使用pandas的skiprows和nrows参数实现分页读取
            # skiprows=start_row+1 是因为要跳过表头和前面的数据行
            df = pd.read_csv(
                file_path,
                skiprows=range(1, start_row + 1),  # 跳过表头后的前start_row行
                nrows=per_page  # 只读取per_page行
            )
            
            # 将DataFrame转换为列表格式，处理NaN值
            data = []
            for _, row in df.iterrows():
                row_data = []
                for value in row:
                    # 处理NaN值，转换为None或空字符串
                    if pd.isna(value):
                        row_data.append("")
                    else:
                        row_data.append(str(value))
                data.append(row_data)
            
            self.logger.debug(f"成功读取分页数据: {len(data)}行")
            return data
            
        except Exception as e:
            self.logger.error(f"读取分页CSV数据失败: {str(e)}")
            raise ValueError(f"无法读取CSV分页数据: {str(e)}")
    
    def get_data_preview(self, dataset_id: str, preview_rows: int = 5) -> Dict[str, Any]:
        """
        获取数据集的预览数据（前几行）
        
        Args:
            dataset_id: 数据集ID
            preview_rows: 预览行数，默认5行
            
        Returns:
            预览数据字典
        """
        try:
            return self.get_paginated_data(dataset_id, page=1, per_page=preview_rows)
        except Exception as e:
            self.logger.error(f"获取数据预览失败: {str(e)}")
            raise 