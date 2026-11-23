package com.example.lotteryinfo.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lotteryinfo.model.LotteryType

class LotterySyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        LotteryRepository.init(applicationContext)
        if (LotteryType.entries.all { LotteryRepository.isDoneForToday(it) }) {
            return Result.success()
        }
        // fetchHistoryByType 内部已把官方最新一期写入缓存；
        // 这里不再逐条 updateFastLatest，避免网络失败时把离线示例数据写进缓存。
        LotteryRepository.fetchLatestAll()
        return Result.success()
    }
}
