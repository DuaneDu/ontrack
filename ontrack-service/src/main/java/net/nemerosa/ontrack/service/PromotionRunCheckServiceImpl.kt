package net.nemerosa.ontrack.service

import net.nemerosa.ontrack.extension.api.ExtensionManager
import net.nemerosa.ontrack.extension.api.PromotionRunCheckExtension
import net.nemerosa.ontrack.model.structure.PromotionRun
import net.nemerosa.ontrack.model.structure.PromotionRunCheckService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PromotionRunCheckServiceImpl(
        private val extensionManager: ExtensionManager
) : PromotionRunCheckService {

    val sortedChecks: List<PromotionRunCheckExtension> by lazy {
        extensionManager.getExtensions(PromotionRunCheckExtension::class.java)
                .toList()
                .sortedBy { it.order }
    }

    /**
     * Checks all check components in turn.
     */
    override fun checkPromotionRunCreation(promotionRun: PromotionRun) {
        sortedChecks.forEach { it.checkPromotionRunCreation(promotionRun) }
    }
}
