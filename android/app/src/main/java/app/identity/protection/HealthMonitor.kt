package app.identity.protection

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import app.identity.filter.InterruptionLog
import app.identity.filter.PrivateDns
import app.identity.filter.ProtectionHealth

/**
 * Looks at protection from the outside every 20 seconds while it runs (M3.5): is the lookup handler
 * alive, can the filtering service be reached, is a custom Private DNS bypassing us? It publishes
 * the result for Home and writes a history entry when a new problem appears. The timer does not
 * wake a sleeping phone, and nothing but the kind of problem and its time is saved.
 */
internal class HealthMonitor(
    context: Context,
    private val tunnel: () -> DnsTunnel?,
    private val publish: (ProtectionHealth.Report) -> Unit,
) {
    private val appContext = context.applicationContext
    private val history = InterruptionStore(appContext)
    private val handler = Handler(Looper.getMainLooper())
    private var lastProblems: Set<ProtectionHealth.Problem> = emptySet()
    private val check = Runnable { tick() }

    fun start() {
        lastProblems = emptySet()
        tick()
    }

    fun stop() {
        handler.removeCallbacks(check)
    }

    private fun tick() {
        val current = tunnel()
        val report = ProtectionHealth.assess(
            ProtectionHealth.Observation(
                wantsOn = true,
                serviceRunning = current != null,
                dnsHandlerAlive = current?.handlerAlive() ?: true,
                consecutiveUpstreamFailures = current?.upstreamFailures() ?: 0,
                outsideDnsBlockedRecently = current?.outsideDnsAttemptedWithin(OUTSIDE_DNS_WINDOW_MS) ?: false,
                privateDnsBypass = PrivateDns.bypassesProtection(
                    Settings.Global.getString(appContext.contentResolver, "private_dns_mode"),
                ),
            ),
        )
        // Log a problem when it first appears, not every 20 seconds while it lasts.
        for (problem in report.problems - lastProblems) history.record(InterruptionLog.of(problem))
        lastProblems = report.problems
        publish(report)
        handler.postDelayed(check, INTERVAL_MS)
    }

    private companion object {
        const val INTERVAL_MS = 20_000L

        /** An outside encrypted-DNS attempt counts as "recent" for this long, so Home explains it while it is happening. */
        const val OUTSIDE_DNS_WINDOW_MS = 5 * 60 * 1000L
    }
}
