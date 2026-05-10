package ru.lopon.platform

import platform.Foundation.NSUserDefaults

internal class IosPrefs {

    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    fun getString(key: String): String? = defaults.stringForKey(key)

    fun setString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
        defaults.synchronize()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            default
        }
    }

    fun setBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
        defaults.synchronize()
    }

    companion object {
        const val KEY_LAST_BLE_DEVICE_ID = "lopon_ble_last_device_id"

        fun permissionRequestedKey(permission: String): String =
            "lopon_perm_requested_$permission"
    }
}
