create table public.htyy_users
(
    id            varchar      not null
        primary key,
    username      varchar(50)  not null,
    email         varchar(100) not null,
    password_hash text         not null,
    avatar_url    text,
    role          varchar(20) default 'user'::character varying,
    created_at    timestamp   default CURRENT_TIMESTAMP
);

comment on table public.htyy_users is '用户表';

comment on column public.htyy_users.id is '用户ID';

comment on column public.htyy_users.username is '用户名';

comment on column public.htyy_users.email is '邮箱';

comment on column public.htyy_users.password_hash is '密码哈希';

comment on column public.htyy_users.avatar_url is '头像URL';

comment on column public.htyy_users.role is '用户角色（user/admin）';

comment on column public.htyy_users.created_at is '创建时间';

create table public.htyy_conversations
(
    id         varchar not null
        primary key,
    user_id    varchar,
    title      varchar(255),
    created_at timestamp default CURRENT_TIMESTAMP
);

comment on table public.htyy_conversations is '对话表';

comment on column public.htyy_conversations.id is '对话ID';

comment on column public.htyy_conversations.user_id is '用户ID';

comment on column public.htyy_conversations.title is '对话标题';

comment on column public.htyy_conversations.created_at is '创建时间';

create table public.htyy_messages
(
    id              varchar not null
        primary key,
    conversation_id varchar,
    sender          varchar(20)
        constraint htyy_messages_sender_check
            check ((sender)::text = ANY ((ARRAY ['user'::character varying, 'ai'::character varying])::text[])),
    ai_name         varchar(20),
    content         text    not null,
    message_index   integer not null,
    created_at      timestamp default CURRENT_TIMESTAMP
);

comment on table public.htyy_messages is '消息表';

comment on column public.htyy_messages.id is '消息ID';

comment on column public.htyy_messages.conversation_id is '所属对话ID';

comment on column public.htyy_messages.sender is '发送者类型（user/ai）';

comment on column public.htyy_messages.ai_name is 'AI 名称（如果适用）';

comment on column public.htyy_messages.content is '消息内容';

comment on column public.htyy_messages.message_index is '消息索引';

comment on column public.htyy_messages.created_at is '创建时间';

create table public.htyy_knowledge_base
(
    id         varchar      not null
        primary key,
    title      varchar(255) not null,
    content    text         not null,
    created_at timestamp default CURRENT_TIMESTAMP
);

comment on table public.htyy_knowledge_base is '知识库表';

comment on column public.htyy_knowledge_base.id is '知识ID';

comment on column public.htyy_knowledge_base.title is '知识标题';

comment on column public.htyy_knowledge_base.content is '知识内容';

comment on column public.htyy_knowledge_base.created_at is '创建时间';

create table public.htyy_favorites
(
    id              varchar not null
        primary key,
    user_id         varchar,
    conversation_id varchar,
    created_at      timestamp default CURRENT_TIMESTAMP
);

comment on table public.htyy_favorites is '收藏表';

comment on column public.htyy_favorites.id is '收藏ID';

comment on column public.htyy_favorites.user_id is '用户ID';

comment on column public.htyy_favorites.conversation_id is '收藏的对话ID';

comment on column public.htyy_favorites.created_at is '创建时间';

create table public.htyy_logs
(
    id         varchar not null
        primary key,
    user_id    varchar,
    action     text    not null,
    created_at timestamp default CURRENT_TIMESTAMP
);

comment on table public.htyy_logs is '日志表';

comment on column public.htyy_logs.id is '日志ID';

comment on column public.htyy_logs.user_id is '用户ID';

comment on column public.htyy_logs.action is '用户操作内容';

comment on column public.htyy_logs.created_at is '创建时间';

create table public.htyy_message_files
(
    id         varchar not null
        primary key,
    message_id varchar,
    file_url   text    not null,
    file_type  varchar(50),
    file_size  integer,
    created_at timestamp default CURRENT_TIMESTAMP
);

comment on table public.htyy_message_files is '对话消息的文件信息表';

comment on column public.htyy_message_files.id is '文件ID';

comment on column public.htyy_message_files.message_id is '所属消息ID';

comment on column public.htyy_message_files.file_url is '文件存储URL';

comment on column public.htyy_message_files.file_type is '文件类型（如png, pdf等）';

comment on column public.htyy_message_files.file_size is '文件大小（字节）';

comment on column public.htyy_message_files.created_at is '创建时间';

create table public.htyy_models
(
    id                varchar      not null
        primary key,
    model_name        varchar(255) not null,
    api_key           text         not null,
    provider          varchar(100) not null,
    max_tokens        integer          default 4096,
    temperature       double precision default 0.7,
    top_p             double precision default 1.0,
    frequency_penalty double precision default 0.0,
    presence_penalty  double precision default 0.0,
    created_at        timestamp        default CURRENT_TIMESTAMP,
    updated_at        timestamp        default CURRENT_TIMESTAMP,
    user_id           varchar
);

comment on table public.htyy_models is '模型管理表';

comment on column public.htyy_models.id is '模型ID';

comment on column public.htyy_models.model_name is '模型名称';

comment on column public.htyy_models.api_key is 'API Key';

comment on column public.htyy_models.provider is '模型提供方（如OpenAI、Google等）';

comment on column public.htyy_models.max_tokens is '最大Token数';

comment on column public.htyy_models.temperature is '温度参数，影响随机性';

comment on column public.htyy_models.top_p is 'Top-p 采样参数';

comment on column public.htyy_models.frequency_penalty is '频率惩罚';

comment on column public.htyy_models.presence_penalty is '存在惩罚';

comment on column public.htyy_models.created_at is '创建时间';

comment on column public.htyy_models.updated_at is '更新时间';

comment on column public.htyy_models.user_id is '添加模型的用户id';

