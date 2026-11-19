package com.example.lotteryinfo.data

object LotteryConfig {
    /**
     * 中国福彩网公开开奖查询接口（JSON）
     */
    const val CWL_DRAW_API =
        "https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice"

    /**
     * 中国体彩网公开历史开奖接口（JSON）
     */
    const val SPORTTERY_DRAW_API =
        "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry"

    /**
     * 官方及门户开奖直播网页（支持实时直播与往期视频回放）
     */
    const val FC_OFFICIAL_LIVE_WEB = "https://www.cwl.gov.cn/html5/ygkj/kjzb/"
    const val TC_OFFICIAL_LIVE_WEB = "https://m.lottery.gov.cn/kjzb/"
    const val FC_SINA_LIVE_WEB = "https://lotto.sina.cn/video/fcopen/"
    const val TC_SINA_LIVE_WEB = "https://lotto.sina.cn/video/tcopen"
}
