package com.example.tallerdom.model

enum class EstadoPartida { ESPERANDO, EN_JUEGO, TERMINADA }


data class Partida(
    val codigo: String = "",
    val estado: EstadoPartida = EstadoPartida.ESPERANDO,
    val jugadores: List<Jugador> = emptyList(),
    val tablero: Tablero = Tablero(),
    val pozo: List<Ficha> = emptyList(),
    val turnoUid: String = "",
    val ronda: Int = 1,
    val maxRondas: Int = 20,
    val pasesSeguidos: Int = 0,
    val ganadorUid: String? = null,
    // lo usa el portal
    val portalFila: Int = -1,
    val portalColumna: Int = -1
) {
    fun jugadorPorUid(uid: String): Jugador? = jugadores.firstOrNull { it.uid == uid }

    fun jugadorEnTurno(): Jugador? = jugadorPorUid(turnoUid)
}