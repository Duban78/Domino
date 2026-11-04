package com.example.tallerdom.ui.navigation

// nombres de las pantallas
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val MENU = "menu"
    const val UNIRSE = "unirse"
    const val ESPERA = "espera/{codigo}"
    const val JUEGO = "juego/{codigo}"
    const val RESULTADO = "resultado/{codigo}"

    fun espera(codigo: String) = "espera/$codigo"
    fun juego(codigo: String) = "juego/$codigo"
    fun resultado(codigo: String) = "resultado/$codigo"
}