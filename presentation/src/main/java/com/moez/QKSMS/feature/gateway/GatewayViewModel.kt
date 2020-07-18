package com.moez.QKSMS.feature.gateway

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.moez.QKSMS.common.base.QkViewModel
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.withLatestFrom
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject

class GatewayViewModel @Inject constructor(
    private val context: Context
) : QkViewModel<GatewayView, GatewayState>(GatewayState()) {

    init {
        newState { GatewayState(getAddressList(), isServiceRunning()) }
    }

    override fun bindView(view: GatewayView) {
        super.bindView(view)

        view.stateClickIntent
                .withLatestFrom(state.map { it.running }) { _, running -> running }
                .autoDisposable(view.scope())
                .subscribe { running ->
                    val intent = Intent(context, GatewayService::class.java)
                    when (running) {
                        true -> context.stopService(intent)
                        false -> ContextCompat.startForegroundService(context, intent)
                    }
                    newState { copy(running = !running) }
                }
    }

    private fun getAddressList(): List<String> {
        val result = mutableListOf<String>()
        NetworkInterface.getNetworkInterfaces().toList().forEach { networkInterface ->
            networkInterface.inetAddresses.toList().forEach { address ->
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    result.add("http:/${address}:${GatewayService.DEFAULT_PORT}")
                }
            }
        }
        return result
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (GatewayService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

}