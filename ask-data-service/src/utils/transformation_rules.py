"""
转换规则管理模块

根据数据集中的字段动态生成transformations配置
"""

import json
import os
import pandas as pd
from typing import Dict, List, Any, Optional
from ..utils.logger import get_logger

class TransformationRuleManager:
    """转换规则管理器"""
    
    def __init__(self, rules_config_path: str = None):
        """
        初始化转换规则管理器
        
        Args:
            rules_config_path: 规则配置文件路径，如果为None则使用默认路径
        """
        self.logger = get_logger(__name__)
        
        # 设置默认配置文件路径
        if rules_config_path is None:
            # current_dir = os.path.dirname(__file__)
            # rules_config_path = os.path.join(current_dir, "transformation_rules.json")
            rules_config_path = "./json_config/transformation_rules.json"
        
        self.rules_config_path = rules_config_path
        self.rules = self._load_rules()
    
    def _load_rules(self) -> Dict[str, Any]:
        """从JSON文件加载转换规则配置"""
        try:
            if os.path.exists(self.rules_config_path):
                with open(self.rules_config_path, 'r', encoding='utf-8') as f:
                    rules = json.load(f)
                self.logger.info(f"✅ 已加载转换规则配置: {self.rules_config_path}")
                return rules
            else:
                self.logger.warning(f"⚠️ 规则配置文件不存在: {self.rules_config_path}，使用默认配置")
                return self._get_default_rules()
        except Exception as e:
            self.logger.error(f"❌ 加载规则配置失败: {e}，使用默认配置")
            return self._get_default_rules()
    
    def _get_default_rules(self) -> Dict[str, Any]:
        """获取默认的转换规则配置"""
        return {
            "field_conditions": {
                "ZLDWDM": {
                    "description": "坐落单位代码字段相关的转换规则",
                    "transformations": [
                        {
                            "name": "提取省级代码",
                            "type": "extract",
                            "params": {
                                "column": "ZLDWDM",
                                "pattern": "^(\\d{2})",
                                "new_column": "省级代码"
                            },
                            "sort_order": 0,
                            "enabled": True,
                            "description": "从ZLDWDM（坐落单位代码）中提取前2位作为省级行政区划代码"
                        },
                        {
                            "name": "提取市级代码",
                            "type": "extract",
                            "params": {
                                "column": "ZLDWDM",
                                "pattern": "^(\\d{4})",
                                "new_column": "市级代码"
                            },
                            "sort_order": 0,
                            "enabled": True,
                            "description": "从ZLDWDM（坐落单位代码）中提取前4位作为市级行政区划代码"
                        },
                        {
                            "name": "提取县级代码",
                            "type": "extract",
                            "params": {
                                "column": "ZLDWDM",
                                "pattern": "^(\\d{6})",
                                "new_column": "县级代码"
                            },
                            "sort_order": 0,
                            "enabled": True,
                            "description": "从ZLDWDM（坐落单位代码）中提取前6位作为县级行政区划代码"
                        },
                        {
                            "name": "县级代码映射",
                            "type": "map_values",
                            "params": {
                                "column": "县级代码",
                                "mapping": {
                                    "360702": "章贡区",
                                    "360703": "南康区", 
                                    "360704": "赣县区",
                                    "360722": "信丰县",
                                    "360723": "大余县",
                                    "360724": "上犹县",
                                    "360725": "崇义县",
                                    "360726": "安远县"
                                },
                                "new_column": "行政区名称"
                            },
                            "sort_order": 1,
                            "enabled": True,
                            "description": "将县级行政区划代码映射为区县名称"
                        }
                    ]
                },
                "DLBM": {
                    "description": "地类编码字段相关的转换规则",
                    "transformations": [
                        {
                            "name": "提取大类编码",
                            "type": "extract",
                            "params": {
                                "column": "DLBM",
                                "pattern": "^(\\d{2})",
                                "new_column": "大类编码"
                            },
                            "sort_order": 2,
                            "enabled": True,
                            "description": "从DLBM（地类编码）中提取前2位作为大类编码"
                        },
                        {
                            "name": "大类编码映射",
                            "type": "map_values",
                            "params": {
                                "column": "大类编码",
                                "mapping": {
                                    "01": "耕地",
                                    "02": "园地",
                                    "03": "林地",
                                    "04": "草地",
                                    "10": "交通运输用地",
                                    "11": "水域",
                                    "12": "其他类型土地",
                                    "20": "城镇村及工矿用地",
                                    "91": "其他类型农用地",
                                    "92": "水工建筑用地"
                                },
                                "new_column": "一级地类名称"
                            },
                            "sort_order": 3,
                            "enabled": True,
                            "description": "将大类编码映射为一级地类名称"
                        },
                        {
                            "name": "详细地类编码映射",
                            "type": "map_values",
                            "params": {
                                "column": "DLBM",
                                "mapping": {
                                    "201": "城市",
                                    "202": "建制镇",
                                    "203": "村庄",
                                    "204": "盐田及采矿用地",
                                    "205": "特殊用地",
                                    "0101": "水田",
                                    "0102": "水浇地",
                                    "0103": "旱地",
                                    "0201": "果园",
                                    "0202": "茶园",
                                    "0203": "橡胶园",
                                    "0204": "其他园地",
                                    "0301": "乔木林地",
                                    "0302": "竹林地",
                                    "0303": "红树林地",
                                    "0304": "森林沼泽",
                                    "0305": "灌木林地",
                                    "0306": "灌丛沼泽",
                                    "0307": "其他林地",
                                    "0401": "天然牧草地",
                                    "0402": "沼泽草地",
                                    "0403": "人工牧草地",
                                    "0404": "其他草地",
                                    "1001": "铁路用地",
                                    "1002": "轨道交通用地",
                                    "1003": "公路用地",
                                    "1006": "农村道路",
                                    "1007": "机场用地",
                                    "1008": "港口码头用地",
                                    "1009": "管道运输用地",
                                    "1101": "河流水面",
                                    "1102": "湖泊水面",
                                    "1103": "水库水面",
                                    "1104": "坑塘水面",
                                    "1105": "沿海滩涂",
                                    "1106": "内陆滩涂",
                                    "1107": "沟渠",
                                    "1108": "沼泽地",
                                    "1109": "水工建筑用地",
                                    "1110": "冰川及永久积雪",
                                    "1202": "设施农用地",
                                    "1203": "田坎",
                                    "1204": "盐碱地",
                                    "1205": "沙地",
                                    "1206": "裸土地",
                                    "1207": "裸岩石砾地",
                                    "201A": "城市独立工业用地",
                                    "202A": "建制镇独立工业用地",
                                    "203A": "村庄独立工业用地",
                                    "1104A": "养殖坑塘",
                                    "1107A": "干渠"
                                },
                                "new_column": "地类名称"
                            },
                            "sort_order": 5,
                            "enabled": True,
                            "description": "将详细地类编码转换为具体地类名称"
                        }
                    ]
                },
                "TBMJ": {
                    "description": "图斑面积字段相关的转换规则",
                    "transformations": [
                        {
                            "name": "标准化面积数据",
                            "type": "to_numeric",
                            "params": {
                                "column": "TBMJ",
                                "errors": "coerce"
                            },
                            "sort_order": 4,
                            "enabled": True,
                            "description": "确保图斑面积字段为数值类型"
                        },
                        {
                            "name": "四舍五入面积",
                            "type": "round_numbers",
                            "params": {
                                "column": "TBMJ",
                                "decimals": 2
                            },
                            "sort_order": 6,
                            "enabled": True,
                            "description": "将面积保留2位小数"
                        }
                    ]
                }
            },
            "common_transformations": {
                "description": "通用的转换规则，适用于所有数据集",
                "transformations": []
            }
        }
    
    def generate_transformations_for_dataframe(self, df: pd.DataFrame) -> List[Dict[str, Any]]:
        """
        根据DataFrame的字段动态生成转换规则
        
        Args:
            df: pandas DataFrame
            
        Returns:
            生成的转换规则列表
        """
        transformations = []
        df_columns = list(df.columns)
        
        self.logger.info(f"🔍 分析数据字段: {df_columns}")
        
        # 根据字段条件生成转换规则
        for field_name, field_config in self.rules.get("field_conditions", {}).items():
            if field_name in df_columns:
                field_transformations = field_config.get("transformations", [])
                transformations.extend(field_transformations)
                self.logger.info(f"✅ 字段 '{field_name}' 匹配，添加了 {len(field_transformations)} 个转换规则")
            else:
                self.logger.debug(f"⏭️ 字段 '{field_name}' 不存在，跳过相关转换规则")
        
        # 添加通用转换规则
        common_transformations = self.rules.get("common_transformations", {}).get("transformations", [])
        transformations.extend(common_transformations)
        
        if common_transformations:
            self.logger.info(f"✅ 添加了 {len(common_transformations)} 个通用转换规则")
        
        # 按sort_order排序
        transformations.sort(key=lambda x: x.get("sort_order", 999))
        
        self.logger.info(f"🎯 总共生成了 {len(transformations)} 个转换规则")
        
        return transformations
    
    def add_field_condition(self, field_name: str, transformations: List[Dict[str, Any]], 
                           description: str = "") -> bool:
        """
        添加新的字段条件规则
        
        Args:
            field_name: 字段名称
            transformations: 转换规则列表
            description: 描述信息
            
        Returns:
            是否添加成功
        """
        try:
            if "field_conditions" not in self.rules:
                self.rules["field_conditions"] = {}
            
            self.rules["field_conditions"][field_name] = {
                "description": description or f"{field_name}字段相关的转换规则",
                "transformations": transformations
            }
            
            # 保存到文件
            self._save_rules()
            
            self.logger.info(f"✅ 已添加字段条件规则: {field_name}")
            return True
            
        except Exception as e:
            self.logger.error(f"❌ 添加字段条件规则失败: {e}")
            return False
    
    def add_common_transformation(self, transformation: Dict[str, Any]) -> bool:
        """
        添加通用转换规则
        
        Args:
            transformation: 转换规则配置
            
        Returns:
            是否添加成功
        """
        try:
            if "common_transformations" not in self.rules:
                self.rules["common_transformations"] = {"transformations": []}
            
            self.rules["common_transformations"]["transformations"].append(transformation)
            
            # 保存到文件
            self._save_rules()
            
            self.logger.info(f"✅ 已添加通用转换规则: {transformation.get('name', '未命名')}")
            return True
            
        except Exception as e:
            self.logger.error(f"❌ 添加通用转换规则失败: {e}")
            return False
    
    def _save_rules(self) -> bool:
        """保存规则配置到JSON文件"""
        try:
            # 确保目录存在
            os.makedirs(os.path.dirname(self.rules_config_path), exist_ok=True)
            
            with open(self.rules_config_path, 'w', encoding='utf-8') as f:
                json.dump(self.rules, f, ensure_ascii=False, indent=2)
            
            self.logger.debug(f"✅ 已保存规则配置: {self.rules_config_path}")
            return True
            
        except Exception as e:
            self.logger.error(f"❌ 保存规则配置失败: {e}")
            return False
    
    def get_all_rules(self) -> Dict[str, Any]:
        """获取所有规则配置"""
        return self.rules.copy()
    
    def reload_rules(self) -> bool:
        """重新加载规则配置"""
        try:
            self.rules = self._load_rules()
            self.logger.info("✅ 已重新加载规则配置")
            return True
        except Exception as e:
            self.logger.error(f"❌ 重新加载规则配置失败: {e}")
            return False

# 创建全局实例
_rule_manager = None

def get_rule_manager(rules_config_path: str = None) -> TransformationRuleManager:
    """获取转换规则管理器实例（单例模式）"""
    global _rule_manager
    if _rule_manager is None or rules_config_path is not None:
        _rule_manager = TransformationRuleManager(rules_config_path)
    return _rule_manager 