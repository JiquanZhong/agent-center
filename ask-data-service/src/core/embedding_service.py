"""
Embedding服务模块

支持OpenAI协议的远程embedding服务
"""

import numpy as np
from typing import List, Union, Optional
import requests
import json
from ..utils.logger import get_logger

class EmbeddingService:
    """远程embedding服务"""
    
    def __init__(self, api_key: str, base_url: str, model_name: str = "bge-large-zh-v1.5"):
        """
        初始化embedding服务
        
        Args:
            api_key: API密钥（此服务可能不需要，但保留接口兼容性）
            base_url: API基础URL
            model_name: 模型名称
        """
        self.logger = get_logger(__name__)
        self.model_name = model_name
        self.api_key = api_key
        self.base_url = base_url.rstrip('/')
        self.embeddings_url = f"{self.base_url}/embeddings"
        
        self.logger.info(f"初始化远程embedding服务 - 模型: {model_name}, URL: {base_url}")
        
        # 设置请求头
        self.headers = {
            "Content-Type": "application/json"
        }
        
        # 如果提供了API密钥，添加到头信息中（但可能不需要）
        if api_key and api_key != "dummy":
            self.headers["Authorization"] = f"Bearer {api_key}"
        
        self.logger.info("远程embedding服务初始化成功")
    
    def encode_single(self, text: str, normalize: bool = True) -> np.ndarray:
        """
        对单个文本进行向量化
        
        Args:
            text: 输入文本
            normalize: 是否对向量进行归一化
            
        Returns:
            numpy.ndarray: 文本向量
        """
        if not text or not text.strip():
            self.logger.warning("输入文本为空，返回零向量")
            return np.zeros(1024)  # 假设向量维度为1024
        
        try:
            # 准备请求数据
            data = {
                "model": self.model_name,
                "input": text.strip()
            }
            
            # 调用远程embedding API
            response = requests.post(
                self.embeddings_url,
                headers=self.headers,
                json=data,
                timeout=30
            )
            
            if response.status_code != 200:
                raise Exception(f"HTTP {response.status_code}: {response.text}")
            
            # 解析响应
            result = response.json()
            embedding = np.array(result["data"][0]["embedding"])
            
            # 如果需要归一化
            if normalize:
                norm = np.linalg.norm(embedding)
                if norm > 0:
                    embedding = embedding / norm
            
            return embedding
            
        except Exception as e:
            self.logger.error(f"远程文本向量化失败: {e}")
            self.logger.error(f"错误详情: {type(e).__name__}: {str(e)}")
            # 检查是否为服务不可用的错误
            if "502" in str(e) or "Bad Gateway" in str(e):
                self.logger.error("Embedding服务不可用 (502 Bad Gateway)，请检查服务状态")
            return np.zeros(1024)
    
    def encode_batch(self, texts: List[str], normalize: bool = True, batch_size: int = 32) -> List[np.ndarray]:
        """
        批量文本向量化
        
        Args:
            texts: 文本列表
            normalize: 是否对向量进行归一化
            batch_size: 批处理大小
            
        Returns:
            List[np.ndarray]: 向量列表
        """
        if not texts:
            return []
        
        # 过滤空文本
        valid_texts = [text.strip() if text else "" for text in texts]
        results = []
        
        try:
            # 分批处理
            for i in range(0, len(valid_texts), batch_size):
                batch_texts = valid_texts[i:i + batch_size]
                
                # 准备请求数据
                data = {
                    "model": self.model_name,
                    "input": batch_texts
                }
                
                # 调用远程embedding API
                response = requests.post(
                    self.embeddings_url,
                    headers=self.headers,
                    json=data,
                    timeout=60
                )
                
                if response.status_code != 200:
                    raise Exception(f"HTTP {response.status_code}: {response.text}")
                
                # 解析响应
                result = response.json()
                
                # 提取向量
                for item in result["data"]:
                    embedding = np.array(item["embedding"])
                    
                    # 如果需要归一化
                    if normalize:
                        norm = np.linalg.norm(embedding)
                        if norm > 0:
                            embedding = embedding / norm
                    
                    results.append(embedding)
            
            return results
            
        except Exception as e:
            self.logger.error(f"批量向量化失败: {e}")
            return [np.zeros(1024) for _ in texts]
    
    def compute_similarity(self, embedding1: np.ndarray, embedding2: np.ndarray) -> float:
        """
        计算两个向量的余弦相似度
        
        Args:
            embedding1: 向量1
            embedding2: 向量2
            
        Returns:
            float: 相似度分数 (0-1)
        """
        try:
            # 确保向量是numpy数组
            emb1 = np.array(embedding1)
            emb2 = np.array(embedding2)
            
            # 计算余弦相似度
            similarity = np.dot(emb1, emb2) / (np.linalg.norm(emb1) * np.linalg.norm(emb2))
            return float(similarity)
        except Exception as e:
            self.logger.error(f"相似度计算失败: {e}")
            return 0.0
    
    def generate_dataset_embedding(self, dataset_info: dict) -> np.ndarray:
        """
        为数据集生成综合embedding
        
        Args:
            dataset_info: 数据集信息字典
            
        Returns:
            np.ndarray: 数据集向量
        """
        try:
            self.logger.debug(f"🧠 开始生成数据集向量: {dataset_info.get('name', 'Unknown')}")
            
            # 构建数据集的综合描述文本
            text_parts = []
            
            # 数据集名称
            if dataset_info.get('name'):
                text_parts.append(f"数据集名称：{dataset_info['name']}")
                self.logger.debug(f"📝 添加名称: {dataset_info['name']}")
            
            # 数据集描述
            if dataset_info.get('description'):
                text_parts.append(f"描述：{dataset_info['description']}")
                self.logger.debug(f"📝 添加描述: {dataset_info['description'][:50]}...")
            
            # 关键词
            if dataset_info.get('keywords'):
                keywords = dataset_info['keywords']
                if isinstance(keywords, list):
                    text_parts.append(f"关键词：{', '.join(keywords)}")
                    self.logger.debug(f"📝 添加关键词列表: {len(keywords)}个")
                else:
                    text_parts.append(f"关键词：{keywords}")
                    self.logger.debug(f"📝 添加关键词字符串: {keywords}")
            
            # 业务领域
            if dataset_info.get('domain'):
                text_parts.append(f"业务领域：{dataset_info['domain']}")
                self.logger.debug(f"📝 添加业务领域: {dataset_info['domain']}")
            
            # 数据摘要
            if dataset_info.get('data_summary'):
                text_parts.append(f"数据摘要：{dataset_info['data_summary']}")
                self.logger.debug(f"📝 添加数据摘要: {dataset_info['data_summary'][:50]}...")
            
            # 列信息
            if dataset_info.get('columns_info'):
                text_parts.append(f"数据字段：{dataset_info['columns_info']}")
                self.logger.debug(f"📝 添加列信息: {dataset_info['columns_info'][:50]}...")
            
            # 合并所有文本
            combined_text = " ".join(text_parts)
            
            if not combined_text.strip():
                self.logger.warning("❌ 数据集信息为空，返回零向量")
                return np.zeros(1024)
            
            self.logger.debug(f"📝 合并文本长度: {len(combined_text)} 字符")
            self.logger.debug(f"📝 合并文本预览: {combined_text[:100]}...")
            
            # 调用向量化服务
            embedding = self.encode_single(combined_text)
            
            if embedding is not None and hasattr(embedding, 'shape'):
                self.logger.debug(f"✅ 向量生成成功: 维度={embedding.shape}")
            else:
                self.logger.warning(f"⚠️ 向量生成异常: 返回值类型={type(embedding)}")
            
            return embedding
            
        except Exception as e:
            self.logger.error(f"❌ 数据集向量生成失败: {e}")
            self.logger.error(f"❌ 错误详情: {type(e).__name__}: {str(e)}")
            import traceback
            self.logger.error(f"❌ 堆栈跟踪: {traceback.format_exc()}")
            return np.zeros(1024)
    
    def get_model_info(self) -> dict:
        """
        获取模型信息
        
        Returns:
            dict: 模型信息
        """
        return {
            "model_name": self.model_name,
            "api_base": self.base_url,
            "embedding_dim": 1024,  # 根据实际模型调整
            "service_type": "remote_openai"
        } 