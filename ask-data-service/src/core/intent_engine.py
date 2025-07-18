"""
意图识别引擎

基于BGE embedding和Elasticsearch向量检索的智能数据集匹配
"""

import re
from typing import List, Dict, Any, Optional, Tuple
from .embedding_service import EmbeddingService
from .vector_search_service import VectorSearchService
from ..utils.schema_database import SchemaDatabase
from ..utils.logger import get_logger, LogContext
import pandas as pd

class IntentRecognitionEngine:
    """意图识别引擎"""
    
    def __init__(self, embedding_service: EmbeddingService, 
                 vector_service: VectorSearchService, 
                 db: SchemaDatabase):
        """
        初始化意图识别引擎
        
        Args:
            embedding_service: BGE向量化服务
            vector_service: ES向量检索服务
            db: 数据库服务
        """
        self.logger = get_logger(__name__)
        self.embedding_service = embedding_service
        self.vector_service = vector_service
        self.db = db
        
        self.logger.info("意图识别引擎初始化完成")
    
    def recognize_intent(self, question: str, max_results: int = 5, 
                        min_score: float = 0.3) -> List[Dict[str, Any]]:
        """
        识别用户意图并返回匹配的数据集列表
        
        Args:
            question: 用户问题
            max_results: 最大返回结果数
            min_score: 最小匹配分数（基于enhanced_score）
            
        Returns:
            List[Dict]: 匹配结果列表
            格式: [{"dataset_id": "1", "dataset_name": "xx", "score": 0.95, "reason": "匹配原因"}]
        """
        with LogContext(self.logger, f"意图识别: {question}"):
            # 1. 关键词预处理和领域识别
            domain_info = self._extract_domain_info(question)
            self.logger.info(f"提取的领域信息: {domain_info}")
            
            # 2. 生成问题向量
            question_embedding = self.embedding_service.encode_single(question)
            
            # 3. 向量搜索（使用较低的min_score进行初步过滤，后续基于enhanced_score精确过滤）
            vector_results = self.vector_service.vector_search(
                query_embedding=question_embedding,
                size=max_results * 3,  # 取更多结果，后续基于enhanced_score过滤
                min_score=0.0  # 先不过滤，让所有结果进入后续处理
            )
            
            # 4. 结果后处理和排序
            processed_results = self._process_results(
                vector_results, question, domain_info, max_results
            )
            
            # 5. 基于enhanced_score进行过滤
            filtered_results = [
                result for result in processed_results 
                if result.get("enhanced_score", 0.0) >= min_score
            ]
            
            self.logger.info(f"意图识别完成: 向量搜索{len(vector_results)}个结果, 增强处理{len(processed_results)}个, 最终过滤{len(filtered_results)}个匹配结果")
            return filtered_results
    
    def _extract_domain_info(self, question: str) -> Dict[str, Any]:
        """
        从问题中提取领域信息和关键词
        
        Args:
            question: 用户问题
            
        Returns:
            Dict: 领域信息，包含：
                - keywords: 关键词列表
                - domain: 领域分类
                - location: 地理位置
                - time_range: 时间范围
                - query_type: 查询类型
                - subject: 主题词
        """
        domain_info = {
            "keywords": [],
            "domain": None,
            "location": None,
            "time_range": None,
            "query_type": None,
            "subject": None
        }
        
        # 领域关键词映射（扩充关键词）
        domain_keywords = {
            "土地": [
                "土地", "地块", "耕地", "用地", "土地利用", "地类", "面积", "DLTB", "DLBM",
                "建设用地", "农用地", "林地", "草地", "园地", "田", "荒地", "未利用地",
                "基本农田", "土地整治", "土地规划", "土地储备", "土地出让", "土地征收"
            ],
            "金融": [
                "金融", "银行", "贷款", "投资", "理财", "股票", "基金", "证券", "保险",
                "存款", "利率", "汇率", "债券", "期货", "信托", "支付", "结算", "信贷"
            ],
            "人口": [
                "人口", "户籍", "人员", "统计", "普查", "常住人口", "流动人口", "人口密度",
                "出生率", "死亡率", "迁入", "迁出", "人口结构", "老龄化", "适龄人口"
            ],
            "经济": [
                "GDP", "经济", "收入", "产值", "财政", "税收", "增长率", "发展", "投资",
                "消费", "进出口", "贸易", "产业", "企业", "市场", "价格", "成本", "效益"
            ],
            "环境": [
                "环境", "污染", "空气", "水质", "环保", "监测", "排放", "治理", "生态",
                "绿化", "节能", "减排", "碳排放", "废弃物", "噪声", "PM2.5", "空气质量"
            ],
            "交通": [
                "交通", "道路", "车辆", "运输", "公路", "铁路", "航空", "港口", "物流",
                "客运", "货运", "高速", "地铁", "公交", "停车", "通行", "路网"
            ]
        }
        
        # 同义词映射
        synonyms = {
            "面积": ["大小", "占地", "用地", "地块大小", "占地面积", "用地面积"],
            "数量": ["个数", "总数", "多少", "数目", "总量"],
            "比例": ["占比", "百分比", "构成", "比重", "份额"],
            "分布": ["分布情况", "空间分布", "地理分布", "分布状况"],
            "变化": ["变化趋势", "增长", "减少", "变动", "波动", "走势"],
            "排名": ["排序", "前几", "最大", "最小", "第一", "最多", "最少"]
        }
        
        # 查询类型映射
        query_types = {
            "统计类": ["多少", "总共", "一共", "总计", "合计"],
            "比较类": ["对比", "比较", "差异", "不同", "高于", "低于"],
            "趋势类": ["趋势", "变化", "增长", "下降", "波动"],
            "分布类": ["分布", "分散", "集中", "密集"],
            "排名类": ["排名", "排序", "前几", "最大", "最小"],
            "占比类": ["占比", "比例", "百分比", "构成"]
        }
        
        # 地理位置关键词（增强地理位置识别）
        location_patterns = [
            # 完整的行政区划匹配（用于初步提取）
            r'([\u4e00-\u9fa5]{2,}市[\u4e00-\u9fa5]{2,}区)',  # 匹配"xx市xx区"，必须是完整的市区组合
            r'([\u4e00-\u9fa5]{2,}市)',                      # 匹配"xx市"
            r'([\u4e00-\u9fa5]{2,}区)',                      # 匹配"xx区"
            r'([\u4e00-\u9fa5]{2,}[镇乡])',                  # 匹配"xx镇/乡"
            r'([\u4e00-\u9fa5]{2,}村)',                      # 匹配"xx村"
            r'([\u4e00-\u9fa5]{2,}地区)',                    # 匹配"xx地区"
            r'([\u4e00-\u9fa5]{2,}新区)',                    # 匹配"xx新区"
            r'([\u4e00-\u9fa5]{2,}开发区)',                  # 匹配"xx开发区"
            r'([\u4e00-\u9fa5]{2,}园区)'                     # 匹配"xx园区"
        ]
        
        # 时间关键词（增强时间识别）
        time_patterns = [
            r'(\d{4}年)',                # 2023年
            r'(\d{4})',                  # 2023
            r'(今年|去年|前年)',           # 相对年份
            r'(\d+年[前后])',             # X年前/后
            r'(\d{4}[-/]\d{4})',        # 2022-2023
            r'(\d{4}年度)',              # 2023年度
            r'(\d{4}上半年|\d{4}下半年)',  # 2023上半年
            r'(\d{4}第[一二三四]季度)'     # 2023第一季度
        ]
        
        question_lower = question.lower()
        
        # 1. 提取领域和关键词
        for domain, keywords in domain_keywords.items():
            matched_keywords = [kw for kw in keywords if kw in question]
            if matched_keywords:
                domain_info["domain"] = domain
                domain_info["keywords"].extend(matched_keywords)
                break
        
        self.logger.info(f"开始处理问题中的地理位置: {question}")
        
        # 2. 提取地理位置
        seen_locations = set()  # 用于存储已处理的地理位置
        
        # 预处理：移除问题中的一些干扰词
        clean_question = re.sub(r'的|是|在|有|和|与|及', '', question)
        self.logger.debug(f"清理后的问题文本: {clean_question}")
        
        # 无效词列表
        invalid_words = ['年', '面积', '城镇', '用地']
        
        def extract_locations(text: str) -> None:
            """提取地理位置并添加到seen_locations集合中"""
            # 首先尝试匹配完整的市区组合
            city_district_match = re.search(r'([\u4e00-\u9fa5]{2,})市([\u4e00-\u9fa5]{2,})区', text)
            if city_district_match:
                # 提取市
                city = f"{city_district_match.group(1)}市"
                if not any(word in city for word in invalid_words):
                    seen_locations.add(city)
                    self.logger.debug(f"从复合地名中提取市级地理关键词: {city}")
                
                # 提取区
                district = f"{city_district_match.group(2)}区"
                if not any(word in district for word in invalid_words):
                    seen_locations.add(district)
                    self.logger.debug(f"从复合地名中提取区级地理关键词: {district}")
                return
            
            # 如果没有找到市区组合，尝试单独的市或区
            for pattern in location_patterns[1:]:  # 跳过第一个模式（市区组合）
                match = re.search(pattern, text)
                if match:
                    location = match.group(1)
                    if location and not any(word in location for word in invalid_words):
                        seen_locations.add(location)
                        self.logger.debug(f"提取单独的地理关键词: {location}")
                        break
        
        # 处理文本
        extract_locations(clean_question)
        
        # 记录找到的地理位置
        if seen_locations:
            self.logger.info(f"找到的地理位置: {list(seen_locations)}")
            
            # 将处理后的地理位置添加到关键词中
            domain_info["keywords"].extend(list(seen_locations))
            
            # 选择最合适的位置作为主要位置
            # 优先选择区级单位，其次是市级单位
            main_location = None
            for loc in seen_locations:
                if '区' in loc:
                    main_location = loc
                    break
            if not main_location:
                for loc in seen_locations:
                    if '市' in loc:
                        main_location = loc
                        break
            if not main_location:
                main_location = list(seen_locations)[0]
            
            domain_info["location"] = main_location
            self.logger.info(f"提取到的地理位置: {list(seen_locations)}, 主要位置: {main_location}")
        else:
            self.logger.warning("未找到任何地理位置")
        
        # 3. 提取时间信息（支持多种时间格式）
        for pattern in time_patterns:
            matches = re.findall(pattern, question)
            if matches:
                time_str = matches[0]
                # 处理相对时间
                if time_str in ["今年", "去年", "前年"]:
                    from datetime import datetime
                    current_year = datetime.now().year
                    if time_str == "今年":
                        time_str = f"{current_year}年"
                    elif time_str == "去年":
                        time_str = f"{current_year-1}年"
                    elif time_str == "前年":
                        time_str = f"{current_year-2}年"
                domain_info["time_range"] = time_str
                domain_info["keywords"].append(time_str)
                break
        
        # 4. 识别查询类型
        for query_type, patterns in query_types.items():
            if any(pattern in question_lower for pattern in patterns):
                domain_info["query_type"] = query_type
                break
        
        # 5. 提取主题词（通过分析"的"前后结构）
        if "的" in question:
            parts = question.split("的")
            for part in parts[:-1]:  # 检查除最后一部分外的所有部分
                # 清理部分文本
                cleaned_part = part
                if domain_info["location"]:
                    cleaned_part = cleaned_part.replace(domain_info["location"], "").strip()
                if domain_info["time_range"]:
                    cleaned_part = cleaned_part.replace(domain_info["time_range"], "").strip()
                if cleaned_part and len(cleaned_part) > 1:  # 确保主题词不是单字
                    domain_info["subject"] = cleaned_part
                    domain_info["keywords"].append(cleaned_part)
                    break
        
        # 6. 处理同义词
        for standard_term, synonym_list in synonyms.items():
            if any(syn in question_lower for syn in synonym_list):
                if standard_term not in domain_info["keywords"]:
                    domain_info["keywords"].append(standard_term)
        
        # 7. 去除停用词和清理关键词
        stopwords = {"是", "的", "了", "在", "有", "和", "与", "及", "或", "但", "而", 
                    "如何", "怎么", "什么", "哪个", "多少", "情况", "现状", "一下"}
        # 保留长度大于1的关键词，去重并移除停用词
        domain_info["keywords"] = list(set(
            kw for kw in domain_info["keywords"] 
            if len(kw) > 1 and kw not in stopwords
        ))
        
        self.logger.debug(f"提取的领域信息: {domain_info}")
        return domain_info
    
    def _process_results(self, vector_results: List[Dict], question: str, 
                        domain_info: Dict, max_results: int) -> List[Dict[str, Any]]:
        """
        处理和增强搜索结果
        
        Args:
            vector_results: 向量搜索结果
            question: 原始问题
            domain_info: 领域信息
            max_results: 最大结果数
            
        Returns:
            List[Dict]: 处理后的结果
        """
        processed_results = []
        
        for result in vector_results:
            # 基础信息
            dataset_id = result.get("dataset_id")
            dataset_name = result.get("name", "")
            description = result.get("description", "")
            vector_score = result.get("similarity_score", 0.0)
            
            # 计算增强分数
            enhanced_score = self._calculate_enhanced_score(
                result, question, domain_info, vector_score
            )
            
            # 生成匹配原因
            match_reason = self._generate_match_reason(
                result, domain_info, vector_score, enhanced_score
            )
            
            processed_result = {
                "dataset_id": dataset_id,
                "dataset_name": dataset_name,
                "description": description,
                "vector_score": vector_score,
                "enhanced_score": enhanced_score,
                "match_reason": match_reason,
                "domain": result.get("domain"),
                "keywords": result.get("keywords", []),
                "tree_node_id": result.get("tree_node_id")
            }
            
            processed_results.append(processed_result)
        
        # 按增强分数排序
        processed_results.sort(key=lambda x: x["enhanced_score"], reverse=True)
        
        # 返回top结果
        return processed_results[:max_results]
    
    def _calculate_enhanced_score(self, result: Dict, question: str, 
                                 domain_info: Dict, vector_score: float) -> float:
        """
        计算增强的匹配分数
        
        Args:
            result: 搜索结果
            question: 用户问题
            domain_info: 领域信息
            vector_score: 向量相似度分数
            
        Returns:
            float: 增强后的分数
        """
        # 基础分数（向量相似度），确保在[0, 1]范围内
        base_score = max(0.0, min(vector_score, 1.0))
        
        # 计算各种匹配加分（使用乘法因子而非简单加法，避免分数过高）
        boost_factor = 1.0
        boost_details = []
        calculation_steps = []
        
        # 记录初始分数
        dataset_name = result.get("name", "")
        calculation_steps.append(f"=== 数据集[{dataset_name}]分数计算过程 ===")
        calculation_steps.append(f"1. 初始向量分数: {vector_score:.3f}")
        calculation_steps.append(f"2. 归一化基础分数: {base_score:.3f} (限制在[0,1]范围内)")
        step_count = 3
        
        # 领域匹配加成（10%提升）
        if domain_info.get("domain") and result.get("domain") == domain_info["domain"]:
            boost_factor += 0.1
            boost_details.append(f"领域匹配({domain_info['domain']}):+0.1")
            calculation_steps.append(f"{step_count}. 领域匹配加成: +0.1 (当前boost_factor={boost_factor:.3f})")
            step_count += 1
        
        # 关键词匹配加成和扣分
        result_keywords = result.get("keywords", [])
        if isinstance(result_keywords, list):
            # 提取用户问题中的时间和地区关键词
            user_time = domain_info.get("time_range")
            user_location = domain_info.get("location")
            
            # 跟踪时间和地区是否都不匹配
            time_mismatch = False
            location_mismatch = False
            
            # 时间匹配检查
            if user_time:
                calculation_steps.append(f"{step_count}. 时间匹配检查:")
                step_count += 1
                if user_time not in result_keywords:
                    boost_factor -= 0.2
                    boost_details.append(f"时间不匹配({user_time}):-0.2")
                    calculation_steps.append(f"   - 不匹配: -0.2 (当前boost_factor={boost_factor:.3f})")
                    time_mismatch = True
                else:
                    boost_factor += 0.1
                    boost_details.append(f"时间匹配({user_time}):+0.1")
                    calculation_steps.append(f"   - 匹配: +0.1 (当前boost_factor={boost_factor:.3f})")
            
            # 地区匹配检查
            if user_location:
                calculation_steps.append(f"{step_count}. 地区匹配检查:")
                step_count += 1
                if user_location not in result_keywords:
                    boost_factor -= 0.3
                    boost_details.append(f"地区不匹配({user_location}):-0.3")
                    calculation_steps.append(f"   - 不匹配: -0.3 (当前boost_factor={boost_factor:.3f})")
                    location_mismatch = True
                else:
                    boost_factor += 0.2
                    boost_details.append(f"地区匹配({user_location}):+0.2")
                    calculation_steps.append(f"   - 匹配: +0.2 (当前boost_factor={boost_factor:.3f})")
            
            # 如果时间和地区都不匹配，额外惩罚
            if time_mismatch and location_mismatch and user_time and user_location:
                boost_factor -= 0.2
                boost_details.append("时间地区双重不匹配:-0.2")
                calculation_steps.append(f"{step_count}. 时间地区双重不匹配惩罚: -0.2 (当前boost_factor={boost_factor:.3f})")
                step_count += 1
            # 如果时间和地区都匹配，额外奖励
            elif not time_mismatch and not location_mismatch and user_time and user_location:
                boost_factor += 0.2
                boost_details.append("时间地区双重匹配:+0.2")
                calculation_steps.append(f"{step_count}. 时间地区双重匹配奖励: +0.2 (当前boost_factor={boost_factor:.3f})")
                step_count += 1
            
            # 其他关键词匹配加成
            other_keywords = set(domain_info.get("keywords", [])) - {user_time, user_location}
            keyword_matches = len(other_keywords & set(result_keywords))
            if keyword_matches > 0:
                calculation_steps.append(f"{step_count}. 其他关键词匹配检查:")
                step_count += 1
                # 如果时间和地区都不匹配，则降低其他关键词的加分权重
                if time_mismatch and location_mismatch and user_time and user_location:
                    keyword_boost = min(0.01 * keyword_matches, 0.05)
                    boost_details.append(f"关键词匹配(降权)({keyword_matches}个):+{keyword_boost:.3f}")
                    calculation_steps.append(f"   - 降权匹配({keyword_matches}个关键词): +{keyword_boost:.3f} (当前boost_factor={boost_factor+keyword_boost:.3f})")
                else:
                    keyword_boost = min(0.03 * keyword_matches, 0.15)
                    boost_details.append(f"关键词匹配({keyword_matches}个):+{keyword_boost:.3f}")
                    calculation_steps.append(f"   - 正常匹配({keyword_matches}个关键词): +{keyword_boost:.3f} (当前boost_factor={boost_factor+keyword_boost:.3f})")
                boost_factor += keyword_boost
                matched_keywords = list(other_keywords & set(result_keywords))
                calculation_steps.append(f"   - 匹配的关键词: {', '.join(matched_keywords[:3])}" + ("..." if len(matched_keywords) > 3 else ""))
        
        # 计算最终分数
        original_boost = boost_factor
        boost_factor = min(boost_factor, 1.5)  # 只限制最高提升
        enhanced_score = base_score * boost_factor
        final_score = min(max(enhanced_score, 0.0), 1.0)
        
        # 记录最终计算结果
        calculation_steps.append(f"\n最终计算结果:")
        calculation_steps.append(f"- 原始提升因子: {original_boost:.3f}")
        calculation_steps.append(f"- 限制后提升因子: {boost_factor:.3f} (限制最高1.5)")
        calculation_steps.append(f"- 增强后分数: {enhanced_score:.3f} = {base_score:.3f} * {boost_factor:.3f}")
        calculation_steps.append(f"- 最终分数: {final_score:.3f} (限制在[0,1]范围内)")
        
        # 记录详细的分数计算过程
        self.logger.debug("\n".join(calculation_steps))
        if boost_details:
            self.logger.debug(f"提升详情汇总: {'; '.join(boost_details)}")
        
        return final_score
    
    def _generate_match_reason(self, result: Dict, domain_info: Dict, 
                              vector_score: float, enhanced_score: float) -> str:
        """
        生成匹配原因说明
        
        Args:
            result: 搜索结果
            domain_info: 领域信息
            vector_score: 向量分数
            enhanced_score: 增强分数
            
        Returns:
            str: 匹配原因
        """
        reasons = []
        
        # 向量相似度
        if vector_score > 0.7:
            reasons.append("语义高度相关")
        elif vector_score > 0.5:
            reasons.append("语义相关")
        else:
            reasons.append("语义部分相关")
        
        # 领域匹配
        if domain_info.get("domain") and result.get("domain") == domain_info["domain"]:
            reasons.append(f"业务领域匹配({domain_info['domain']})")
        
        # 关键词匹配
        result_keywords = result.get("keywords", [])
        if isinstance(result_keywords, list):
            matched_keywords = set(domain_info.get("keywords", [])) & set(result_keywords)
            if matched_keywords:
                reasons.append(f"关键词匹配({', '.join(list(matched_keywords)[:3])})")
        
        # 名称匹配
        dataset_name = result.get("name", "").lower()
        matched_in_name = [kw for kw in domain_info.get("keywords", []) if kw.lower() in dataset_name]
        if matched_in_name:
            reasons.append(f"数据集名称包含关键词({', '.join(matched_in_name[:2])})")
        
        return "; ".join(reasons) if reasons else "基于语义相似度匹配"
    
    def sync_datasets_to_vector_store(self, force_refresh: bool = False) -> Dict[str, Any]:
        """
        同步数据集到向量存储
        
        Args:
            force_refresh: 是否强制刷新所有数据
            
        Returns:
            Dict: 同步结果统计
        """
        with LogContext(self.logger, "同步数据集到向量存储"):
            try:
                # 获取所有活跃数据集
                datasets = self.db.list_all_datasets()
                
                sync_stats = {
                    "total_count": len(datasets),
                    "success_count": 0,
                    "failed_count": 0,
                    "errors": []
                }
                
                for dataset in datasets:
                    try:
                        dataset_id = str(dataset['id'])
                        
                        # 获取列信息构建完整的数据集信息
                        columns = self.db.list_dataset_columns(dataset_id)
                        columns_info = ", ".join([f"{col['name']}({col['type']})" for col in columns])
                        
                        # 构建完整的数据集信息
                        dataset_info = {
                            "name": dataset.get('name', ''),
                            "description": dataset.get('description', ''),
                            "keywords": self._generate_keywords_from_dataset(dataset, columns),
                            "domain": self._infer_domain_from_dataset(dataset, columns),
                            "data_summary": self._generate_data_summary(dataset, columns),
                            "columns_info": columns_info,
                            "tree_node_id": dataset.get('tree_node_id', ''),
                            "file_path": dataset.get('actual_data_path') or dataset.get('file_path', ''),
                            "status": dataset.get('status', 'active'),
                            "created_at": dataset.get('created_at'),
                            "updated_at": dataset.get('updated_at')
                        }
                        
                        # 生成向量
                        embedding = self.embedding_service.generate_dataset_embedding(dataset_info)
                        
                        # 索引到ES
                        success = self.vector_service.index_dataset(dataset_id, dataset_info, embedding)
                        
                        if success:
                            sync_stats["success_count"] += 1
                        else:
                            sync_stats["failed_count"] += 1
                            sync_stats["errors"].append(f"数据集 {dataset_id} 索引失败")
                            
                    except Exception as e:
                        sync_stats["failed_count"] += 1
                        error_msg = f"数据集 {dataset.get('id', 'unknown')} 处理失败: {str(e)}"
                        sync_stats["errors"].append(error_msg)
                        self.logger.error(error_msg)
                
                self.logger.info(f"数据集同步完成: 成功 {sync_stats['success_count']}, 失败 {sync_stats['failed_count']}")
                return sync_stats
                
            except Exception as e:
                self.logger.error(f"数据集同步失败: {e}")
                return {
                    "total_count": 0,
                    "success_count": 0,
                    "failed_count": 0,
                    "errors": [str(e)]
                }
    
    def _generate_keywords_from_dataset(self, dataset: Dict, columns: List[Dict]) -> List[str]:
        """从数据集信息生成关键词"""
        # 定义目标字段
        target_columns = ['SJNF']  # 基础字段：时间
        
        try:
            # 获取数据集文件路径
            file_path = dataset.get('actual_data_path') or dataset.get('file_path')
            if not file_path:
                return []
            
            self.logger.info(f"开始处理数据集文件: {file_path}")
            
            # 读取CSV文件，确保SJNF列作为字符串读取
            df = pd.read_csv(file_path, encoding='utf-8', dtype={'SJNF': str})
            self.logger.info(f"成功读取CSV文件，列名: {df.columns.tolist()}")
            
            # 检查字段组合
            has_area = 'TBMJ' in df.columns
            has_type = 'DLMC' in df.columns
            
            # 按字段顺序提取关键词
            field_keywords = {field: [] for field in target_columns}  # 用字典存储每个字段的关键词
            
            # 从指定列的实际值中提取关键词
            for col_name in target_columns:
                if col_name in df.columns:
                    self.logger.info(f"处理列: {col_name}")
                    # 获取列的唯一值并移除空值
                    unique_values = df[col_name].dropna().unique()
                    self.logger.info(f"列 {col_name} 的唯一值: {unique_values}")
                    
                    for value in unique_values:
                        try:
                            # 转换为字符串并清理空白字符
                            value_str = str(value).strip()
                            
                            if col_name == 'SJNF':
                                # 提取数字部分
                                year_match = re.search(r'\d{4}', value_str)
                                if year_match:
                                    year_str = year_match.group()
                                    year_keyword = f"{year_str}年"
                                    field_keywords[col_name].append(year_keyword)
                                    self.logger.info(f"添加年份关键词: {year_keyword}")
                                elif value_str.isdigit() and len(value_str) == 4:
                                    # 如果整个字符串是4位数字
                                    year_keyword = f"{value_str}年"
                                    field_keywords[col_name].append(year_keyword)
                                    self.logger.info(f"添加年份关键词: {year_keyword}")
                        except Exception as e:
                            self.logger.warning(f"处理值 {value} 时出错: {str(e)}")
                            continue
                else:
                    self.logger.warning(f"列 {col_name} 不存在于数据集中")
            
            # 提取地区信息
            field_keywords['area'] = []
            if 'XZQHMC' in df.columns:
                # 优先从XZQHMC获取地区信息
                areas = df['XZQHMC'].dropna().unique()
                for area in areas:
                    area_str = str(area).strip()
                    if area_str:
                        # 首先添加完整的地区名称
                        if area_str not in field_keywords['area']:
                            field_keywords['area'].append(area_str)
                            self.logger.info(f"从XZQHMC添加完整地区关键词: {area_str}")
                        
                        # 处理行政区划层级
                        # 使用正则表达式匹配不同级别的行政区划
                        admin_patterns = [
                            (r'([\u4e00-\u9fa5]+省)', '省级'),
                            (r'([\u4e00-\u9fa5]+[市州])', '市级'),
                            (r'([\u4e00-\u9fa5]+[县区])', '县区级'),
                            (r'([\u4e00-\u9fa5]+[镇乡])', '镇乡级'),
                            (r'([\u4e00-\u9fa5]+村)', '村级')
                        ]
                        
                        for pattern, level in admin_patterns:
                            matches = re.findall(pattern, area_str)
                            for match in matches:
                                if match and match not in field_keywords['area']:
                                    field_keywords['area'].append(match)
                                    self.logger.info(f"从XZQHMC拆分添加{level}关键词: {match}")
                        
                        # 处理组合地名（例如：镇+村）
                        if '镇' in area_str and '村' in area_str:
                            # 提取镇名
                            town_match = re.search(r'([\u4e00-\u9fa5]+镇)', area_str)
                            if town_match:
                                town = town_match.group(1)
                                if town not in field_keywords['area']:
                                    field_keywords['area'].append(town)
                                    self.logger.info(f"从XZQHMC提取镇名: {town}")
                            
                            # 提取村名
                            village_match = re.search(r'([\u4e00-\u9fa5]+村)', area_str)
                            if village_match:
                                village = village_match.group(1)
                                if village not in field_keywords['area']:
                                    field_keywords['area'].append(village)
                                    self.logger.info(f"从XZQHMC提取村名: {village}")
            elif 'QSDWMC' in df.columns:
                # 如果没有XZQHMC，则从QSDWMC获取地区信息
                areas = df['QSDWMC'].dropna().unique()
                for area in areas:
                    area_str = str(area).strip()
                    if area_str:
                        # 首先添加完整的地区名称
                        if area_str not in field_keywords['area']:
                            field_keywords['area'].append(area_str)
                            self.logger.info(f"从QSDWMC添加完整地区关键词: {area_str}")
                        
                        # 处理行政区划层级
                        # 使用正则表达式匹配不同级别的行政区划
                        admin_patterns = [
                            (r'([\u4e00-\u9fa5]+省)', '省级'),
                            (r'([\u4e00-\u9fa5]+[市州])', '市级'),
                            (r'([\u4e00-\u9fa5]+[县区])', '县区级'),
                            (r'([\u4e00-\u9fa5]+[镇乡])', '镇乡级'),
                            (r'([\u4e00-\u9fa5]+村)', '村级')
                        ]
                        
                        for pattern, level in admin_patterns:
                            matches = re.findall(pattern, area_str)
                            for match in matches:
                                if match and match not in field_keywords['area']:
                                    field_keywords['area'].append(match)
                                    self.logger.info(f"从QSDWMC拆分添加{level}关键词: {match}")
                        
                        # 处理组合地名（例如：镇+村）
                        if '镇' in area_str and '村' in area_str:
                            # 提取镇名
                            town_match = re.search(r'([\u4e00-\u9fa5]+镇)', area_str)
                            if town_match:
                                town = town_match.group(1)
                                if town not in field_keywords['area']:
                                    field_keywords['area'].append(town)
                                    self.logger.info(f"从QSDWMC提取镇名: {town}")
                            
                            # 提取村名
                            village_match = re.search(r'([\u4e00-\u9fa5]+村)', area_str)
                            if village_match:
                                village = village_match.group(1)
                                if village not in field_keywords['area']:
                                    field_keywords['area'].append(village)
                                    self.logger.info(f"从QSDWMC提取村名: {village}")
            
            # 添加属性关键词（根据字段组合）
            if has_area and has_type:
                # 同时有TBMJ和DLMC字段
                field_keywords['attr'] = ['不同地类面积']
                self.logger.info("添加属性关键词: 不同地类面积")
            elif has_type:
                # 只有DLMC字段
                field_keywords['attr'] = ['不同地类名称']
                self.logger.info("添加属性关键词: 不同地类名称")
            
            # 按字段顺序合并关键词
            keywords = []
            for field in ['SJNF', 'area', 'attr']:
                if field in field_keywords:
                    keywords.extend(field_keywords[field])
            
            self.logger.info(f"提取的关键词（按字段排序）: {keywords}")
        
        except Exception as e:
            self.logger.error(f"从数据集提取关键词时出错: {str(e)}")
            import traceback
            self.logger.error(f"错误详情: {traceback.format_exc()}")
            return []
        
        # 去重和过滤
        stopwords = {"是", "的", "了", "在", "有", "和", "与", "及", "或", "但", "而", "数据", "信息", "记录"}
        unique_keywords = []
        seen = set()
        
        # 保持顺序的去重
        for kw in keywords:
            if kw not in seen and kw not in stopwords:
                unique_keywords.append(kw)
                seen.add(kw)
        
        self.logger.info(f"最终关键词: {unique_keywords[:20]}")
        return unique_keywords[:20]  # 限制关键词数量
    
    def _infer_domain_from_dataset(self, dataset: Dict, columns: List[Dict]) -> str:
        """从数据集信息推断业务领域"""
        name = dataset.get('name', '').lower()
        description = dataset.get('description', '').lower()
        
        # 列名集合
        column_names = " ".join([col.get('name', '').lower() for col in columns])
        
        # 合并文本
        text = f"{name} {description} {column_names}"
        
        # 领域判断规则
        if any(keyword in text for keyword in ['土地', '地块', '耕地', '用地', 'dltb', 'dlbm', '面积']):
            return '土地'
        elif any(keyword in text for keyword in ['金融', '银行', '贷款', '投资', '理财']):
            return '金融'
        elif any(keyword in text for keyword in ['人口', '户籍', '人员', '统计']):
            return '人口'
        elif any(keyword in text for keyword in ['gdp', '经济', '收入', '产值', '财政']):
            return '经济'
        elif any(keyword in text for keyword in ['环境', '污染', '空气', '水质']):
            return '环境'
        elif any(keyword in text for keyword in ['交通', '道路', '车辆', '运输']):
            return '交通'
        else:
            return '通用'
    
    def _generate_data_summary(self, dataset: Dict, columns: List[Dict]) -> str:
        """生成数据摘要，包含时间、地区和地类名称信息"""
        name = dataset.get('name', '青岛测试数据集')
        
        try:
            # 获取数据集文件路径
            file_path = dataset.get('actual_data_path') or dataset.get('file_path')
            if not file_path:
                return "数据集文件路径未找到"
            
            # 读取CSV文件
            df = pd.read_csv(file_path, encoding='utf-8', dtype={'SJNF': str})
            
            # 提取时间信息
            time_info = ""
            if 'SJNF' in df.columns:
                years = sorted(df['SJNF'].dropna().unique())
                if len(years) == 1:
                    time_info = f"数据时间为{years[0]}年"
                elif len(years) > 1:
                    # 将所有年份用顿号连接
                    years_str = '、'.join(f"{year}年" for year in years)
                    time_info = f"数据时间包括：{years_str}"
            
            # 提取地区信息
            area_info = ""
            if 'XZQHMC' in df.columns:
                # 优先从XZQHMC获取地区信息
                areas = sorted(df['XZQHMC'].dropna().unique())
                if areas:
                    area_info = f"涵盖地区：{', '.join(areas[:10])}"
                    if len(areas) > 10:
                        area_info += "等"
            elif 'QSDWMC' in df.columns:
                # 如果没有XZQHMC，则从QSDWMC获取地区信息
                areas = sorted(df['QSDWMC'].dropna().unique())
                if areas:
                    area_info = f"涵盖权属单位：{', '.join(areas[:10])}"
                    if len(areas) > 10:
                        area_info += "等"
            
            # 提取地类名称信息
            geo_names = set()
            geo_name_columns = ['DLMC']
            for col in geo_name_columns:
                if col in df.columns:
                    names = df[col].dropna().unique()
                    geo_names.update(names)
            
            # 生成摘要
            summary_parts = []
            
            # 添加数据集基本信息
            summary_parts.append(name)
            
            # 添加时间和地区信息
            if time_info:
                summary_parts.append(time_info)
            if area_info:
                summary_parts.append(area_info)
            
            # 添加地类名称信息
            if geo_names:
                geo_names_list = sorted(list(geo_names))
                geo_names_info = f"包含的地类：{', '.join(geo_names_list[:20])}"
                if len(geo_names_list) > 20:
                    geo_names_info += "等"
                summary_parts.append(geo_names_info)
            
            # 合并所有信息
            summary = '。'.join(summary_parts) + '。'
            
            return summary
            
        except Exception as e:
            self.logger.error(f"生成数据摘要时出错: {str(e)}")
            import traceback
            self.logger.error(f"错误详情: {traceback.format_exc()}")
            return f"数据摘要生成失败: {str(e)}"
    
    def _get_field_description(self, field: str) -> str:
        """获取字段的中文描述"""
        descriptions = {
            'FID': '唯一标识符',
            'OBJECTID': '对象ID',
            'BSM': '标识码',
            'YSDM': '要素代码',
            'TBYBH': '图斑预编号',
            'TBBH': '图斑编号',
            'DLBM': '地类编码',
            'DLMC': '地类名称',
            'KCDLBM': '扣除地类编码',
            'QSXZ': '权属性质',
            'QSDWDM': '权属单位代码',
            'QSDWMC': '权属单位名称',
            'ZLDWDM': '坐落单位代码',
            'ZLDWMC': '坐落单位名称',
            'TBMJ': '图斑面积',
            'KCMJ': '扣除面积',
            'TBDLMJ': '图斑地类面积',
            'KCXS': '扣除系数',
            'GDLX': '耕地类型',
            'GDPDJB': '耕地坡度级别',
            'XZDWKD': '线状地物宽度',
            'GDDB': '耕地等别',
            'TBXHDM': '图斑细化代码',
            'TBXHMC': '图斑细化名称',
            'ZZSXDM': '种植属性代码',
            'ZZSXMC': '种植属性名称',
            'FRDBS': '分任务代表数',
            'CZCSXM': '操作处数项目',
            'SJNF': '数据年份',
            'MSSM': '描述说明',
            'XZQHDM': '行政区划代码',
            'XZQHMC': '行政区划名称',
            'SHAPE_Leng': '形状长度',
            'SHAPE_Area': '形状面积',
            'HDMC': '名称',
            'BZ': '备注'
        }
        return descriptions.get(field, field) 