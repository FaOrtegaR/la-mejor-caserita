package com.lamejorcaserita.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamejorcaserita.data.AppDatabase
import com.lamejorcaserita.data.entity.Pedido
import com.lamejorcaserita.data.entity.PedidoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PedidosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.obtenerBaseDeDatos(application)
    private val pedidoDao = db.pedidoDao()

    // FLUJOS DE PEDIDOS

    val pedidosPendientes: StateFlow<List<Pedido>> = pedidoDao.obtenerPendientes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pedidosEntregados: StateFlow<List<Pedido>> = pedidoDao.obtenerEntregados()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // CREAR PEDIDO

    fun crearPedido(nombreCliente: String, items: List<PedidoItem>, total: Int) {
        viewModelScope.launch {
            val pedido = Pedido(
                nombreCliente = nombreCliente,
                fecha = System.currentTimeMillis(),
                total = total,
                entregado = false
            )
            pedidoDao.crearPedidoConItems(pedido, items)
        }
    }

    /**
     * Agrega un producto a un pedido pendiente existente del cliente.
     * Si no existe pedido pendiente, crea uno nuevo.
     */
    fun agregarProductoAPedido(
        nombreCliente: String,
        productoId: Int,
        nombreProducto: String,
        precioUnitario: Int,
        cantidad: Int
    ) {
        viewModelScope.launch {
            val pedidoExistente = pedidoDao.buscarPedidoPendientePorCliente(nombreCliente)

            if (pedidoExistente != null) {
                pedidoDao.agregarItemAPedido(
                    pedidoId = pedidoExistente.id,
                    productoId = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cantidad = cantidad
                )
            } else {
                val nuevoPedido = Pedido(
                    nombreCliente = nombreCliente,
                    fecha = System.currentTimeMillis(),
                    total = precioUnitario * cantidad,
                    entregado = false
                )
                val pedidoId = pedidoDao.insertarPedido(nuevoPedido)
                val nuevoItem = PedidoItem(
                    pedidoId = pedidoId.toInt(),
                    productoId = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cantidad = cantidad,
                    subtotal = precioUnitario * cantidad
                )
                pedidoDao.insertarItem(nuevoItem)
            }
        }
    }

    // ACTUALIZAR PEDIDO

    fun marcarComoEntregado(pedidoId: Int, entregado: Boolean) {
        viewModelScope.launch {
            pedidoDao.actualizarEstado(pedidoId, entregado)
        }
    }

    fun eliminarPedido(pedidoId: Int) {
        viewModelScope.launch {
            pedidoDao.eliminarPedido(pedidoId)
        }
    }

    // OBTENER ITEMS

    suspend fun obtenerItemsDePedido(pedidoId: Int): List<PedidoItem> {
        return pedidoDao.obtenerItemsDePedido(pedidoId)
    }

    fun observarItemsDePedido(pedidoId: Int) = pedidoDao.observarItemsDePedido(pedidoId)

    // LIMPIEZA AUTOMÁTICA

    /**
     * Elimina los pedidos entregados que tengan más de 6 días de antigüedad.
     * Se debe llamar al abrir la pantalla de Pedidos.
     */
    fun limpiarPedidosAntiguos() {
        viewModelScope.launch {
            val seisDiasEnMillis = 6L * 24 * 60 * 60 * 1000
            val fechaLimite = System.currentTimeMillis() - seisDiasEnMillis
            pedidoDao.eliminarPedidosEntregadosAntiguos(fechaLimite)
        }
    }

    /**
     * Actualiza un pedido existente:
     * - Borra todos los items actuales
     * - Inserta los items nuevos según las cantidades
     * - Recalcula el total
     */
    fun actualizarPedido(
        pedidoId: Int,
        nombreCliente: String,
        cantidades: Map<Int, Int>,
        productos: List<com.lamejorcaserita.data.entity.Producto>
    ) {
        viewModelScope.launch {
            // 1. Eliminar items actuales
            pedidoDao.eliminarItemsDePedido(pedidoId)

            // 2. Insertar los items nuevos
            val nuevosItems = mutableListOf<PedidoItem>()
            var nuevoTotal = 0

            cantidades.forEach { (productoId, cantidad) ->
                if (cantidad > 0) {
                    val producto = productos.find { it.id == productoId }
                    if (producto != null) {
                        val subtotal = producto.precio * cantidad
                        nuevosItems.add(
                            PedidoItem(
                                pedidoId = pedidoId,
                                productoId = producto.id,
                                nombreProducto = producto.nombre,
                                precioUnitario = producto.precio,
                                cantidad = cantidad,
                                subtotal = subtotal
                            )
                        )
                        nuevoTotal += subtotal
                    }
                }
            }

            pedidoDao.insertarItems(nuevosItems)

            // 3. Actualizar nombre y total del pedido
            pedidoDao.actualizarNombre(pedidoId, nombreCliente)
            pedidoDao.actualizarTotal(pedidoId, nuevoTotal)
        }
    }
    suspend fun obtenerPedidoPorId(pedidoId: Int): Pedido? {
        return pedidoDao.obtenerPorId(pedidoId)
    }

    fun marcarComoPagado(pedidoId: Int, pagado: Boolean) {
        viewModelScope.launch {
            pedidoDao.actualizarPagado(pedidoId, pagado)
        }
    }

    /**
     * Crea un pedido nuevo con múltiples items de una sola vez.
     * Si ya existe un pedido pendiente con el mismo nombre, se suman los items.
     */
    fun crearOActualizarPedidoConItems(
        nombreCliente: String,
        items: List<PedidoItem>
    ) {
        viewModelScope.launch {
            val pedidoExistente = pedidoDao.buscarPedidoPendientePorCliente(nombreCliente)

            if (pedidoExistente != null) {
                // Sumar cada item al pedido existente
                items.forEach { item ->
                    pedidoDao.agregarItemAPedido(
                        pedidoId = pedidoExistente.id,
                        productoId = item.productoId,
                        nombreProducto = item.nombreProducto,
                        precioUnitario = item.precioUnitario,
                        cantidad = item.cantidad
                    )
                }
            } else {
                // Crear un pedido nuevo con todos los items
                val total = items.sumOf { it.subtotal }
                val nuevoPedido = Pedido(
                    nombreCliente = nombreCliente,
                    fecha = System.currentTimeMillis(),
                    total = total,
                    entregado = false,
                    pagado = false
                )
                val pedidoId = pedidoDao.insertarPedido(nuevoPedido)
                val itemsConPedido = items.map { it.copy(pedidoId = pedidoId.toInt()) }
                pedidoDao.insertarItems(itemsConPedido)
            }
        }
    }
}