var appsConfig = [
  {
    appId: '9bdd4fac-e22e-4bc1-8aae-36c7621f667b',
    themeCssUrl: './lib/chatTheme.css',
    chatConfig: {
      brand: {
        logo: './img/logo.png',
        title: 'AI中心',
        isShow: true,
      },
      robot: {
        avatar: './img/robot.png',
        isShow: true,
      },
      user: {
        avatar: '',
      },
      systemFirstComponent:
        '<div class="systemFirstComponent-chat-box">\n' +
        '    <div class="systemFirstComponent-logo">\n' +
        '        <img src="./img/logo.png" alt="DeepSeek Logo">\n' +
        '    </div>\n' +
        '    <div class="systemFirstComponent-message">\n' +
        '        您好！我是山东“统一平台”感知中心AI助手，小慧\n' +
        '       \n' +
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
        url: window.location.origin + '/chat-robot',
        apiKey: 'app-Ug8kHQck7eFWEQ9I3WFcNk7J',
        user: '',
        compony: '大家好',
      },
    },
  },
]
var houtuIp = 'http://localhost:5173/'
