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
        val result = LotteryRepository.fetchLatestAll()
        result.draws.forEach { draw ->
            LotteryRepository.updateFastLatest(draw)
        }
        return Result.success()
    }
}
