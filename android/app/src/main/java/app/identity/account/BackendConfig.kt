package app.identity.account

/**
 * Where the accounts live (M4.2, D-037). Both values are PUBLIC by design: the project's address and
 * its publishable "anon" key, which can do only what the database's Row Level Security allows.
 * Never put the service-role / secret key, a database password or an access token here: this
 * repository is public. Same values as backend/project.properties.
 */
internal object BackendConfig {
    const val URL = "https://enrljisgutpgtnyvkmdb.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVucmxqaXNndXRwZ3RueXZrbWRiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTMyOTg1OTMsImV4cCI6MjEwODg3NDU5M30.aQj_bvlCc2HOgSnLBbxXs1WufSjbb_ZqlYQxR1MF354"
}
