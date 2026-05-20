package nz.eloque.foss_wallet.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nz.eloque.foss_wallet.persistence.NearbyPassesStore
import nz.eloque.foss_wallet.persistence.pass.PassRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun passRepository(): PassRepository

    fun nearbyPassesStore(): NearbyPassesStore
}
