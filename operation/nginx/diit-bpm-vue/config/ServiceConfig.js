window.ServiceConfig = {
    ssoServiceIp: window.location.origin, // 获取当前浏览器的ip+端口地址
    ssoUrl: window.location.origin + '/diit-bpm-web/diit-bpm-html/index.html', // 重定向地址
    sfSso: true, // 是否有单点登录,true是有。
    // 单点登录的token
    satoken: window.top.sessionStorage.getItem('satoken'),
    // 单点登录token拼接在请求头上的字段名
    satokenHeader: 'satoken',
    idLoginLock: { // 账号锁定相关接口配置（单点登录服务）
        getListUrl: '/idLoginLock/list', // 账号锁定列表
        cleanLock: '/idLoginLock/cleanLock' // 解锁接口
    },
    sfLogin: false, // 是否判断用户是否登录，通过判断sessionStorage中的userId是否有值判断是否登录
    // 表格/表单设计器字典code查询接口配置
    dicUrl: '/diit-project/dicEntry/getByDicCode',
    // 表格/表单设计时查询字典列表接口
    getAllDicUrl: '/diit-project/dic/getAll',
    // 附件预览转发路径
    fileUrl: '/diit-file',
    // kkfileview服务地址
    kkFileUrl: '/diit-fileview/',
    // kkfileview预览时，是否使用userName作为水印
    sfWatermarkTxt: true,
    // 表单运行时访问路径
    kFormUrl: '',
    // 表格运行时访问路径
    kPageUrl: '',
    // 流程预览postMessage白名单
    monitorAllowedDomains: [window.location.origin],
    // 业务系统后台转发地址
    projectUrl: '/diit-system',
    // 运维系统后台转发地址
    systemUrl: '/diit-project',
    // 工作流后台转发地址
    workflowUrl: '/diit-project',
    // 表单设计器后台转发地址
    formUrl: '/diit-project',
    // bpm平台前端代理地址
    bpmUrl: '/diit-bpm-web',
    // 双击意见内容是否唤起常用语选择弹窗
    opinionOpenCyy: true,
    setYyCode: false // 是否设置应用id
}