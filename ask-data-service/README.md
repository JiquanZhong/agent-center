# Ask Data AI - 智能数据问答系统

🤖 一个基于PandasAI的智能数据分析和问答系统，支持中文问答和多种数据格式。

## ✨ 特性

- 🧠 **智能问答**: 基于大语言模型的自然语言数据查询
- 🎯 **语义层支持**: 基于PandasAI v3语义层，提供精确的业务语义理解
- 🔍 **自动分析**: 智能分析数据结构，自动生成语义层配置
- 🈯 **中文支持**: 完整的中文界面和问答支持
- 📊 **多格式**: 支持CSV、Excel、JSON等多种数据格式
- 🎨 **图表生成**: 支持生成中文图表和可视化
- 🔄 **重试机制**: 内置智能重试，提高查询成功率
- 💾 **缓存优化**: 支持查询缓存，提升响应速度

## 📦 项目结构

```
ask-data-ai/
├── src/                    # 源代码目录
│   ├── core/              # 核心功能模块
│   │   ├── data_analyzer.py    # 数据结构分析
│   │   ├── query_engine.py     # 查询引擎
│   │   └── llm_adapter.py      # LLM适配器
│   ├── config/            # 配置管理
│   │   ├── settings.py         # 系统设置
│   │   └── font_config.py      # 字体配置
│   ├── utils/             # 工具函数
│   └── api/               # Web API接口
├── data/                  # 数据文件目录
├── cache/                 # 缓存目录
├── .env                   # 环境配置
├── requirements.txt       # 依赖列表
└── app.py                # Web服务入口
```

## 🚀 快速开始

### 1. 环境要求

- Python 3.8+
- Windows/macOS/Linux

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 配置环境

复制并编辑`.env`文件：

```bash
# API配置
OPENAI_API_KEY=your_api_key_here
OPENAI_BASE_URL=https://api.deepseek.com/v1
OPENAI_MODEL=deepseek-chat

# 系统配置
VERBOSE=false
ENABLE_CACHE=true
CACHE_PATH=cache
DEFAULT_DATA_PATH=data/tdsc_tdgg_p_y.csv

# 服务配置
HOST=127.0.0.1
PORT=8000
RELOAD=false
```

### 4. 启动服务

```bash
python app.py
```

服务启动后：
- API文档：http://localhost:8000/docs
- ReDoc文档：http://localhost:8000/redoc

## 🔧 配置说明

### API配置

| 参数 | 说明 | 必需 |
|------|------|------|
| OPENAI_API_KEY | API密钥 | ✅ |
| OPENAI_BASE_URL | API基础URL | ✅ |
| OPENAI_MODEL | 模型名称 | ❌ |

### 系统配置

| 参数 | 说明 | 默认值 |
|------|------|--------|
| VERBOSE | 详细输出 | false |
| ENABLE_CACHE | 启用缓存 | true |
| CACHE_PATH | 缓存路径 | cache |

### 服务配置

| 参数 | 说明 | 默认值 |
|------|------|--------|
| HOST | 服务绑定地址 | 127.0.0.1 |
| PORT | 服务端口 | 8000 |
| RELOAD | 开发模式热重载 | false |

## 📊 支持的数据格式

- **CSV文件**: 自动检测日期列并优化解析
- **Excel文件**: 支持.xlsx格式
- **JSON文件**: 支持标准JSON格式

## 🎯 语义层功能

基于 [PandasAI v3语义层](https://docs.pandas-ai.com/v3/semantic-layer/new) 技术，为您的数据提供业务语义理解：

### 自动语义分析
- 🔍 **智能类型推断**: 自动识别日期、数值、分类等字段类型
- 📝 **字段描述生成**: 根据字段名和数据特征生成业务描述
- 🏷️ **别名映射**: 为字段提供业务友好的中文别名
- 📊 **分组建议**: 自动识别适合分组分析的维度字段

### 自定义语义配置
```yaml
# examples/custom_schema.yaml
path: "ask-data-ai/sales-data"
description: "销售数据集 - 包含产品销售的详细记录"

columns:
  - name: "date"
    type: "datetime"
    description: "销售日期 - 用于时间序列分析"
    alias: "交易日期"
    
  - name: "amount"
    type: "float"
    description: "销售金额 - 单位为人民币元"
    alias: "销售额"

group_by:
  - "date"
  - "product"
```

### 语义层优势
- ✅ **更准确的查询理解**: AI能更好地理解业务含义
- ✅ **自动生成业务别名**: 支持中文字段名查询
- ✅ **智能分组建议**: 自动识别可分组的维度
- ✅ **数据转换支持**: 时区转换、匿名化等
- ✅ **业务上下文增强**: 根据行业特点优化配置

## 🎨 中文支持

系统自动配置中文字体支持：

- **Windows**: SimHei, Microsoft YaHei
- **macOS**: Heiti TC, Arial Unicode MS
- **Linux**: WenQuanYi Micro Hei

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License