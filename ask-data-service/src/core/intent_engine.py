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
        
        # 提取领域
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
        
        # 关键词匹配加成
        result_keywords = result.get("keywords", [])
        if isinstance(result_keywords, list):
            keyword_matches = len(set(domain_info.get("keywords", [])) & set(result_keywords))
            if keyword_matches > 0:
                # 关键词匹配度：每个匹配词增加3%，最多15%
                keyword_boost = min(0.03 * keyword_matches, 0.15)
                boost_factor += keyword_boost
                matched_keywords = list(set(domain_info.get("keywords", [])) & set(result_keywords))
                boost_details.append(f"关键词匹配({keyword_matches}个):+{keyword_boost:.3f}")
        
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
        
        # 计算最终分数，限制boost_factor最大为1.5（即最多50%提升）
        original_boost = boost_factor
        boost_factor = min(boost_factor, 1.5)
        enhanced_score = base_score * boost_factor
        
        # 确保分数在合理范围内[0, 1]
        final_score = min(enhanced_score, 1.0)
        
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
        keywords = []
        
        # 从名称提取关键词
        name = dataset.get('name', '')
        if name:
            # 提取中文关键词
            name_keywords = re.findall(r'[\u4e00-\u9fff]+', name)
            keywords.extend([kw for kw in name_keywords if len(kw) > 1])
        
        # 从描述提取关键词
        description = dataset.get('description', '')
        if description:
            desc_keywords = re.findall(r'[\u4e00-\u9fff]+', description)
            keywords.extend([kw for kw in desc_keywords if len(kw) > 1])
        
        # 从列名提取关键词
        for col in columns:
            col_name = col.get('name', '')
            if col_name:
                col_keywords = re.findall(r'[\u4e00-\u9fff]+', col_name)
                keywords.extend([kw for kw in col_keywords if len(kw) > 1])
        
        # 去重和过滤
        stopwords = {"是", "的", "了", "在", "有", "和", "与", "及", "或", "但", "而", "数据", "信息", "记录"}
        unique_keywords = list(set([kw for kw in keywords if kw not in stopwords]))
        
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
        """生成数据摘要"""
        name = dataset.get('name', '未命名数据集')
        col_count = len(columns)
        
        # 分析列类型
        col_types = {}
        for col in columns:
            col_type = col.get('type', 'unknown')
            col_types[col_type] = col_types.get(col_type, 0) + 1
        
        type_desc = []
        if col_types.get('string', 0) > 0:
            type_desc.append(f"{col_types['string']}个文本字段")
        if col_types.get('integer', 0) > 0:
            type_desc.append(f"{col_types['integer']}个整数字段")
        if col_types.get('float', 0) > 0:
            type_desc.append(f"{col_types['float']}个数值字段")
        if col_types.get('datetime', 0) > 0:
            type_desc.append(f"{col_types['datetime']}个日期字段")
        
        summary = f"{name}包含{col_count}个字段"
        if type_desc:
            summary += f"，包括{', '.join(type_desc)}"
        
        return summary 