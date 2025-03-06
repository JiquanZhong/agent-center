create table public.apps
(
    id                      uuid         default uuid_generate_v4()          not null
        constraint app_pkey
            primary key,
    tenant_id               uuid                                             not null,
    name                    varchar(255)                                     not null,
    mode                    varchar(255)                                     not null,
    icon                    varchar(255),
    icon_background         varchar(255),
    app_model_config_id     uuid,
    status                  varchar(255) default 'normal'::character varying not null,
    enable_site             boolean                                          not null,
    enable_api              boolean                                          not null,
    api_rpm                 integer      default 0                           not null,
    api_rph                 integer      default 0                           not null,
    is_demo                 boolean      default false                       not null,
    is_public               boolean      default false                       not null,
    created_at              timestamp    default CURRENT_TIMESTAMP(0)        not null,
    updated_at              timestamp    default CURRENT_TIMESTAMP(0)        not null,
    is_universal            boolean      default false                       not null,
    workflow_id             uuid,
    description             text         default ''::character varying       not null,
    tracing                 text,
    max_active_requests     integer,
    icon_type               varchar(255),
    created_by              uuid,
    updated_by              uuid,
    use_icon_as_answer_icon boolean      default false                       not null
);

alter table public.apps
    owner to postgres;

create index app_tenant_id_idx
    on public.apps (tenant_id);

create table public.app_model_configs
(
    id                               uuid         default uuid_generate_v4()          not null
        constraint app_model_config_pkey
            primary key,
    app_id                           uuid                                             not null,
    provider                         varchar(255),
    model_id                         varchar(255),
    configs                          json,
    created_at                       timestamp    default CURRENT_TIMESTAMP(0)        not null,
    updated_at                       timestamp    default CURRENT_TIMESTAMP(0)        not null,
    opening_statement                text,
    suggested_questions              text,
    suggested_questions_after_answer text,
    more_like_this                   text,
    model                            text,
    user_input_form                  text,
    pre_prompt                       text,
    agent_mode                       text,
    speech_to_text                   text,
    sensitive_word_avoidance         text,
    retriever_resource               text,
    dataset_query_variable           varchar(255),
    prompt_type                      varchar(255) default 'simple'::character varying not null,
    chat_prompt_config               text,
    completion_prompt_config         text,
    dataset_configs                  text,
    external_data_tools              text,
    file_upload                      text,
    text_to_speech                   text,
    created_by                       uuid,
    updated_by                       uuid
);

alter table public.app_model_configs
    owner to postgres;

create index app_app_id_idx
    on public.app_model_configs (app_id);

create table public.conversations
(
    id                        uuid      default uuid_generate_v4()   not null
        constraint conversation_pkey
            primary key,
    app_id                    uuid                                   not null,
    app_model_config_id       uuid,
    model_provider            varchar(255),
    override_model_configs    text,
    model_id                  varchar(255),
    mode                      varchar(255)                           not null,
    name                      varchar(255)                           not null,
    summary                   text,
    inputs                    json                                   not null,
    introduction              text,
    system_instruction        text,
    system_instruction_tokens integer   default 0                    not null,
    status                    varchar(255)                           not null,
    from_source               varchar(255)                           not null,
    from_end_user_id          uuid,
    from_account_id           uuid,
    read_at                   timestamp,
    read_account_id           uuid,
    created_at                timestamp default CURRENT_TIMESTAMP(0) not null,
    updated_at                timestamp default CURRENT_TIMESTAMP(0) not null,
    is_deleted                boolean   default false                not null,
    invoke_from               varchar(255),
    dialogue_count            integer   default 0                    not null
);

alter table public.conversations
    owner to postgres;

create index conversation_app_from_user_idx
    on public.conversations (app_id, from_source, from_end_user_id);

create table public.message_feedbacks (
                                          id uuid primary key not null default uuid_generate_v4(),
                                          app_id uuid not null,
                                          conversation_id uuid not null,
                                          message_id uuid not null,
                                          rating character varying(255) not null,
                                          content text,
                                          from_source character varying(255) not null,
                                          from_end_user_id uuid,
                                          from_account_id uuid,
                                          created_at timestamp without time zone not null default CURRENT_TIMESTAMP(0),
                                          updated_at timestamp without time zone not null default CURRENT_TIMESTAMP(0)
);
create index message_feedback_app_idx on message_feedbacks using btree (app_id);
create index message_feedback_conversation_idx on message_feedbacks using btree (conversation_id, from_source, rating);
create index message_feedback_message_idx on message_feedbacks using btree (message_id, from_source);

create table public.message_files (
                                      id uuid primary key not null default uuid_generate_v4(),
                                      message_id uuid not null,
                                      type character varying(255) not null,
                                      transfer_method character varying(255) not null,
                                      url text,
                                      upload_file_id uuid,
                                      created_by_role character varying(255) not null,
                                      created_by uuid not null,
                                      created_at timestamp without time zone not null default CURRENT_TIMESTAMP(0),
                                      belongs_to character varying(255)
);
create index message_file_created_by_idx on message_files using btree (created_by);
create index message_file_message_idx on message_files using btree (message_id);

create table public.messages (
                                 id uuid primary key not null default uuid_generate_v4(),
                                 app_id uuid not null,
                                 model_provider character varying(255),
                                 model_id character varying(255),
                                 override_model_configs text,
                                 conversation_id uuid not null,
                                 inputs json not null,
                                 query text not null,
                                 message json not null,
                                 message_tokens integer not null default 0,
                                 message_unit_price numeric(10,4) not null,
                                 answer text not null,
                                 answer_tokens integer not null default 0,
                                 answer_unit_price numeric(10,4) not null,
                                 provider_response_latency double precision not null default 0,
                                 total_price numeric(10,7),
                                 currency character varying(255) not null,
                                 from_source character varying(255) not null,
                                 from_end_user_id uuid,
                                 from_account_id uuid,
                                 created_at timestamp without time zone not null default CURRENT_TIMESTAMP(0),
                                 updated_at timestamp without time zone not null default CURRENT_TIMESTAMP(0),
                                 agent_based boolean not null default false,
                                 message_price_unit numeric(10,7) not null default 0.001,
                                 answer_price_unit numeric(10,7) not null default 0.001,
                                 workflow_run_id uuid,
                                 status character varying(255) not null default 'normal',
                                 error text,
                                 message_metadata text,
                                 invoke_from character varying(255),
                                 parent_message_id uuid
);
create index message_account_idx on messages using btree (app_id, from_source, from_account_id);
create index message_app_id_idx on messages using btree (app_id, created_at);
create index message_conversation_id_idx on messages using btree (conversation_id);
create index message_end_user_idx on messages using btree (app_id, from_source, from_end_user_id);
create index message_workflow_run_id_idx on messages using btree (conversation_id, workflow_run_id);
create index message_created_at_idx on messages using btree (created_at);

create table public.provider_model_settings (
                                                id uuid primary key not null default uuid_generate_v4(),
                                                tenant_id uuid not null,
                                                provider_name character varying(255) not null,
                                                model_name character varying(255) not null,
                                                model_type character varying(40) not null,
                                                enabled boolean not null default true,
                                                load_balancing_enabled boolean not null default false,
                                                created_at timestamp without time zone not null default CURRENT_TIMESTAMP(0),
                                                updated_at timestamp without time zone not null default CURRENT_TIMESTAMP(0)
);
create index provider_model_setting_tenant_provider_model_idx on provider_model_settings using btree (tenant_id, provider_name, model_type);

create table public.provider_models (
                                        id uuid primary key not null default uuid_generate_v4(),
                                        tenant_id uuid not null,
                                        provider_name character varying(255) not null,
                                        model_name character varying(255) not null,
                                        model_type character varying(40) not null,
                                        encrypted_config text,
                                        is_valid boolean not null default false,
                                        created_at timestamp without time zone not null default CURRENT_TIMESTAMP(0),
                                        updated_at timestamp without time zone not null default CURRENT_TIMESTAMP(0)
);
create unique index unique_provider_model_name on provider_models using btree (tenant_id, provider_name, model_name, model_type);
create index provider_model_tenant_id_provider_idx on provider_models using btree (tenant_id, provider_name);

