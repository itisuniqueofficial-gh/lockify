package com.itisuniqueofficial.lockify.features.quicktile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.itisuniqueofficial.lockify.services.AppLockManager

/** Tapping the tile clears all temporary unlocks, forcing protected apps to re-lock. */
class LockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            icon = Icon.createWithResource(this@LockTileService, android.R.drawable.ic_lock_idle_lock)
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        AppLockManager.clearAllUnlockState()
        Toast.makeText(this, "Apps locked", Toast.LENGTH_SHORT).show()
    }
}
