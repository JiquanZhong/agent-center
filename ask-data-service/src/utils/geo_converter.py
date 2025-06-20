"""
地理数据转换工具

提供SHP文件到CSV文件的转换功能，提取非空间属性数据用于数据问答
"""

import os
import pandas as pd
import geopandas as gpd
from typing import Optional, Dict, Any, List
from .logger import get_logger, LogContext

logger = get_logger(__name__)

class GeoConverter:
    """地理数据转换器"""
    
    @staticmethod
    def shp_to_csv(shp_file_path: str, output_csv_path: str = None, 
                   include_geometry_info: bool = True, 
                   geometry_columns: List[str] = None) -> str:
        """
        将SHP文件转换为CSV文件
        
        Args:
            shp_file_path: SHP文件路径
            output_csv_path: 输出CSV文件路径，如果为None则自动生成
            include_geometry_info: 是否包含几何信息（如坐标、面积等）
            geometry_columns: 要提取的几何信息列名列表
            
        Returns:
            str: 输出CSV文件路径
        """
        with LogContext(logger, f"转换SHP文件: {os.path.basename(shp_file_path)}"):
            try:
                # 读取SHP文件
                gdf = gpd.read_file(shp_file_path, encoding='utf-8')
                logger.info(f"📊 读取SHP文件成功: {len(gdf)} 个要素, {len(gdf.columns)} 个字段")
                
                # 创建DataFrame副本用于转换
                df = gdf.copy()
                
                # 处理几何信息
                if include_geometry_info and 'geometry' in df.columns:
                    df = GeoConverter._extract_geometry_info(df, geometry_columns)
                
                # 移除geometry列（因为CSV不支持几何对象）
                if 'geometry' in df.columns:
                    df = df.drop(columns=['geometry'])
                    logger.info("✂️ 已移除geometry列")
                
                # 处理数据类型
                df = GeoConverter._clean_dataframe(df)
                
                # 生成输出路径
                if output_csv_path is None:
                    base_name = os.path.splitext(os.path.basename(shp_file_path))[0]
                    output_dir = os.path.dirname(shp_file_path)
                    output_csv_path = os.path.join(output_dir, f"{base_name}_converted.csv")
                
                # 保存为CSV
                df.to_csv(output_csv_path, index=False, encoding='utf-8-sig')
                logger.info(f"💾 CSV文件保存成功: {output_csv_path}")
                logger.info(f"📋 输出数据: {len(df)} 行, {len(df.columns)} 列")
                
                return output_csv_path
                
            except Exception as e:
                logger.error(f"❌ SHP转CSV失败: {str(e)}")
                raise
    
    @staticmethod
    def _extract_geometry_info(df: gpd.GeoDataFrame, geometry_columns: List[str] = None) -> pd.DataFrame:
        """
        从几何对象中提取有用的信息
        
        Args:
            df: 包含几何信息的GeoDataFrame
            geometry_columns: 要提取的几何信息列名
            
        Returns:
            pd.DataFrame: 添加了几何信息的DataFrame
        """
        logger.info("🔍 提取几何信息...")
        
        try:
            # 默认要提取的几何信息
            default_columns = ['centroid_x', 'centroid_y', 'area', 'perimeter', 'bounds']
            columns_to_extract = geometry_columns or default_columns
            
            geometry = df.geometry
            
            # 提取质心坐标
            if 'centroid_x' in columns_to_extract or 'centroid_y' in columns_to_extract:
                centroids = geometry.centroid
                if 'centroid_x' in columns_to_extract:
                    df['centroid_x'] = centroids.x
                if 'centroid_y' in columns_to_extract:
                    df['centroid_y'] = centroids.y
                logger.debug("✅ 已提取质心坐标")
            
            # 提取面积（仅对面要素有效）
            if 'area' in columns_to_extract:
                try:
                    # 确保使用投影坐标系计算面积
                    if df.crs and df.crs.is_geographic:
                        # 如果是地理坐标系，转换为Web Mercator投影
                        df_projected = df.to_crs('EPSG:3857')
                        df['area'] = df_projected.geometry.area
                        logger.debug("✅ 已提取面积（使用投影坐标系）")
                    else:
                        df['area'] = geometry.area
                        logger.debug("✅ 已提取面积")
                except Exception as e:
                    logger.warning(f"⚠️ 提取面积失败: {e}")
            
            # 提取周长
            if 'perimeter' in columns_to_extract:
                try:
                    if df.crs and df.crs.is_geographic:
                        df_projected = df.to_crs('EPSG:3857')
                        df['perimeter'] = df_projected.geometry.length
                        logger.debug("✅ 已提取周长（使用投影坐标系）")
                    else:
                        df['perimeter'] = geometry.length
                        logger.debug("✅ 已提取周长")
                except Exception as e:
                    logger.warning(f"⚠️ 提取周长失败: {e}")
            
            # 提取边界框
            if 'bounds' in columns_to_extract:
                try:
                    bounds = geometry.bounds
                    df['min_x'] = bounds['minx']
                    df['min_y'] = bounds['miny']
                    df['max_x'] = bounds['maxx']
                    df['max_y'] = bounds['maxy']
                    logger.debug("✅ 已提取边界框")
                except Exception as e:
                    logger.warning(f"⚠️ 提取边界框失败: {e}")
            
            return df
            
        except Exception as e:
            logger.warning(f"⚠️ 几何信息提取失败: {e}")
            return df
    
    @staticmethod
    def _clean_dataframe(df: pd.DataFrame) -> pd.DataFrame:
        """
        清理DataFrame，处理数据类型和特殊值
        
        Args:
            df: 要清理的DataFrame
            
        Returns:
            pd.DataFrame: 清理后的DataFrame
        """
        logger.info("🧹 清理数据...")
        
        # 处理列名（移除特殊字符）
        df.columns = [col.replace(' ', '_').replace('-', '_').replace('.', '_') 
                     for col in df.columns]
        
        # 处理数据类型
        for col in df.columns:
            # 尝试转换为数值类型
            if df[col].dtype == 'object':
                # 尝试转换为数值
                try:
                    pd.to_numeric(df[col], errors='raise')
                    df[col] = pd.to_numeric(df[col], errors='coerce')
                except:
                    # 保持为字符串，但处理None值
                    df[col] = df[col].astype(str).replace('None', '')
        
        # 处理无穷大和NaN值
        df = df.replace([float('inf'), float('-inf')], None)
        
        logger.info("✅ 数据清理完成")
        return df
    
    @staticmethod
    def get_shp_info(shp_file_path: str) -> Dict[str, Any]:
        """
        获取SHP文件的基本信息
        
        Args:
            shp_file_path: SHP文件路径
            
        Returns:
            Dict: SHP文件信息
        """
        try:
            gdf = gpd.read_file(shp_file_path)
            
            info = {
                'features_count': len(gdf),
                'columns_count': len(gdf.columns),
                'columns': list(gdf.columns),
                'crs': str(gdf.crs) if gdf.crs else None,
                'geometry_type': gdf.geometry.geom_type.unique().tolist(),
                'bounds': gdf.total_bounds.tolist() if len(gdf) > 0 else None,
                'non_geometry_columns': [col for col in gdf.columns if col != 'geometry']
            }
            
            logger.info(f"📋 SHP文件信息: {info['features_count']} 个要素, {info['columns_count']} 个字段")
            return info
            
        except Exception as e:
            logger.error(f"❌ 获取SHP文件信息失败: {str(e)}")
            raise
    
    @staticmethod
    def validate_shp_file(shp_file_path: str) -> bool:
        """
        验证SHP文件是否有效
        
        Args:
            shp_file_path: SHP文件路径
            
        Returns:
            bool: 是否有效
        """
        try:
            # 检查文件是否存在
            if not os.path.exists(shp_file_path):
                logger.error(f"❌ SHP文件不存在: {shp_file_path}")
                return False
            
            # 检查相关文件是否存在（.shx, .dbf）
            base_path = os.path.splitext(shp_file_path)[0]
            required_files = ['.shx', '.dbf']
            
            for ext in required_files:
                file_path = base_path + ext
                if not os.path.exists(file_path):
                    logger.warning(f"⚠️ 缺少文件: {file_path}")
            
            # 尝试读取文件
            gdf = gpd.read_file(shp_file_path)
            
            if len(gdf) == 0:
                logger.warning("⚠️ SHP文件为空")
                return False
            
            logger.info(f"✅ SHP文件验证通过: {len(gdf)} 个要素")
            return True
            
        except Exception as e:
            logger.error(f"❌ SHP文件验证失败: {str(e)}")
            return False 