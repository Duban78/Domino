package com.example.tallerdom.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tallerdom.model.EstadoPartida
import com.example.tallerdom.ui.screens.EsperaScreen
import com.example.tallerdom.ui.screens.JuegoScreen
import com.example.tallerdom.ui.screens.LoginScreen
import com.example.tallerdom.ui.screens.MenuScreen
import com.example.tallerdom.ui.screens.RegistroScreen
import com.example.tallerdom.ui.screens.ResultadoScreen
import com.example.tallerdom.ui.screens.UnirseScreen
import com.example.tallerdom.viewmodel.AuthViewModel
import com.example.tallerdom.viewmodel.JuegoViewModel
import com.example.tallerdom.viewmodel.LobbyViewModel

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val lobbyVm: LobbyViewModel = viewModel()
    val juegoVm: JuegoViewModel = viewModel()
    val auth by authVm.estado.collectAsStateWithLifecycle()

    // si ya hay sesion arrancamos en el menu, si no en el login
    val inicio = remember {
        if (authVm.estado.value.usuarioUid != null) Rutas.MENU else Rutas.LOGIN
    }

    // cuando cambia la sesion (login o cerrar sesion) movemos la navegacion
    LaunchedEffect(auth.usuarioUid) {
        val destino = navController.currentDestination?.route ?: return@LaunchedEffect
        val enPantallaDeAcceso = destino == Rutas.LOGIN || destino == Rutas.REGISTRO

        if (auth.usuarioUid != null && enPantallaDeAcceso) {
            navController.navigate(Rutas.MENU) {
                popUpTo(Rutas.LOGIN) { inclusive = true }
            }
        } else if (auth.usuarioUid == null && !enPantallaDeAcceso) {
            juegoVm.detener()
            navController.navigate(Rutas.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = inicio,
        modifier = modifier
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                estado = auth,
                onIniciarSesion = authVm::iniciarSesion,
                onIrARegistro = {
                    authVm.limpiarError()
                    navController.navigate(Rutas.REGISTRO)
                }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                estado = auth,
                onRegistrar = authVm::registrar,
                onVolver = {
                    authVm.limpiarError()
                    navController.popBackStack()
                }
            )
        }

        composable(Rutas.MENU) {
            val lobby by lobbyVm.estado.collectAsStateWithLifecycle()

            MenuScreen(
                nombre = auth.nombre ?: "jugador",
                cargando = lobby.cargando,
                error = lobby.error,
                onNuevaPartida = {
                    val uid = auth.usuarioUid
                    if (uid != null) {
                        lobbyVm.crearSala(uid, auth.nombre ?: "Jugador") { codigo ->
                            navController.navigate(Rutas.espera(codigo))
                        }
                    }
                },
                onUnirse = {
                    lobbyVm.limpiarError()
                    navController.navigate(Rutas.UNIRSE)
                },
                onCerrarSesion = authVm::cerrarSesion
            )
        }

        composable(Rutas.UNIRSE) {
            val lobby by lobbyVm.estado.collectAsStateWithLifecycle()

            UnirseScreen(
                cargando = lobby.cargando,
                error = lobby.error,
                onUnirse = { codigo ->
                    val uid = auth.usuarioUid
                    if (uid != null) {
                        lobbyVm.unirse(codigo, uid, auth.nombre ?: "Jugador") { codigoSala ->
                            // popUpTo saca la pantalla de unirse del historial
                            navController.navigate(Rutas.espera(codigoSala)) {
                                popUpTo(Rutas.MENU)
                            }
                        }
                    }
                },
                onVolver = {
                    lobbyVm.limpiarError()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Rutas.ESPERA,
            arguments = listOf(navArgument("codigo") { type = NavType.StringType })
        ) { entrada ->
            val codigo = entrada.arguments?.getString("codigo") ?: ""
            val lobby by lobbyVm.estado.collectAsStateWithLifecycle()
            val miUid = auth.usuarioUid ?: ""

            // escuchamos la sala mientras estemos en esta pantalla
            DisposableEffect(codigo) {
                lobbyVm.escuchar(codigo)
                onDispose { lobbyVm.detenerEscucha() }
            }

            // cuando el anfitrion inicia, todos pasan al juego
            LaunchedEffect(lobby.partida?.estado) {
                if (lobby.partida?.estado == EstadoPartida.EN_JUEGO) {
                    navController.navigate(Rutas.juego(codigo)) {
                        popUpTo(Rutas.MENU)
                    }
                }
            }

            // si cancelaron la sala volvemos al menu
            LaunchedEffect(lobby.salaCerrada) {
                if (lobby.salaCerrada) {
                    navController.popBackStack(Rutas.MENU, false)
                }
            }

            EsperaScreen(
                codigo = codigo,
                partida = lobby.partida,
                miUid = miUid,
                error = lobby.error,
                onIniciar = { lobbyVm.iniciarPartida(codigo) },
                onSalir = {
                    lobbyVm.salirDeSala(codigo, miUid) {
                        navController.popBackStack(Rutas.MENU, false)
                    }
                }
            )
        }

        composable(
            route = Rutas.JUEGO,
            arguments = listOf(navArgument("codigo") { type = NavType.StringType })
        ) { entrada ->
            val codigo = entrada.arguments?.getString("codigo") ?: ""
            val juego by juegoVm.estado.collectAsStateWithLifecycle()
            val miUid = auth.usuarioUid ?: ""

            LaunchedEffect(codigo, miUid) {
                juegoVm.iniciar(codigo, miUid)
            }

            // cuando la partida termina (fin normal o abandono) pasamos al resultado
            // se revisa el codigo para no confundirnos con datos de una partida anterior
            val terminada = juego.partida?.codigo == codigo &&
                    juego.partida?.estado == EstadoPartida.TERMINADA
            LaunchedEffect(terminada) {
                if (terminada) {
                    navController.navigate(Rutas.resultado(codigo)) {
                        popUpTo(Rutas.MENU)
                    }
                }
            }

            JuegoScreen(
                estado = juego,
                onFichaClick = juegoVm::seleccionarFicha,
                onCasillaClick = { casilla -> juegoVm.colocarEn(casilla.fila, casilla.columna) },
                onRobar = juegoVm::robar,
                onPasar = juegoVm::pasar,
                onCancelarSeleccion = juegoVm::cancelarSeleccion,
                onAbandonar = juegoVm::abandonar
            )
        }

        composable(
            route = Rutas.RESULTADO,
            arguments = listOf(navArgument("codigo") { type = NavType.StringType })
        ) { entrada ->
            val codigo = entrada.arguments?.getString("codigo") ?: ""
            val juego by juegoVm.estado.collectAsStateWithLifecycle()

            // si alguno le da "jugar de nuevo", los dos vuelven al juego
            val reiniciada = juego.partida?.codigo == codigo &&
                    juego.partida?.estado == EstadoPartida.EN_JUEGO
            LaunchedEffect(reiniciada) {
                if (reiniciada) {
                    navController.navigate(Rutas.juego(codigo)) {
                        popUpTo(Rutas.MENU)
                    }
                }
            }

            ResultadoScreen(
                partida = juego.partida,
                miUid = auth.usuarioUid ?: "",
                mensaje = juego.mensaje,
                onJugarDeNuevo = juegoVm::reiniciar,
                onVolverAlMenu = {
                    juegoVm.detener()
                    navController.popBackStack(Rutas.MENU, false)
                }
            )
        }
    }
}