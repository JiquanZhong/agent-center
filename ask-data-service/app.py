#!/usr/bin/env python3
"""
Ask Data AI - Web API服务启动脚本

启动FastAPI Web服务器
"""

import sys
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from src.api.main import start_server

def main():
    """主函数"""
    # host = os.getenv("DOCKER_HOST_IP", "127.0.0.1")
    # port = int(os.getenv("ASK_DATA_PORT", "8000"))
    host = "0.0.0.0"
    port = 8000
    reload = os.getenv("RELOAD", "false").lower() == "true"
    
    print("🤖 Ask Data AI - 智能数据问答Web API服务")
    print("=" * 50)
    
    try:
        # 初始化日志系统
        from src.utils.logger import setup_api_logging, get_logger
        setup_api_logging(debug=reload)
        logger = get_logger("ask_data_ai.app")
        
        # 验证环境配置
        from src.config.settings import Settings
        settings = Settings()
        logger.info("✅ 配置验证成功")
        
        # 启动服务器
        start_server(
            host=host,
            port=port,
            reload=reload
        )
        
    except KeyboardInterrupt:
        if 'logger' in locals():
            logger.info("\n👋 服务已停止")
        else:
            print("\n👋 服务已停止")
    except Exception as e:
        if 'logger' in locals():
            logger.error(f"❌ 服务启动失败: {e}", exc_info=True)
        else:
            print(f"❌ 服务启动失败: {e}")
        print("请检查配置文件和环境变量设置")
        sys.exit(1)

if __name__ == "__main__":
    main() 