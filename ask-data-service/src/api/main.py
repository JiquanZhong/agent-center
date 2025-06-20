"""
Ask Data AI - FastAPI主应用

提供完整的RESTful API服务
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from contextlib import asynccontextmanager
import uvicorn

from .routers import query, system, datasets, columns, transformations, tree_nodes
from .models import ErrorResponse
from ..config.settings import Settings
from ..utils.logger import get_logger, setup_api_logging

# 应用生命周期管理
@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用启动和关闭事件处理"""
    # 初始化日志系统
    setup_api_logging(debug=False)
    logger = get_logger("ask_data_ai.startup")
    
    # 启动时
    logger.info("🚀 Ask Data AI 服务启动中...")
    try:
        # 验证配置
        settings = Settings()
        logger.info("✅ 配置加载成功")
        logger.info(f"📊 数据库连接: {settings.postgres_host}:{settings.postgres_port}/{settings.postgres_db}")
        logger.info(f"🤖 LLM模型: {getattr(settings, 'llm_model', 'unknown')}")
        
        # 测试数据库连接
        from ..utils.schema_database import SchemaDatabase
        db = SchemaDatabase(settings)
        db.test_connection()
        logger.info("✅ 数据库连接测试成功")
        
    except Exception as e:
        logger.error(f"❌ 启动检查失败: {e}", exc_info=True)
        logger.warning("⚠️  服务将继续启动，但可能存在配置问题")
    
    logger.info("✅ Ask Data AI 服务启动完成")
    logger.info("📖 API文档地址: http://localhost:8000/docs")
    
    yield
    
    # 关闭时
    logger.info("👋 Ask Data AI 服务正在关闭...")

# 创建FastAPI应用
app = FastAPI(
    title="Ask Data AI",
    description="""
## 🤖 智能数据问答API服务

Ask Data AI 提供简洁而强大的自然语言数据查询能力：

- 🔍 **智能查询**: 只需数据集ID + 问题，即可获得答案
- 📊 **数据集管理**: 简单的数据集注册和查询
- 🗄️ **语义层支持**: 自动schema生成和管理
- ⚙️ **系统监控**: 服务状态监控

### 🔧 技术栈

- **后端框架**: FastAPI
- **数据库**: PostgreSQL  
- **AI模型**: 支持OpenAI兼容API
- **数据处理**: PandasAI + Pandas
    """,
    version="1.0.0",
    contact={
        "name": "Zhong Jiquan",
        "email": "zhongjq@diit.cn",
    },
    lifespan=lifespan
)

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应该限制具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 添加路由
app.include_router(datasets.router, prefix="/api/ask-data")
app.include_router(columns.router, prefix="/api/ask-data")
app.include_router(transformations.router, prefix="/api/ask-data")
app.include_router(tree_nodes.router, prefix="/api/ask-data")
app.include_router(query.router, prefix="/api/ask-data")
app.include_router(system.router, prefix="/api/ask-data")

# 添加智能查询路由
from .routers import smart_query
app.include_router(smart_query.router, prefix="/api/ask-data")

# 根路径重定向到文档
@app.get("/", include_in_schema=False)
async def root():
    """重定向到API文档"""
    return RedirectResponse(url="/docs")

# 全局异常处理
@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """全局异常处理器"""
    logger = get_logger("ask_data_ai.exception")
    logger.error(f"未处理异常: {str(exc)}", exc_info=True)
    
    from fastapi.responses import JSONResponse
    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "message": "服务器内部错误",
            "error_code": "INTERNAL_ERROR",
            "details": {"error": str(exc)}
        }
    )

# 启动函数
def start_server(host: str = "0.0.0.0", port: int = 8000, reload: bool = False):
    """启动Web服务器"""
    # 设置调试模式日志
    setup_api_logging(debug=reload)
    logger = get_logger("ask_data_ai.server")
    
    logger.info(f"🌐 启动Ask Data AI Web服务...")
    logger.info(f"📍 服务地址: http://{host}:{port}")
    logger.info(f"📖 API文档: http://{host}:{port}/docs")
    logger.info(f"🔄 热重载: {'启用' if reload else '禁用'}")
    
    uvicorn.run(
        "src.api.main:app",
        host=host,
        port=port,
        reload=reload,
        access_log=True,
        log_level="info"
    )

if __name__ == "__main__":
    start_server(reload=True) 