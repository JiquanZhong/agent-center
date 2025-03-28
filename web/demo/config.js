var appsConfig = [
  {
    appId: '9bdd4fac-e22e-4bc1-8aae-36c7621f667b',
    themeCssUrl: './theme/my-theme.css',
    chatConfig: {
      brand: {
        logo: './img/logo.png',
        title: '聊天机器人',
      },
      robot: {
        avatar: './img/robot.png',
      },
      user: {
        avatar: '',
      },
      systemFirstComponent:
        '<div class="systemFirstComponent-chat-box">\n' +
        '    <div class="systemFirstComponent-logo">\n' +
        '        <img src="./img/top.png" alt="DeepSeek Logo">\n' +
        '        DeepSeek聊天机器人\n' +
        '    </div>\n' +
        '    <div class="systemFirstComponent-message">\n' +
        '      您好！我是中国的深度求索（DeepSeek）公司开发的'+'智能助手DeepSeek-R1。\n' +
        '    </div>\n' +
        '</div>',
      messages: [
        {
          code: 'system',
          data: {
            text: '',
          },
        },
        {
          code: 'robot',
          data: {
            text: '',
          },
        },
      ],

      placeholder: '',
      sidebar: {
        customComponent: null,
      },
      apiConfig: {
        // 聊天接口
        url: 'http://192.9.100.162:9091',
        apiKey: 'app-fcw7GxJACAUbxR4hadDYBH6k',
        user: '',
      },
    },
  },
]
