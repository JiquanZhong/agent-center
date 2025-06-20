"""
数据转换配置辅助工具

提供针对不同业务场景的数据转换配置模板
"""

from typing import Dict, List, Any, Optional
import pandas as pd
from ..utils.logger import get_logger

class TransformationsHelper:
    """数据转换配置助手"""
    
    def __init__(self):
        self.logger = get_logger(__name__)
    
    @staticmethod
    def get_land_data_transformations(df: pd.DataFrame = None) -> List[Dict[str, Any]]:
        """
        获取土地数据的标准转换配置
        
        Args:
            df: pandas DataFrame，如果提供则根据实际字段动态生成
        
        Returns:
            适用于土地数据的转换配置列表
        """
        transformations = []
        sort_order = 0
        
        # 如果没有提供DataFrame，返回默认配置
        if df is None:
            return TransformationsHelper._get_default_land_transformations()
        
        # 1. 处理BSM字段（标识码）- 如果存在
        # if 'BSM' in df.columns:
        #     transformations.append({
        #         "name": "提取县级代码",
        #         "type": "extract",
        #         "params": {
        #             "column": "BSM",
        #             "pattern": "^(\\d{6})",  # 提取前6位
        #             "new_column": "县级代码"
        #         },
        #         "sort_order": sort_order,
        #         "enabled": True,
        #         "description": "从BSM（标识码）中提取前6位作为县级行政区划代码"
        #     })
        #     sort_order += 1
            
        #     # 县级代码映射
        #     transformations.append({
        #         "name": "县级代码映射",
        #         "type": "map_values",
        #         "params": {
        #             "column": "县级代码",
        #             "mapping": {
        #                 "360702": "章贡区",
        #                 "360703": "南康区",
        #                 "360704": "赣县区",
        #                 "360722": "信丰县",
        #                 "360723": "大余县",
        #                 "360724": "上犹县",
        #                 "360725": "崇义县",
        #                 "360726": "安远县"
        #             },
        #             "new_column": "行政区名称"
        #         },
        #         "sort_order": sort_order,
        #         "enabled": True,
        #         "description": "将县级行政区划代码映射为区县名称"
        #     })
        #     sort_order += 1
        
        # 2. 处理ZLDWDM字段（坐落单位代码）- 如果存在
        if 'ZLDWDM' in df.columns:
            # 提取省级代码
            transformations.append({
                "name": "提取省级代码",
                "type": "extract",
                "params": {
                    "column": "ZLDWDM",
                    "pattern": "^(\\d{2})",
                    "new_column": "省级代码"
                },
                "sort_order": sort_order,
                "enabled": True,
                "description": "从ZLDWDM（坐落单位代码）中提取前2位作为省级行政区划代码"
            })
            sort_order += 1
            
            # 提取市级代码
            transformations.append({
                "name": "提取市级代码",
                "type": "extract",
                "params": {
                    "column": "ZLDWDM",
                    "pattern": "^(\\d{4})",
                    "new_column": "市级代码"
                },
                "sort_order": sort_order,
                "enabled": True,
                "description": "从ZLDWDM（坐落单位代码）中提取前4位作为市级行政区划代码"
            })
            sort_order += 1
            
            # 提取县级代码（如果BSM不存在）
            if 'BSM' not in df.columns:
                transformations.append({
                    "name": "提取县级代码",
                    "type": "extract",
                    "params": {
                        "column": "ZLDWDM",
                        "pattern": "^(\\d{6})",
                        "new_column": "县级代码"
                    },
                    "sort_order": sort_order,
                    "enabled": True,
                    "description": "从ZLDWDM（坐落单位代码）中提取前6位作为县级行政区划代码"
                })
                sort_order += 1
        
        # 3. 处理DLBM字段（地类编码）- 如果存在
        if 'DLBM' in df.columns:
            # 提取大类编码
            transformations.append({
                "name": "提取大类编码",
                "type": "extract",
                "params": {
                    "column": "DLBM",
                    "pattern": "^(\\d{2})",  # 提取前2位
                    "new_column": "大类编码"
                },
                "sort_order": sort_order,
                "enabled": True,
                "description": "从DLBM（地类编码）中提取前2位作为大类编码"
            })
            sort_order += 1
            
            # 大类编码映射
            transformations.append({
                "name": "大类编码映射",
                "type": "map_values",
                "params": {
                    "column": "大类编码",
                    "mapping": {
                        "01": "耕地",
                        "02": "园地",
                        "03": "林地", 
                        "04": "草地",
                        "05": "商服用地",
                        "06": "工矿仓储用地",
                        "07": "住宅用地",
                        "08": "公共管理与公共服务用地",
                        "09": "特殊用地",
                        "10": "交通运输用地",
                        "11": "水域及水利设施用地",
                        "12": "其他土地"
                    },
                    "new_column": "一级地类名称"
                },
                "sort_order": sort_order,
                "enabled": True,
                "description": "将大类编码映射为一级地类名称"
            })
            sort_order += 1
            
            # 详细地类编码映射
            transformations.append({
                "name": "详细地类编码映射",
                "type": "map_values",
                "params": {
                    "column": "DLBM",
                    "mapping": {
                        # 耕地
                        "0101": "水田",
                        "0102": "水浇地", 
                        "0103": "旱地",
                        # 园地
                        "0201": "果园",
                        "0202": "茶园",
                        "0203": "橡胶园",
                        "0204": "其他园地",
                        # 林地
                        "0301": "乔木林地",
                        "0302": "竹林地",
                        "0303": "红树林地",
                        "0304": "森林沼泽",
                        "0305": "灌木林地",
                        "0306": "灌丛沼泽",
                        "0307": "其他林地",
                        # 草地
                        "0401": "天然牧草地",
                        "0402": "沼泽草地",
                        "0403": "人工牧草地",
                        "0404": "其他草地",
                        # 商服用地
                        "05H1": "批发零售用地",
                        "05H2": "住宿餐饮用地",
                        "05H3": "商务金融用地",
                        "05H4": "其他商服用地",
                        # 工矿仓储用地
                        "0601": "工业用地",
                        "0602": "采矿用地",
                        "0603": "仓储用地",
                        # 住宅用地
                        "0701": "城镇住宅用地",
                        "0702": "农村宅基地",
                        # 公共管理与公共服务用地
                        "0801": "机关团体用地",
                        "0802": "新闻出版用地",
                        "0803": "科研用地",
                        "0804": "教育用地",
                        "0805": "体育用地",
                        "0806": "医疗卫生用地",
                        "0807": "社会福利用地",
                        "0808": "文化设施用地",
                        "0809": "公共设施用地",
                        "0810": "公园与绿地",
                        # 特殊用地
                        "0901": "军事用地",
                        "0902": "使领馆用地",
                        "0903": "监教用地",
                        "0904": "宗教用地",
                        "0905": "殡葬用地",
                        # 交通运输用地
                        "1001": "铁路用地",
                        "1002": "轨道交通用地",
                        "1003": "公路用地",
                        "1004": "街巷用地",
                        "1005": "农村道路",
                        "1006": "机场用地",
                        "1007": "港口码头用地",
                        "1008": "管道运输用地",
                        # 水域及水利设施用地
                        "1101": "河流水面",
                        "1102": "湖泊水面",
                        "1103": "水库水面",
                        "1104": "坑塘水面",
                        "1104A": "养殖坑塘",
                        "1105": "沿海滩涂",
                        "1106": "内陆滩涂",
                        "1107": "沟渠",
                        "1107A": "干渠",
                        "1108": "沼泽地",
                        "1109": "水工建筑用地",
                        "1110": "冰川及永久积雪",
                        # 其他土地
                        "1201": "空闲地",
                        "1202": "设施农用地",
                        "1203": "田坎",
                        "1204": "盐碱地",
                        "1205": "沙地",
                        "1206": "裸土地",
                        "1207": "裸岩石砾地",
                        "1208": "其他草地",
                        # 城镇村及工矿用地
                        "201": "城市",
                        "201A": "城市独立工业用地",
                        "202": "建制镇",
                        "202A": "建制镇独立工业用地",
                        "203": "村庄",
                        "203A": "村庄独立工业用地",
                        "204": "盐田及采矿用地",
                        "205": "特殊用地"
                    },
                    "new_column": "详细地类名称"
                },
                "sort_order": sort_order,
                "enabled": True,
                "description": "将详细地类编码转换为具体地类名称"
            })
            sort_order += 1
        
        # 4. 处理面积字段 - 检查哪些存在
        area_fields = ['TBMJ', 'KCMJ', 'TBDLMJ', 'area', 'Shape_Area']
        for area_field in area_fields:
            if area_field in df.columns:
                # 标准化面积数据
                transformations.append({
                    "name": f"标准化{area_field}",
                    "type": "to_numeric",
                    "params": {
                        "column": area_field,
                        "errors": "coerce"
                    },
                    "sort_order": sort_order,
                    "enabled": True,
                    "description": f"确保{area_field}字段为数值类型"
                })
                sort_order += 1
                
                # 四舍五入面积
                transformations.append({
                    "name": f"四舍五入{area_field}",
                    "type": "round_numbers",
                    "params": {
                        "column": area_field,
                        "decimals": 2
                    },
                    "sort_order": sort_order,
                    "enabled": True,
                    "description": f"将{area_field}保留2位小数"
                })
                sort_order += 1
        
        # 5. 处理字符串字段的清理 - 检查哪些存在
        string_fields = ['QSDWMC', 'ZLDWMC', 'DLMC', 'ZZSXMC', 'GDLX']
        for field in string_fields:
            if field in df.columns:
                transformations.append({
                    "name": f"清理{field}空白字符",
                    "type": "strip",
                    "params": {
                        "column": field
                    },
                    "sort_order": sort_order,
                    "enabled": True,
                    "description": f"清理{field}字段的前后空白字符"
                })
                sort_order += 1
        
        # 6. 处理NaN值填充 - 检查哪些存在
        nullable_fields = ['HDMC', 'TBXHMC', 'BZ']
        for field in nullable_fields:
            if field in df.columns:
                transformations.append({
                    "name": f"填充{field}空值",
                    "type": "fill_na",
                    "params": {
                        "column": field,
                        "value": "未知"
                    },
                    "sort_order": sort_order,
                    "enabled": True,
                    "description": f"将{field}字段的空值填充为'未知'"
                })
                sort_order += 1
        
        return transformations
    
    @staticmethod
    def _get_default_land_transformations() -> List[Dict[str, Any]]:
        """
        获取默认的土地数据转换配置（向后兼容）
        
        Returns:
            默认土地数据转换配置列表
        """
        return [
            {
                "name": "提取县级代码",
                "type": "extract",
                "params": {
                    "column": "BSM",
                    "pattern": "^(\\d{6})",  # 提取前6位
                    "new_column": "县级代码"
                },
                "description": "从BSM（标识码）中提取前6位作为县级行政区划代码"
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
                "description": "将县级行政区划代码映射为区县名称"
            },
            {
                "name": "提取大类编码",
                "type": "extract",
                "params": {
                    "column": "DLBM",
                    "pattern": "^(\\d{2})",  # 提取前2位
                    "new_column": "大类编码"
                },
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
                "description": "将大类编码映射为一级地类名称"
            },
            {
                "name": "标准化面积数据",
                "type": "to_numeric",
                "params": {
                    "column": "TBMJ",
                    "errors": "coerce"
                },
                "description": "确保图斑面积字段为数值类型"
            },
            {
                "name": "详细地类编码映射",
                "type": "map_values",
                "params": {
                    "column": "DLBM",
                    "mapping": {
                        # 耕地
                        "0101": "水田",
                        "0102": "水浇地", 
                        "0103": "旱地",
                        # 园地
                        "0201": "果园",
                        "0202": "茶园",
                        "0203": "橡胶园",
                        "0204": "其他园地",
                        # 林地
                        "0301": "乔木林地",
                        "0302": "竹林地",
                        "0303": "红树林地",
                        "0304": "森林沼泽",
                        "0305": "灌木林地",
                        "0306": "灌丛沼泽",
                        "0307": "其他林地",
                        # 草地
                        "0401": "天然牧草地",
                        "0402": "沼泽草地",
                        "0403": "人工牧草地",
                        "0404": "其他草地",
                        # 交通运输用地
                        "1001": "铁路用地",
                        "1002": "轨道交通用地",
                        "1003": "公路用地",
                        "1006": "农村道路",
                        "1007": "机场用地",
                        "1008": "港口码头用地",
                        "1009": "管道运输用地",
                        # 水域
                        "1101": "河流水面",
                        "1102": "湖泊水面",
                        "1103": "水库水面",
                        "1104": "坑塘水面",
                        "1104A": "养殖坑塘",
                        "1105": "沿海滩涂",
                        "1106": "内陆滩涂",
                        "1107": "沟渠",
                        "1107A": "干渠",
                        "1108": "沼泽地",
                        "1109": "水工建筑用地",
                        "1110": "冰川及永久积雪",
                        # 其他类型土地
                        "1202": "设施农用地",
                        "1203": "田坎",
                        "1204": "盐碱地",
                        "1205": "沙地",
                        "1206": "裸土地",
                        "1207": "裸岩石砾地",
                        # 城镇村及工矿用地
                        "201": "城市",
                        "201A": "城市独立工业用地",
                        "202": "建制镇",
                        "202A": "建制镇独立工业用地",
                        "203": "村庄",
                        "203A": "村庄独立工业用地",
                        "204": "盐田及采矿用地",
                        "205": "特殊用地"
                    },
                    "new_column": "地类名称"
                },
                "description": "将详细地类编码转换为具体地类名称"
            },
            {
                "name": "四舍五入面积",
                "type": "round_numbers",
                "params": {
                    "column": "TBMJ",
                    "decimals": 2
                },
                "description": "将面积保留2位小数"
            }
        ]
    
    
    @staticmethod  
    def generate_transformations_for_data(df: pd.DataFrame, data_type: str = "auto") -> List[Dict[str, Any]]:
        """
        根据数据特征自动生成转换配置
        
        Args:
            df: pandas DataFrame
            data_type: 数据类型 ("land", "auto")
            
        Returns:
            转换配置列表
        """
        # 检查是否为土地数据（包含ZLDWDM和DLBM字段）
        if 'ZLDWDM' in df.columns and 'DLBM' in df.columns:
            return TransformationsHelper.get_land_data_transformations(df)
        
        # 对于非土地数据，生成通用转换
        transformations = []
        
        for column in df.columns:
            # 数值列的通用处理
            if df[column].dtype in ['int64', 'float64']:
                # 如果有缺失值，填充为0
                if df[column].isnull().any():
                    transformations.append({
                        "name": f"填充{column}缺失值",
                        "type": "fill_na",
                        "params": {
                            "column": column,
                            "value": 0
                        },
                        "description": f"将{column}字段的缺失值填充为0"
                    })
                
                # 如果是浮点数，四舍五入
                if df[column].dtype == 'float64':
                    transformations.append({
                        "name": f"四舍五入{column}",
                        "type": "round_numbers",
                        "params": {
                            "column": column,
                            "decimals": 2
                        },
                        "description": f"将{column}保留2位小数"
                    })
            
            # 字符串列的通用处理
            elif df[column].dtype == 'object':
                # 清理空白字符
                transformations.append({
                    "name": f"清理{column}空白字符",
                    "type": "strip",
                    "params": {
                        "column": column
                    },
                    "description": f"移除{column}字段的前后空白字符"
                })
        
        return transformations
    
    
    @staticmethod
    def create_custom_transformation(name: str, trans_type: str, params: Dict[str, Any], 
                                   description: str = "") -> Dict[str, Any]:
        """
        创建自定义转换配置
        
        Args:
            name: 转换名称
            trans_type: 转换类型
            params: 转换参数
            description: 描述
            
        Returns:
            转换配置字典
        """
        return {
            "name": name,
            "type": trans_type,
            "params": params,
            "description": description,
            "enabled": True
        }
    
    @staticmethod
    def validate_transformation(transformation: Dict[str, Any]) -> List[str]:
        """
        验证转换配置的有效性
        
        Args:
            transformation: 转换配置
            
        Returns:
            错误信息列表
        """
        errors = []
        
        # 检查必需字段
        required_fields = ["type", "params"]
        for field in required_fields:
            if field not in transformation:
                errors.append(f"缺少必需字段: {field}")
        
        # 检查转换类型
        valid_types = [
            'strip', 'extract', 'round_numbers', 'to_numeric', 'fill_na', 'map_values'
        ]
        
        if "type" in transformation and transformation["type"] not in valid_types:
            errors.append(f"无效的转换类型: {transformation['type']}")
        
        # 检查参数
        if "params" in transformation:
            params = transformation["params"]
            if not isinstance(params, dict):
                errors.append("params字段必须是字典类型")
            elif "column" not in params and transformation.get("type") not in ["remove_duplicates"]:
                errors.append("大多数转换类型需要指定column参数")
        
        return errors 