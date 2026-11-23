# 新增大乐透票种 设计（短）

## 意图
彩票助手目前只有福彩3D、排列三、排列五。新增「超级大乐透」，开奖大厅、历史开奖、看直播都要和现有票种一样能用，并且开奖当晚走新浪秒级快速同步。

## 目标 / 非目标
- 目标：大乐透最新一期展示（前区 5 个红球 + 后区 2 个蓝球，两位数补零）、历史开奖分页、新浪快速同步、后台 WorkManager 同步。
- 非目标：走势图、选号、投注、双色球等其他彩种。
- 反目标：改动影响现有 3 个票种的显示和同步行为。

## 方案
- A：只用体彩官方接口（gameNo=85）。改动小，出号慢几分钟。
- **B（已选）**：在 A 的基础上扩展新浪 WebView 抓取脚本，识别大乐透 5+2 两位数号码，出号更快。

## 设计
1. `LotteryType` 增加 `DLT("dlt", "大乐透", TC, "每周一、三、六 21:25", 35, 12, isDigitGame=false, digitCount=0, cwlName=null, sportteryGameNo="85")`，再增加 `drawDays` 字段（周几开奖，`Calendar.DAY_OF_WEEK` 集合）。现有三个票种填每天开奖。
2. `fetchFromSporttery`：如果 `type.blueMax > 0`，前 5 个号码作为红球，后 2 个作为蓝球；其他票种的逻辑保持不变。
3. `getFallbackHistory` 补一条大乐透离线示例数据。
4. 新增 `LotteryRepository.isDoneForToday(type)`：当天不开奖的票种直接返回 true，否则返回 `isUpdatedForToday(type)`。`HomeScreen` 和 `LotterySyncWorker` 里所有 `entries.all { isUpdatedForToday }` 改用它。这样周二、四、五、日不会因为大乐透不开奖而一直轮询。
5. 新浪抓取 JS：
   - targets 增加 `{ key: 'DLT', names: ['大乐透'], count: 7, twoDigit: true }`。
   - `others` 去掉「大乐透」，并在原有 3 个 target 的 exclude 里加上「大乐透」，现有排除效果不变。
   - 号码校验：`twoDigit` 时匹配 `/^\d{2}$/`，否则沿用 `/^[0-9]$/`。
   - Kotlin 解析：DLT 要求 7 个号码，前 5 个为红球（1–35），后 2 个为蓝球（1–12），超出范围的结果直接丢弃。
6. `MainActivity`、`HistoryScreen` 的票种列表由 `LotteryType.entries` 自动生成，无需改动。

## 留给实现者
- 体彩接口在开发机被 WAF 拦截（567），gameNo=85 的返回格式只能在手机上验证。
- 本机没有 Android SDK。编译依赖 GitHub Actions：推送到 main 后会自动构建并发布 Release，同时删除旧 Release。推送前需要征得用户同意。
