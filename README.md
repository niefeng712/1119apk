# 福彩体彩资讯与走势 APK 工程（9寸–14寸平板自适应版）

本项目为面向中国大陆福彩（双色球、福彩3D、七乐彩）与体彩（超级大乐透、排列三）的 Android 客户端，已接入公开免费开奖数据接口与官方开奖直播网页，并针对 **9寸–14寸平板电脑** 与手机做了多断点响应式自适应设计。

## 核心特性
1. **已接入公开免费数据接口（自动拉取 + 离线兜底）**：
   - 中国福彩网公开接口：`https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice`
   - 中国体彩网公开接口：`https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry`
   - 支持异步加载最新开奖与历史 30 期数据；若因网络环境限制请求失败，自动回退到本地示例数据并提供一键重试。
2. **已填入官方开奖直播入口**：
   - 中国福彩官方直播页：`https://www.cwl.gov.cn/html5/ygkj/kjzb/`（及新浪彩票福彩直播 `https://lotto.sina.cn/video/fcopen/`）
   - 中国体彩官方直播页：`https://m.lottery.gov.cn/kjzb/`（及新浪彩票体彩直播 `https://lotto.sina.cn/video/tcopen`）
   - 内置 `WebView` 直播播放模式与 `ExoPlayer` 直连 `.m3u8`/`.mp4` 流媒体模式切换。
3. **9寸–14寸平板自适应布局**：
   - **导航栏**：手机（`< 600dp`）使用底部 `NavigationBar`；9寸–10寸平板（`600dp–839dp`）使用左侧 `NavigationRail`；10寸–14寸宽屏平板（`>= 840dp`）使用常驻侧边导航栏 `PermanentNavigationDrawer`。
   - **开奖大厅**：平板下自动切换为 2–3 列自适应网格卡片。
   - **开奖直播**：平板横屏下自动切换为“左侧 65% 大屏播放器 + 右侧 35% 频道与开奖时间表”双栏布局。
   - **历史开奖**：10寸–14寸宽屏下自动切换为“左侧列表 + 右侧选中期号详情”Master-Detail 双栏布局。
   - **走势图**：使用 Compose `Canvas` 原生绘制网格与折线，大屏下自动放大单元格并减少横向滚动。

## 编译生成 APK 步骤
1. 解压压缩包并用 **Android Studio** 打开 `lottery_tablet_apk_project` 文件夹。
2. 等待 Gradle 同步完成。
3. 点击菜单 **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**，生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。
