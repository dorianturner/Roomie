package com.example.roomie.components

import com.example.roomie.BuildConfig.SUPABASE_ANON_KEY
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://hfcaegjefutavivecpqf.supabase.co",
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Storage)
    }
}