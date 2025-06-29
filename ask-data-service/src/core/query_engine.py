"""
查询引擎模块

提供智能查询功能
"""

import os
import pandasai as pai
from .data_analyzer import DataAnalyzer
from ..utils.chart_patch import apply_chart_patch
from ..utils.string_response_patch import apply_string_response_patch
from ..utils.schema_database import SchemaDatabase
from ..utils.logger import get_logger, LogContext
from ..utils.sql_interceptor import get_sql_interceptor, start_sql_interception, stop_sql_interception, clear_current_query

# 配置matplotlib不显示弹窗
import matplotlib
matplotlib.use('Agg')  # 使用非交互式后端，不显示图表窗口
import matplotlib.pyplot as plt
plt.ioff()  # 关闭交互模式

# 抑制matplotlib的各种警告
import warnings
warnings.filterwarnings('ignore', category=UserWarning, module='matplotlib')
warnings.filterwarnings('ignore', message='.*Using categorical units to plot a list of strings.*')
warnings.filterwarnings('ignore', message='.*tight_layout.*')

# 设置matplotlib日志级别
import logging
logging.getLogger('matplotlib').setLevel(logging.ERROR)

class QueryEngine:
    """智能查询引擎"""
    
    def __init__(self, llm, data_path=None, dataframe=None, use_semantic_layer=True, 
                 settings=None, dataset_id=None, schema_config=None, db=None):
        """
        初始化查询引擎
        
        Args:
            llm: 语言模型实例
            data_path: 数据文件路径
            dataframe: 已加载的DataFrame
            use_semantic_layer: 是否使用语义层
            settings: 系统配置对象
            dataset_id: 数据集ID
            schema_config: 预加载的schema配置
            db: 可选的数据库连接实例，用于复用连接
        """
        self.logger = get_logger(__name__)
        self.llm = llm
        self.analyzer = DataAnalyzer()
        self.use_semantic_layer = use_semantic_layer
        self.data_path = data_path
        self.settings = settings
        self.dataset_id = dataset_id
        self.schema_config = schema_config
        self.db = db  # 保存数据库连接引用
        
        # 配置PandasAI详细日志
        self._configure_pandasai_logging()
        
        # 配置PandasAI
        pai.config.set({
            "llm": llm,
            "verbose": False,  # 禁用控制台输出，只使用文件日志
            "enable_cache": True,
            "cache_path": "cache",
            "custom_whitelisted_dependencies": ["matplotlib", "seaborn", "plotly", "kaleido"],
            "enforce_privacy": False,  # 允许记录详细日志
            "log_server_url": None,   # 禁用远程日志服务器
            "advanced_reasoning": True,  # 启用高级推理日志
            "save_logs": True  # 启用日志保存
        })
        
        # 加载数据
        if use_semantic_layer and data_path:
            self.logger.info("🎯 启用语义层模式")
            
            # 如果有dataset_id，使用数据库中的配置
            if dataset_id:
                self.logger.info(f"✅ 使用数据库配置: {dataset_id}")
                # 使用增强的语义DataFrame创建方法（包含transformations支持）
                self.df = self.analyzer.create_enhanced_semantic_dataframe(data_path, dataset_id, self.db)
            else:
                self.logger.info("📋 自动生成schema配置")
                self.df = self.analyzer.create_semantic_dataframe(data_path)
        else:
            # 传统模式
            self.logger.info("📊 使用传统模式")
            if dataframe is not None:
                self.df = pai.DataFrame(dataframe)
            elif data_path:
                raw_df = self.analyzer.load_data(data_path)
                self.df = pai.DataFrame(raw_df)
            else:
                raise ValueError("必须提供data_path或dataframe参数")
        
        # 分析数据结构
        self.data_info = self.analyzer.analyze_structure(self.df)
        
        # 初始化 query_id
        self.current_query_id = None
        
        # 应用图表文件名修补
        apply_chart_patch()
        
        # 应用String响应补丁
        apply_string_response_patch()
        
        # 初始化SQL拦截器
        self.sql_interceptor = get_sql_interceptor()
    
    def _configure_pandasai_logging(self):
        """配置PandasAI详细日志"""
        import logging
        
        # 确保logs目录存在
        logs_dir = 'logs'
        if not os.path.exists(logs_dir):
            os.makedirs(logs_dir)
            self.logger.info(f"📁 创建日志目录: {logs_dir}")
        
        # PandasAI日志文件路径
        pandasai_log_path = os.path.join(logs_dir, 'pandasai.log')
        
        # 修补PandasAI内置Logger的日志文件路径
        try:
            from pandasai.helpers.logger import Logger
            from pandasai.helpers.path import find_closest
            
            # 保存原始的find_closest函数
            original_find_closest = find_closest
            
            # 创建自定义的find_closest函数
            def custom_find_closest(filename):
                if filename == "pandasai.log":
                    return pandasai_log_path
                return original_find_closest(filename)
            
            # 替换find_closest函数
            import pandasai.helpers.path
            pandasai.helpers.path.find_closest = custom_find_closest
            
            # 也替换logger模块中的引用
            import pandasai.helpers.logger
            pandasai.helpers.logger.find_closest = custom_find_closest
            
            self.logger.debug("🔧 已修补PandasAI内置Logger路径")
            
        except Exception as e:
            self.logger.warning(f"修补PandasAI日志路径失败: {e}")
        
        # 获取PandasAI相关的logger
        pandasai_loggers = [
            'pandasai',
            'pandasai.agent',
            'pandasai.smart_dataframe',
            'pandasai.helpers.query_exec_tracker',
            'pandasai.helpers.logger',
            'pandasai.pipelines',
            'pandasai.skills',
            'pandasai.smart_lake'
        ]
        
        # 为每个PandasAI logger设置DEBUG级别
        for logger_name in pandasai_loggers:
            logger = logging.getLogger(logger_name)
            logger.setLevel(logging.DEBUG)
            
            # 清除现有的handlers
            logger.handlers.clear()
            
            # 添加文件handler
            file_handler = logging.FileHandler(pandasai_log_path, encoding='utf-8')
            file_handler.setLevel(logging.DEBUG)
            
            # 设置详细的日志格式
            formatter = logging.Formatter(
                '%(asctime)s [%(levelname)s] %(name)s: %(message)s',
                datefmt='%Y-%m-%d %H:%M:%S'
            )
            file_handler.setFormatter(formatter)
            
            logger.addHandler(file_handler)
            # 防止重复日志
            logger.propagate = False
        
        self.logger.info(f"🔧 已配置PandasAI详细日志输出到 {pandasai_log_path}")
    
    def set_query_id(self, query_id):
        """
        设置当前查询的ID，用于生成图表文件名
        
        Args:
            query_id: 查询ID
        """
        self.current_query_id = query_id
        # 设置环境变量，让 PandasAI 可以访问到 query_id
        os.environ['CURRENT_QUERY_ID'] = query_id
        
    def query(self, question, query_id=None):
        """
        执行智能查询
        
        Args:
            question: 用户问题
            query_id: 查询ID，用于生成图表文件名
            
        Returns:
            查询结果
        """
        # 如果提供了 query_id，则设置它
        if query_id:
            self.set_query_id(query_id)
        # 生成增强的查询上下文
        context = self.analyzer.generate_context(self.data_info)
        
        # 构建完整的查询指令
        enhanced_query = f"""请用中文回答以下问题，一定要给出文本结果，里面有你对问题的总结，并根据结果生成图表：{question}

{context}

重要说明：
- 结果格式：result = {{"type": "string", "value": 具体值}}
- 如果是文本描述，使用"string"类型
"""
        
        try:
            # 记录查询开始
            self.logger.info(f"🔍 开始查询: {question}")
            self.logger.debug(f"📝 增强查询: {enhanced_query}")
            
            # 清空之前的SQL记录并启动拦截
            clear_current_query()
            start_sql_interception()
            
            with LogContext(self.logger, "执行PandasAI查询"):
                # 确保PandasAI日志记录到文件
                import logging
                logging.getLogger('pandasai').info(f"开始处理查询: {question}")
                
                response = self.df.chat(enhanced_query)
                
                # 记录响应类型和内容
                self.logger.debug(f"📊 响应类型: {type(response)}")
                self.logger.debug(f"📋 响应内容: {str(response)[:200]}...")
                
                # 记录执行的SQL
                executed_sqls = self.sql_interceptor.get_current_query_sqls()
                if executed_sqls:
                    self.logger.debug(f"🗂️ 执行了 {len(executed_sqls)} 个SQL查询:")
                    for i, sql_record in enumerate(executed_sqls, 1):
                        self.logger.info(f"  SQL #{i}: {sql_record['sql']}")
                else:
                    self.logger.info("📝 未检测到SQL查询执行")
            
            # 检查响应是否有效（针对String响应补丁后的逻辑）
            if self._is_valid_response(response):
                self.logger.info("✅ 查询完成")
                # 记录到PandasAI日志
                logging.getLogger('pandasai').info(f"查询成功完成: {question}")
                return response
            else:
                self.logger.warning("查询没有返回有效结果")
                logging.getLogger('pandasai').warning(f"查询无结果: {question}")
                # 由于应用了String补丁，返回错误信息字符串
                return "查询没有返回有效结果，请尝试重新表述问题"
                
        except Exception as e:
            self.logger.error(f"❌ 查询失败：{str(e)}", exc_info=True)
            # 记录到PandasAI日志
            logging.getLogger('pandasai').error(f"查询失败: {question} - 错误: {str(e)}")
            # 由于应用了String补丁，返回错误信息字符串
            return f"查询过程中出现错误：{str(e)}\n💡 建议：尝试重新表述问题或检查问题是否与数据相关"
        finally:
            # 停止SQL拦截
            stop_sql_interception()
    
    def _is_valid_response(self, response):
        """
        检查响应是否有效（安全处理String类型）
        
        Args:
            response: 查询响应
            
        Returns:
            bool: 响应是否有效
        """
        if response is None:
            return False
        
        # 现在主要处理字符串响应
        if isinstance(response, str):
            return bool(response.strip())
        
        # 如果是其他类型，使用原来的逻辑
        try:
            return bool(response) and str(response).strip()
        except (ValueError, TypeError):
            # 如果出现任何转换错误，认为响应有效
            return True
    
    def get_data_info(self):
        """获取数据信息"""
        return self.data_info
    
    def get_executed_sqls(self):
        """获取当前查询执行的SQL查询列表"""
        return self.sql_interceptor.get_current_query_sqls()
    
    def get_executed_sqls_string(self):
        """获取当前查询执行的SQL查询字符串"""
        sqls = self.sql_interceptor.get_current_query_sqls()
        if not sqls:
            return None
        
        # 如果只有一个SQL，直接返回
        if len(sqls) == 1:
            return sqls[0]["sql"]
        
        # 如果有多个SQL，用换行符连接
        return "\n\n".join([f"-- Query {sql['execution_order']}\n{sql['sql']}" for sql in sqls])
    
    def get_latest_sql(self):
        """获取最新执行的SQL"""
        return self.sql_interceptor.get_latest_sql()
    
    def print_data_summary(self):
        """打印数据摘要信息"""
        print(f"📊 数据规模：{self.data_info['shape'][0]}行 x {self.data_info['shape'][1]}列")
        
        if self.data_info['date_columns']:
            for col in self.data_info['date_columns']:
                range_key = col + '_range'
                if range_key in self.data_info:
                    print(f"📅 {col}：{self.data_info[range_key]}")
        
        print(f"🏷️ 分类字段：{len(self.data_info['categorical_info'])}个")
        for col, info in self.data_info['categorical_info'].items():
            if isinstance(info, list) and len(info) <= 5:
                print(f"   {col}: {', '.join(map(str, info))}")
            else:
                print(f"   {col}: {info}")
    
    def print_detailed_info(self):
        """打印详细数据信息"""
        print("\n📊 数据详细信息:")
        print(f"   规模: {self.data_info['shape'][0]}行 x {self.data_info['shape'][1]}列")
        print(f"   列名: {', '.join(self.data_info['columns'])}")
        
        if self.data_info['date_columns']:
            for col in self.data_info['date_columns']:
                range_key = col + '_range'
                if range_key in self.data_info:
                    print(f"   {col}: {self.data_info[range_key]}")
        
        print("\n🏷️ 分类字段信息:")
        for col, info in self.data_info['categorical_info'].items():
            if isinstance(info, list):
                print(f"   {col}: {', '.join(map(str, info))}")
            else:
                print(f"   {col}: {info}") 