"""
基于Elasticsearch的向量检索服务

用于数据集的向量存储和语义搜索
"""

import json
from typing import List, Dict, Any, Optional
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk
import numpy as np
from ..utils.logger import get_logger

class VectorSearchService:
    """ES向量检索服务"""
    
    def __init__(self, es_config: Dict[str, Any], index_name: str = "intent_recognition"):
        """
        初始化ES向量检索服务
        
        Args:
            es_config: ES配置字典
                {
                    'hosts': ['localhost:9200'],
                    'username': 'elastic',
                    'password': 'password',
                    'verify_certs': False
                }
            index_name: ES索引名称
        """
        self.logger = get_logger(__name__)
        self.es_config = es_config
        self.index_name = index_name
        
        try:
            # 初始化ES客户端
            self.es = Elasticsearch(**es_config)
            self.logger.info(f"ES连接成功: {es_config.get('hosts', ['localhost:9200'])}")
            
            # 确保索引存在
            self._ensure_index_exists()
            
        except Exception as e:
            self.logger.error(f"ES连接失败: {e}")
            raise
    
    def _ensure_index_exists(self):
        """确保向量索引存在"""
        try:
            # 首先尝试获取ES集群信息以验证连接
            info = self.es.info()
            self.logger.info(f"ES集群信息: {info['version']['number']}")
            
            # 检查索引是否存在
            exists = self.es.indices.exists(index=self.index_name)
            self.logger.info(f"索引 {self.index_name} 存在状态: {exists}")
            
            if not exists:
                self.logger.info(f"创建ES索引: {self.index_name}")
                
                # 索引映射配置 - 简化版本以提高兼容性
                mapping = {
                    "mappings": {
                        "properties": {
                            "dataset_id": {"type": "keyword"},
                            "name": {"type": "text"},
                            "description": {"type": "text"},
                            "keywords": {"type": "keyword"},
                            "domain": {"type": "keyword"},
                            "data_summary": {"type": "text"},
                            "columns_info": {"type": "text"},
                            "tree_node_id": {"type": "keyword"},
                            "file_path": {"type": "keyword"},
                            "status": {"type": "keyword"},
                            "embedding": {
                                "type": "dense_vector",
                                "dims": 1024
                            },
                            "created_at": {"type": "date"},
                            "updated_at": {"type": "date"}
                        }
                    },
                    "settings": {
                        "number_of_shards": 1,
                        "number_of_replicas": 0
                    }
                }
                
                try:
                    # 使用兼容性更好的API调用方式
                    self.es.indices.create(index=self.index_name, **mapping)
                    self.logger.info("ES索引创建成功")
                except Exception as e:
                    self.logger.error(f"ES索引创建失败: {e}")
                    # 尝试使用更简单的索引结构
                    self.logger.info("尝试创建简化索引结构")
                    simple_mapping = {
                        "mappings": {
                            "properties": {
                                "dataset_id": {"type": "keyword"},
                                "name": {"type": "text"},
                                "description": {"type": "text"},
                                "embedding": {
                                    "type": "dense_vector",
                                    "dims": 1024
                                }
                            }
                        }
                    }
                    self.es.indices.create(index=self.index_name, **simple_mapping)
                    self.logger.info("简化ES索引创建成功")
        
        except Exception as e:
            self.logger.error(f"ES索引检查/创建失败: {e}")
            self.logger.error(f"错误详情: {type(e).__name__}: {str(e)}")
            # 不抛出异常，允许服务继续运行但记录错误
            self.logger.warning("ES索引初始化失败，向量搜索功能可能不可用")
    
    def vector_search(self, query_embedding: np.ndarray, size: int = 10, 
                     min_score: float = 0.5, filters: Dict[str, Any] = None) -> List[Dict[str, Any]]:
        """
        基于向量的语义搜索
        
        Args:
            query_embedding: 查询向量
            size: 返回结果数量
            min_score: 最小相似度分数
            filters: 额外过滤条件
            
        Returns:
            List[Dict]: 搜索结果列表
        """
        try:
            # 构建查询
            query = {
                "script_score": {
                    "query": {"match_all": {}},
                    "script": {
                        "source": "cosineSimilarity(params.query_vector, 'embedding') + 1.0",
                        "params": {
                            "query_vector": query_embedding.tolist() if isinstance(query_embedding, np.ndarray) else query_embedding
                        }
                    }
                }
            }
            
            # 添加过滤条件
            if filters:
                filter_clauses = []
                for key, value in filters.items():
                    if key == "status" and value:
                        filter_clauses.append({"term": {"status": value}})
                    elif key == "domain" and value:
                        filter_clauses.append({"term": {"domain": value}})
                    elif key == "tree_node_id" and value:
                        filter_clauses.append({"term": {"tree_node_id": value}})
                
                if filter_clauses:
                    query["script_score"]["query"] = {
                        "bool": {
                            "filter": filter_clauses
                        }
                    }
            
            # 执行搜索
            response = self.es.search(
                index=self.index_name,
                query=query,
                size=size,
                source_excludes=["embedding"]
            )
            
            results = []
            for hit in response['hits']['hits']:
                # 计算相似度分数（ES返回的分数需要减1）
                similarity_score = hit['_score'] - 1.0
                
                if similarity_score >= min_score:
                    result = hit['_source']
                    result['similarity_score'] = similarity_score
                    results.append(result)
            
            self.logger.debug(f"向量搜索完成: 找到 {len(results)} 个相关结果")
            return results
            
        except Exception as e:
            self.logger.error(f"向量搜索失败: {e}")
            return []
    
    def index_dataset(self, dataset_id: str, dataset_info: Dict[str, Any], embedding: np.ndarray) -> bool:
        """
        索引单个数据集
        
        Args:
            dataset_id: 数据集ID
            dataset_info: 数据集信息
            embedding: 数据集向量
            
        Returns:
            bool: 是否成功
        """
        try:
            doc = {
                "dataset_id": dataset_id,
                "name": dataset_info.get("name", ""),
                "description": dataset_info.get("description", ""),
                "keywords": dataset_info.get("keywords", []),
                "domain": dataset_info.get("domain", ""),
                "data_summary": dataset_info.get("data_summary", ""),
                "columns_info": dataset_info.get("columns_info", ""),
                "tree_node_id": dataset_info.get("tree_node_id", ""),
                "file_path": dataset_info.get("file_path", ""),
                "status": dataset_info.get("status", "active"),
                "embedding": embedding.tolist() if isinstance(embedding, np.ndarray) else embedding,
                "created_at": dataset_info.get("created_at"),
                "updated_at": dataset_info.get("updated_at")
            }
            
            response = self.es.index(
                index=self.index_name,
                id=dataset_id,
                document=doc
            )
            
            self.logger.debug(f"数据集索引成功: {dataset_id}")
            return True
            
        except Exception as e:
            self.logger.error(f"数据集索引失败 {dataset_id}: {e}")
            return False
    
    def bulk_index_datasets(self, datasets: List[Dict[str, Any]]) -> int:
        """
        批量索引数据集
        
        Args:
            datasets: 数据集列表，每个包含dataset_id, dataset_info, embedding
            
        Returns:
            int: 成功索引的数量
        """
        if not datasets:
            return 0
        
        try:
            actions = []
            for item in datasets:
                dataset_id = item["dataset_id"]
                dataset_info = item["dataset_info"]
                embedding = item["embedding"]
                
                doc = {
                    "_index": self.index_name,
                    "_id": dataset_id,
                    "_source": {
                        "dataset_id": dataset_id,
                        "name": dataset_info.get("name", ""),
                        "description": dataset_info.get("description", ""),
                        "keywords": dataset_info.get("keywords", []),
                        "domain": dataset_info.get("domain", ""),
                        "data_summary": dataset_info.get("data_summary", ""),
                        "columns_info": dataset_info.get("columns_info", ""),
                        "tree_node_id": dataset_info.get("tree_node_id", ""),
                        "file_path": dataset_info.get("file_path", ""),
                        "status": dataset_info.get("status", "active"),
                        "embedding": embedding.tolist() if isinstance(embedding, np.ndarray) else embedding,
                        "created_at": dataset_info.get("created_at"),
                        "updated_at": dataset_info.get("updated_at")
                    }
                }
                actions.append(doc)
            
            # 执行批量索引
            success_count, failed_items = bulk(self.es, actions)
            
            if failed_items:
                self.logger.warning(f"批量索引部分失败: {len(failed_items)} 个失败")
                for item in failed_items:
                    self.logger.error(f"失败项: {item}")
            
            self.logger.info(f"批量索引完成: {success_count} 个成功")
            return success_count
            
        except Exception as e:
            self.logger.error(f"批量索引失败: {e}")
            return 0
    
    def hybrid_search(self, query_text: str, query_embedding: np.ndarray, 
                     size: int = 10, text_weight: float = 0.3, vector_weight: float = 0.7,
                     filters: Dict[str, Any] = None) -> List[Dict[str, Any]]:
        """
        混合搜索：结合文本搜索和向量搜索
        
        Args:
            query_text: 查询文本
            query_embedding: 查询向量
            size: 返回结果数量
            text_weight: 文本搜索权重
            vector_weight: 向量搜索权重
            filters: 过滤条件
            
        Returns:
            List[Dict]: 搜索结果
        """
        try:
            # 构建混合查询
            query = {
                "bool": {
                    "should": [
                        # 文本搜索部分
                        {
                            "multi_match": {
                                "query": query_text,
                                "fields": ["name^2", "description", "keywords", "data_summary"],
                                "type": "best_fields",
                                "boost": text_weight
                            }
                        },
                        # 向量搜索部分
                        {
                            "script_score": {
                                "query": {"match_all": {}},
                                "script": {
                                    "source": f"(cosineSimilarity(params.query_vector, 'embedding') + 1.0) * {vector_weight}",
                                    "params": {
                                        "query_vector": query_embedding.tolist() if isinstance(query_embedding, np.ndarray) else query_embedding
                                    }
                                }
                            }
                        }
                    ]
                }
            }
            
            # 添加过滤条件
            if filters:
                filter_clauses = []
                for key, value in filters.items():
                    if key == "status" and value:
                        filter_clauses.append({"term": {"status": value}})
                    elif key == "domain" and value:
                        filter_clauses.append({"term": {"domain": value}})
                    elif key == "tree_node_id" and value:
                        filter_clauses.append({"term": {"tree_node_id": value}})
                
                if filter_clauses:
                    query["bool"]["filter"] = filter_clauses
            
            # 执行搜索
            response = self.es.search(
                index=self.index_name,
                query=query,
                size=size,
                source_excludes=["embedding"]
            )
            
            results = []
            for hit in response['hits']['hits']:
                result = hit['_source']
                result['hybrid_score'] = hit['_score']
                results.append(result)
            
            self.logger.debug(f"混合搜索完成: 找到 {len(results)} 个结果")
            return results
            
        except Exception as e:
            self.logger.error(f"混合搜索失败: {e}")
            return []
    
    def delete_dataset(self, dataset_id: str) -> bool:
        """删除数据集索引"""
        try:
            response = self.es.delete(index=self.index_name, id=dataset_id)
            self.logger.debug(f"数据集索引删除成功: {dataset_id}")
            return True
        except Exception as e:
            self.logger.error(f"数据集索引删除失败 {dataset_id}: {e}")
            return False
    
    def update_dataset(self, dataset_id: str, dataset_info: Dict[str, Any], embedding: np.ndarray = None) -> bool:
        """更新数据集索引"""
        try:
            update_body = {
                "doc": {
                    "name": dataset_info.get("name", ""),
                    "description": dataset_info.get("description", ""),
                    "keywords": dataset_info.get("keywords", []),
                    "domain": dataset_info.get("domain", ""),
                    "data_summary": dataset_info.get("data_summary", ""),
                    "columns_info": dataset_info.get("columns_info", ""),
                    "status": dataset_info.get("status", "active"),
                    "updated_at": dataset_info.get("updated_at")
                }
            }
            
            if embedding is not None:
                update_body["doc"]["embedding"] = embedding.tolist() if isinstance(embedding, np.ndarray) else embedding
            
            response = self.es.update(
                index=self.index_name,
                id=dataset_id,
                doc=update_body["doc"]
            )
            
            self.logger.debug(f"数据集索引更新成功: {dataset_id}")
            return True
            
        except Exception as e:
            self.logger.error(f"数据集索引更新失败 {dataset_id}: {e}")
            return False
    
    def get_stats(self) -> Dict[str, Any]:
        """获取索引统计信息"""
        try:
            stats = self.es.indices.stats(index=self.index_name)
            count_response = self.es.count(index=self.index_name)
            
            return {
                "index_name": self.index_name,
                "document_count": count_response["count"],
                "index_size": stats["indices"][self.index_name]["total"]["store"]["size_in_bytes"],
                "status": "healthy"
            }
        except Exception as e:
            self.logger.error(f"获取索引统计失败: {e}")
            return {
                "index_name": self.index_name,
                "status": "error",
                "error": str(e)
            } 