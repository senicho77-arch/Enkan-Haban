package com.example.enkan_haban

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform