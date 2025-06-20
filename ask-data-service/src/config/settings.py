"""
系统设置配置模块

管理环境变量、API配置、数据库配置等
"""

import os
from dotenv import load_dotenv

class Settings:
    """系统设置管理器"""
    
    def __init__(self, env_file=None):
        """
        初始化设置
        
        Args:
            env_file: .env文件路径，如果不指定则使用默认位置
        """
        # 加载环境变量
        if env_file:
            load_dotenv(env_file)
        else:
            # 尝试在几个常见位置找到.env文件
            possible_paths = [
                ".env",
                "../.env", 
                "../../.env",
                "config/.env"
            ]
            
            for path in possible_paths:
                if os.path.exists(path):
                    load_dotenv(path)
                    break
        
        # API配置
        self.api_key = self._get_env("OPENAI_API_KEY")
        self.base_url = self._get_env("OPENAI_BASE_URL")
        self.model_name = self._get_env("OPENAI_MODEL", "gpt-3.5-turbo")
        
        # 系统配置
        self.verbose = self._get_env("VERBOSE", "false").lower() == "true"
        self.enable_cache = self._get_env("ENABLE_CACHE", "true").lower() == "true"
        self.cache_path = self._get_env("CACHE_PATH", "cache")
        
        # 数据配置
        self.default_data_path = self._get_env("DEFAULT_DATA_PATH", "data")
        
        # PostgreSQL数据库配置
        self.postgres_host = self._get_env("POSTGRES_HOST", "localhost")
        self.postgres_port = self._get_env("POSTGRES_PORT", "5432")
        self.postgres_db = self._get_env("POSTGRES_HOUTU_DB", "houtu")
        self.postgres_user = self._get_env("POSTGRES_USER", "postgres")
        self.postgres_password = self._get_env("POSTGRES_PASSWORD")
        
        # Elasticsearch配置
        self.elasticsearch_host = self._get_env("ES_URL", "http://localhost:9200")
        self.elasticsearch_username = self._get_env("ES_USERNAME")
        self.elasticsearch_password = self._get_env("ES_PASSWORD")
        self.elasticsearch_index = self._get_env("ES_INDEX", "intent_recognition")
        
        # Embedding服务配置
        self.embedding_model_url = self._get_env("EMBEDDING_MODEL_URL", "http://localhost:9997/v1/")
        self.embedding_model = self._get_env("EMBEDDING_MODEL", "bge-large-zh-v1.5")
        
        # Reranker服务配置
        self.reranker_model_url = self._get_env("RERANKER_MODEL_URL", "http://localhost:9997/v1/")
        self.reranker_model = self._get_env("RERANKER_MODEL", "bge-reranker-large-zh-v1.5")
        
        # 验证必要配置
        self._validate()
    
    def _get_env(self, key: str, default: str = None) -> str:
        """
        获取环境变量值，处理注释和空格
        
        Args:
            key: 环境变量名
            default: 默认值
            
        Returns:
            str: 处理后的环境变量值
        """
        value = os.getenv(key, default)
        if value is None:
            return default
            
        # 移除注释和空格
        value = value.split('#')[0].strip()
        return value if value else default
    
    def _validate(self):
        """验证配置的有效性"""
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY环境变量未设置")
        
        if not self.base_url:
            raise ValueError("OPENAI_BASE_URL环境变量未设置")
        
        # PostgreSQL配置验证（可选）
        if not self.postgres_password:
            print("⚠️ 警告: POSTGRES_PASSWORD环境变量未设置，数据库功能可能不可用")
    
    def print_config(self):
        """打印当前配置"""
        print("🔧 当前系统配置:")
        print(f"   API基础URL: {self.base_url}")
        print(f"   模型名称: {self.model_name}")
        print(f"   启用缓存: {self.enable_cache}")
        print(f"   缓存路径: {self.cache_path}")
        print(f"   详细输出: {self.verbose}")
        print(f"   默认数据路径: {self.default_data_path}")
        print(f"   数据库配置: {self.postgres_host}:{self.postgres_port}/{self.postgres_db}")
        print(f"   ES配置: {self.elasticsearch_host}")
        print(f"   ES用户名: {self.elasticsearch_username}")
        print(f"   ES索引: {self.elasticsearch_index}")
        print(f"   Embedding服务: {self.embedding_model_url}")
        print(f"   Embedding模型: {self.embedding_model}")
        print(f"   Reranker服务: {self.reranker_model_url}")
        print(f"   Reranker模型: {self.reranker_model}")
    
    def get_llm_config(self):
        """获取LLM配置字典"""
        return {
            "api_token": self.api_key,
            "model": self.model_name,
            "api_base": self.base_url
        }
    
    def get_pandasai_config(self):
        """获取PandasAI配置字典"""
        return {
            "verbose": self.verbose,
            "enable_cache": self.enable_cache,
            "cache_path": self.cache_path
        }
    
    def get_postgres_config(self):
        """获取PostgreSQL配置字典"""
        return {
            "host": self.postgres_host,
            "port": self.postgres_port,
            "database": self.postgres_db,
            "user": self.postgres_user,
            "password": self.postgres_password
        }
    
    def get_elasticsearch_config(self):
        """获取Elasticsearch配置字典"""
        config = {
            "hosts": [self.elasticsearch_host]
        }
        
        # 如果有认证信息，添加到配置中
        if self.elasticsearch_username and self.elasticsearch_password:
            config["basic_auth"] = (self.elasticsearch_username, self.elasticsearch_password)
        
        return config
    
    def get_embedding_config(self):
        """获取Embedding服务配置字典"""
        return {
            "base_url": self.embedding_model_url,
            "model": self.embedding_model
        }
    
    def get_reranker_config(self):
        """获取Reranker服务配置字典"""
        return {
            "base_url": self.reranker_model_url,
            "model": self.reranker_model
        } 