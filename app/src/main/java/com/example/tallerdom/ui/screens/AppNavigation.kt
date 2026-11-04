package com.example.tallerdom.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tallerdom.ui.screens.LoginScreen
import com.example.tallerdom.ui.screens.MenuScreen
import com.example.tallerdom.ui.screens.RegistroScreen
import com.example.tallerdom.viewmodel.AuthViewModel

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val authVm: AuthViewModel = viewModel()
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
            MenuScreen(
                nombre = auth.nombre ?: "jugador",
                onCerrarSesion = authVm::cerrarSesion
            )
        }
    }
}