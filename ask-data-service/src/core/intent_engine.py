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
    
    def rewrite_question(self, question: str) -> str:
        """
        重写用户问题，使其更专业、清晰、无歧义。
        只对时间和查询目标进行标准化，保留原始关键词。
        
        Args:
            question: 原始用户问题
            
        Returns:
            str: 重写后的问题
        """
        # 1. 提取关键信息
        domain_info = self._extract_domain_info(question)
        
        # 2. 构建重写后的问题
        components = []
        
        # 添加地理位置
        if domain_info["location"]:
            components.append(domain_info["location"])
        
        # 添加时间范围（只标准化相对时间）
        if domain_info["time_range"]:
            time_str = domain_info["time_range"]
            if time_str == "今年":
                from datetime import datetime
                time_str = f"{datetime.now().year}年"
            elif time_str == "去年":
                from datetime import datetime
                time_str = f"{datetime.now().year - 1}年"
            components.append(time_str)
        
        # 提取核心查询目标
        query_target = None
        keywords = domain_info["keywords"]
        
        # 查询目标映射
        target_mappings = {
            "面积": {"面积", "总面积", "用地面积", "占地", "地块大小"},
            "数量": {"数量", "个数", "总数", "多少个"},
            "比例": {"比例", "占比", "百分比", "构成"},
            "分布": {"分布", "分布情况", "空间分布"},
            "变化": {"变化", "变化趋势", "增长", "减少"},
            "排名": {"排名", "排序", "前几", "最大", "最小"}
        }
        
        # 分析关键词确定查询目标
        for target, target_keywords in target_mappings.items():
            if any(kw in target_keywords for kw in keywords):
                query_target = target
                break
        
        # 处理特殊查询词
        special_queries = {
            "多少": "数量",
            "怎么样": "情况",
            "如何": "情况"
        }
        for query, replacement in special_queries.items():
            if query in question:
                query_target = replacement
        
        # 提取原始主题词（保留用户原始表述）
        subject = None
        # 尝试从问题中提取主题词
        if "的" in question:
            parts = question.split("的")
            for part in parts[:-1]:  # 检查除最后一部分外的所有部分
                # 移除时间和地点信息后的剩余部分可能是主题词
                cleaned_part = part
                if domain_info["location"]:
                    cleaned_part = cleaned_part.replace(domain_info["location"], "").strip()
                if domain_info["time_range"]:
                    cleaned_part = cleaned_part.replace(domain_info["time_range"], "").strip()
                if cleaned_part:
                    subject = cleaned_part
                    break
        
        # 组装重写后的问题
        if components:
            # 添加原始主题词（如果有）
            if subject:
                components.append(subject)
            
            if query_target:
                rewritten = f"查询{' '.join(components)}的{query_target}"
            else:
                # 如果没有明确的查询目标，保持原问题的查询部分
                original_query = question.split("的")[-1] if "的" in question else question
                rewritten = f"查询{' '.join(components)}的{original_query}"
        else:
            rewritten = question  # 如果无法有效重写，返回原问题
            
        self.logger.info(f"问题重写: {question} -> {rewritten}")
        return rewritten

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
            # 重写用户问题
            rewritten_question = self.rewrite_question(question)
            self.logger.info(f"重写后的问题: {rewritten_question}")
            
            # 1. 关键词预处理和领域识别
            domain_info = self._extract_domain_info(rewritten_question)
            
            # 2. 生成问题向量
            question_embedding = self.embedding_service.encode_single(rewritten_question)
            
            # 3. 向量搜索（使用较低的min_score进行初步过滤，后续基于enhanced_score精确过滤）
            vector_results = self.vector_service.vector_search(
                query_embedding=question_embedding,
                size=max_results * 3,  # 取更多结果，后续基于enhanced_score过滤
                min_score=0.0  # 先不过滤，让所有结果进入后续处理
            )
            
            # 4. 结果后处理和排序
            processed_results = self._process_results(
                vector_results, rewritten_question, domain_info, max_results
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
            Dict: 领域信息
        """
        domain_info = {
            "keywords": [],
            "domain": None,
            "location": None,
            "time_range": None
        }
        
        # 领域关键词映射
        domain_keywords = {
            "土地": ["土地", "地块", "耕地", "用地", "土地利用", "地类", "面积", "DLTB", "DLBM"],
            "金融": ["金融", "银行", "贷款", "投资", "理财", "股票", "基金"],
            "人口": ["人口", "户籍", "人员", "统计", "普查"],
            "经济": ["GDP", "经济", "收入", "产值", "财政", "税收"],
            "环境": ["环境", "污染", "空气", "水质", "环保", "监测"],
            "交通": ["交通", "道路", "车辆", "运输", "公路", "铁路"]
        }
        
        # 地理位置关键词
        location_patterns = [
            r'([^省市县区]+[省市县区])',  # 省市县区
            r'([^地区]+地区)',          # 地区
            r'([^自治区]+自治区)'        # 自治区
        ]
        
        # 时间关键词
        time_patterns = [
            r'(\d{4}年)',
            r'(\d{4})',
            r'(今年|去年|前年)',
            r'(\d+月)'
        ]
        
        question_lower = question.lower()
        
        # 提取领域和关键词
        for domain, keywords in domain_keywords.items():
            if any(keyword in question for keyword in keywords):
                domain_info["domain"] = domain
                domain_info["keywords"].extend([kw for kw in keywords if kw in question])
                break
        
        # 提取地理位置
        for pattern in location_patterns:
            matches = re.findall(pattern, question)
            if matches:
                domain_info["location"] = matches[0]
                break
        
        # 提取时间信息
        for pattern in time_patterns:
            matches = re.findall(pattern, question)
            if matches:
                domain_info["time_range"] = matches[0]
                break
        
        # 提取其他关键词（去除停用词）
        stopwords = {"是", "的", "了", "在", "有", "和", "与", "及", "或", "但", "而", "如何", "怎么", "什么", "哪个", "多少"}
        words = re.findall(r'[\u4e00-\u9fff]+', question)  # 提取中文词汇
        keywords = [word for word in words if len(word) > 1 and word not in stopwords]
        domain_info["keywords"].extend(keywords)
        
        # 去重
        domain_info["keywords"] = list(set(domain_info["keywords"]))
        
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
        
        # 领域匹配加成（10%提升）
        if domain_info.get("domain") and result.get("domain") == domain_info["domain"]:
            boost_factor += 0.1
            boost_details.append(f"领域匹配({domain_info['domain']}):+0.1")
        
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
                # 检查数据集是否包含对应年份
                if user_time not in result_keywords:
                    boost_factor -= 0.2  # 时间不匹配扣20%
                    boost_details.append(f"时间不匹配({user_time}):-0.2")
                    time_mismatch = True
                else:
                    boost_factor += 0.1  # 时间匹配加10%
                    boost_details.append(f"时间匹配({user_time}):+0.1")
            
            # 地区匹配检查
            if user_location:
                # 检查数据集是否包含对应地区
                if user_location not in result_keywords:
                    boost_factor -= 0.2  # 地区不匹配扣20%
                    boost_details.append(f"地区不匹配({user_location}):-0.2")
                    location_mismatch = True
                else:
                    boost_factor += 0.1  # 地区匹配加10%
                    boost_details.append(f"地区匹配({user_location}):+0.1")
            
            # 如果时间和地区都不匹配，额外惩罚
            if time_mismatch and location_mismatch and user_time and user_location:
                boost_factor -= 0.3  # 双重不匹配额外扣30%
                boost_details.append("时间地区双重不匹配:-0.3")
            
            # 其他关键词匹配加成
            other_keywords = set(domain_info.get("keywords", [])) - {user_time, user_location}
            keyword_matches = len(other_keywords & set(result_keywords))
            if keyword_matches > 0:
                # 如果时间和地区都不匹配，则降低其他关键词的加分权重
                if time_mismatch and location_mismatch and user_time and user_location:
                    keyword_boost = min(0.01 * keyword_matches, 0.05)  # 降低到原来的1/3
                    boost_details.append(f"关键词匹配(降权)({keyword_matches}个):+{keyword_boost:.3f}")
                else:
                    # 关键词匹配度：每个匹配词增加3%，最多15%
                    keyword_boost = min(0.03 * keyword_matches, 0.15)
                    boost_details.append(f"关键词匹配({keyword_matches}个):+{keyword_boost:.3f}")
                boost_factor += keyword_boost
                matched_keywords = list(other_keywords & set(result_keywords))
        
        # 名称匹配加成（8%提升）
        dataset_name = result.get("name", "").lower()
        name_matched_keywords = [kw for kw in domain_info.get("keywords", []) if kw.lower() in dataset_name]
        if name_matched_keywords:
            boost_factor += 0.08
            boost_details.append(f"名称匹配({','.join(name_matched_keywords[:2])}):+0.08")
        
        # 描述匹配加成（5%提升）
        description = result.get("description", "").lower()
        desc_matched_keywords = [kw for kw in domain_info.get("keywords", []) if kw.lower() in description]
        if desc_matched_keywords:
            boost_factor += 0.05
            boost_details.append(f"描述匹配({','.join(desc_matched_keywords[:2])}):+0.05")
        
        # 计算最终分数，不设置最低限制，只限制最高提升1.5倍
        original_boost = boost_factor
        boost_factor = min(boost_factor, 1.5)  # 只限制最高提升
        enhanced_score = base_score * boost_factor
        
        # 确保最终分数在[0, 1]范围内
        final_score = min(max(enhanced_score, 0.0), 1.0)
        
        # 记录详细的分数计算过程
        dataset_name = result.get("name", "")
        self.logger.debug(f"分数计算 [{dataset_name}]: 向量={base_score:.3f}, 提升={original_boost:.3f}, 最终={final_score:.3f}")
        if boost_details:
            self.logger.debug(f"  提升详情: {'; '.join(boost_details)}")
        
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
        target_columns = ['SJNF', 'XZQHMC', 'DLMC']  # 固定顺序
        
        try:
            # 获取数据集文件路径
            file_path = dataset.get('actual_data_path') or dataset.get('file_path')
            if not file_path:
                return []
            
            self.logger.info(f"开始处理数据集文件: {file_path}")
            
            # 读取CSV文件，确保SJNF列作为字符串读取
            df = pd.read_csv(file_path, encoding='utf-8', dtype={'SJNF': str})
            self.logger.info(f"成功读取CSV文件，列名: {df.columns.tolist()}")
            
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
                            elif col_name == 'DLMC':
                                # 处理地类名称
                                if value_str:
                                    # 添加完整的地类名称
                                    field_keywords[col_name].append(value_str)
                                    self.logger.info(f"添加地类关键词: {value_str}")
                            else:
                                # XZQHMC等其他字段
                                if value_str:
                                    field_keywords[col_name].append(value_str)
                                    self.logger.info(f"添加关键词: {value_str}")
                        except Exception as e:
                            self.logger.warning(f"处理值 {value} 时出错: {str(e)}")
                            continue
                else:
                    self.logger.warning(f"列 {col_name} 不存在于数据集中")
            
            # 按字段顺序合并关键词
            keywords = []
            for field in target_columns:
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
        """生成数据摘要，按功能分类展示字段信息"""
        name = "青岛测试数据集"  # 使用固定名称
        
        # 字段分类定义
        field_categories = {
            '标识字段': ['FID', 'OBJECTID', 'BSM', 'YSDM', 'TBYBH', 'TBBH'],
            '地类信息字段': ['DLBM', 'DLMC', 'KCDLBM'],
            '权属信息字段': ['QSXZ', 'QSDWDM', 'QSDWMC', 'ZLDWDM', 'ZLDWMC'],
            '面积信息字段': ['TBMJ', 'KCMJ', 'TBDLMJ', 'KCXS'],
            '耕地相关字段': ['GDLX', 'GDPDJB', 'XZDWKD', 'GDDB'],
            '图斑属性字段': ['TBXHDM', 'TBXHMC', 'ZZSXDM', 'ZZSXMC'],
            '管理信息字段': ['FRDBS', 'CZCSXM', 'SJNF', 'MSSM'],
            '行政区划字段': ['XZQHDM', 'XZQHMC'],
            '空间信息字段': ['SHAPE_Leng', 'SHAPE_Area'],
            '其他字段': ['HDMC', 'BZ']
        }
        
        # 创建字段名到类型的映射
        field_types = {col['name']: col['type'] for col in columns}
        
        # 生成摘要
        summary_parts = []
        # 添加数据集总体信息
        summary_parts.append(f'{name}包含36个字段，其中：')
        
        # 添加各类字段信息
        category_parts = []
        for i, (category, fields) in enumerate(field_categories.items(), 1):
            # 过滤出当前分类中存在的字段
            existing_fields = [f for f in fields if f in field_types]
            if existing_fields:
                field_descriptions = []
                for field in existing_fields:
                    field_descriptions.append(f"{field}:{self._get_field_description(field)}")
                
                category_parts.append(f"({i}){category}共{len(existing_fields)}个：{'、'.join(field_descriptions)}")
        
        summary_parts.append('；'.join(category_parts))
        return ''.join(summary_parts) + '。'
    
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