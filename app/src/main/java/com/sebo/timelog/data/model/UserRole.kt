package com.sebo.timelog.data.model

enum class UserRole {
    ADMIN,      // Kann alles sehen und editieren
    TECHNICIAN, // Kann nur eigene Projekte sehen (Monteur)
    CUSTOMER,   // Begrenzte Sicht auf zugewiesene Projekte
    NEW         // Neue Benutzer, Rolle noch nicht zugewiesen
}

fun String.toUserRole(): UserRole {
    return when (trim().uppercase()) {
        "ADMIN" -> UserRole.ADMIN
        "TECHNICIAN", "MONTEUR" -> UserRole.TECHNICIAN
        "CUSTOMER", "COSTUMER", "KUNDE" -> UserRole.CUSTOMER
        "NEW" -> UserRole.NEW
        else -> UserRole.NEW
    }
}

