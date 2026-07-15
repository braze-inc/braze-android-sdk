package com.appboy.sample.qa.pushunregister

enum class PushUnregisterQaTargetApi {
    UnregisterPush,
    Logout,
    ;

    companion object {
        fun fromStoredValue(value: String?): PushUnregisterQaTargetApi = entries.find { it.name == value } ?: UnregisterPush
    }
}
