package net.nemerosa.ontrack.service.preferences

import net.nemerosa.ontrack.json.parse
import net.nemerosa.ontrack.model.preferences.Preferences
import net.nemerosa.ontrack.model.preferences.PreferencesService
import net.nemerosa.ontrack.model.security.Account
import net.nemerosa.ontrack.repository.PreferencesRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PreferencesServiceImpl(
    private val preferencesRepository: PreferencesRepository,
) : PreferencesService {

    override fun getPreferences(account: Account): Preferences =
        preferencesRepository.getPreferences(account.id())
            ?.parse()
            ?: Preferences()

}