package com.lamejorcaserita

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamejorcaserita.ui.nuevopedido.NuevoPedidoScreen
import com.lamejorcaserita.ui.pedidos.PedidosScreen
import com.lamejorcaserita.ui.productos.ProductosScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.lamejorcaserita.ui.detallepedido.DetallePedidoScreen

// Rutas de navegación
object Rutas {
    const val PEDIDOS = "pedidos"
    const val PRODUCTOS = "productos"
    const val NUEVO_PEDIDO = "nuevo_pedido"
    const val DETALLE_PEDIDO = "detalle_pedido"
    const val EDITAR_PRODUCTO = "editar_producto"
}

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rutas.PEDIDOS
    ) {
        // PANTALLA DE PEDIDOS
        composable(Rutas.PEDIDOS) {
            PedidosScreen(
                onNuevoPedido = {
                    navController.navigate("${Rutas.NUEVO_PEDIDO}?pedidoId=-1")
                },
                onVerDetalle = { pedidoId ->
                    navController.navigate("${Rutas.DETALLE_PEDIDO}/$pedidoId")
                },
                onIrAProductos = {
                    navController.navigate(Rutas.PRODUCTOS)
                }
            )
        }

        // PANTALLA DE PRODUCTOS
        composable(Rutas.PRODUCTOS) {
            ProductosScreen(
                onVolver = { navController.popBackStack() }
            )
        }

        // PANTALLA DE NUEVO PEDIDO / EDITAR PEDIDO
        composable(
            route = "${Rutas.NUEVO_PEDIDO}?pedidoId={pedidoId}",
            arguments = listOf(
                navArgument("pedidoId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val pedidoId = backStackEntry.arguments?.getInt("pedidoId") ?: -1
            NuevoPedidoScreen(
                pedidoId = if (pedidoId > 0) pedidoId else null,
                onVolver = { navController.popBackStack() },
                onPedidoGuardado = {
                    navController.popBackStack()
                }
            )
        }

        // PANTALLA DE DETALLE DEL PEDIDO
        composable(
            route = "${Rutas.DETALLE_PEDIDO}/{pedidoId}",
            arguments = listOf(
                navArgument("pedidoId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pedidoId = backStackEntry.arguments?.getInt("pedidoId") ?: 0
            DetallePedidoScreen(
                pedidoId = pedidoId,
                onVolver = { navController.popBackStack() },
                onEditar = { id ->
                    navController.navigate("${Rutas.NUEVO_PEDIDO}?pedidoId=$id")
                }
            )
        }
    }
}