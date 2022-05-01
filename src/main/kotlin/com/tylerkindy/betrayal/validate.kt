package com.tylerkindy.betrayal

fun validatePlayerName(name: String): String? {
    return if (name.length !in 1..20) {
        "Name must be between 1 and 20 characters"
    } else null
}
